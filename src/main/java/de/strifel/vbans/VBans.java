package de.strifel.vbans;

import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.commands.*;
import de.strifel.vbans.database.Ban;
import de.strifel.vbans.database.DatabaseConnection;
import me.lucko.luckperms.LuckPerms;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;


@Plugin(id = "vbans", name = "VBans", version = "1.0-SNAPSHOT", description = "Ban players! Its fun!")
public class VBans {

    private final ProxyServer server;
    private DatabaseConnection databaseConnection;
    private Toml messages;
    private Toml config;
    Object luckPermsApi;


    private Toml loadConfig(Path path) {
        File folder = path.toFile();
        File file = new File(folder, "config.toml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            try (InputStream input = getClass().getResourceAsStream("/" + file.getName())) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                return null;
            }
        }

        return new Toml().read(file);
    }


    @Inject
    public VBans(ProxyServer server, Logger logger, @DataDirectory final Path folder) {
        this.server = server;
        config = loadConfig(folder);
        messages = config.getTable("Messages");
        Util.BAN_TEMPLATE = messages.getString("BanLayout");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Connect to database
        JDBC.inject(this);
        Toml database = config.getTable("Database");
        try {
            databaseConnection = new DatabaseConnection(database.getString("host"), Integer.parseInt(database.getLong("port").toString()), database.getString("username"), database.getString("password"), database.getString("database"));
        } catch (ClassNotFoundException e) {
            System.err.println("It seems like you do not have JDBC installed. Can not communicate with database");
        } catch (SQLException e) {
            System.err.println("An error occoured while connecting to MySQL: " + e.getMessage());
        }
        // Register commands
        server.getCommandManager().register(new CommandKick(this), "kick", "vkick");
        server.getCommandManager().register(new CommandBan(this), "ban", "vban");
        server.getCommandManager().register(new CommandTempBan(this), "tban", "tempban", "vtempban", "vtban");
        server.getCommandManager().register(new CommandPurgeBan(this), "pban", "vpurgeban", "purgeban", "delban");
        server.getCommandManager().register(new CommandReduce(this), "reduceBan", "rban", "unban", "pardon");
        server.getCommandManager().register(new CommandBanHistory(this), "banhistory", "bhistory", "bhist", "banh");
        // Luck Perms support
        if (server.getPluginManager().isLoaded("luckperms"))
            luckPermsApi = LuckPerms.getApi();
    }

    @Subscribe
    public void onUserLoginEvent(LoginEvent event) {
        try {
            databaseConnection.setUsername(event.getPlayer().getUniqueId().toString(), event.getPlayer().getUsername());
            Ban ban = databaseConnection.getBan(event.getPlayer().getUniqueId().toString());
            if (ban != null) {
                event.setResult(ResultedEvent.ComponentResult.denied(Util.formatBannedMessage(ban, this)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Toml getMessages() {
        return messages;
    }
}
