package de.strifel.vbans;

import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.commands.*;
import de.strifel.vbans.database.Ban;
import de.strifel.vbans.database.DatabaseConnection;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;


@Plugin(id = "vbans", name = "VBans", version = "1.1-SNAPSHOT", description = "Ban players! Its fun!")
public class VBans {

    private final ProxyServer server;
    private DatabaseConnection databaseConnection;
    private Toml messages;
    private Toml config;
    private final Metrics.Factory metricsFactory;
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
    public VBans(ProxyServer server, Logger logger, @DataDirectory final Path folder, Metrics.Factory metricsFactory) {
        this.server = server;
        config = loadConfig(folder);
        messages = config.getTable("Messages");
        Util.BAN_TEMPLATE = messages.getString("BanLayout");

        this.metricsFactory = metricsFactory;
        logger.info("VBans uses bStats to anonymously collect stats like the count of banned players. " +
                "Make sure to deactivate it if you dont want it to.");
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
            return;
        } catch (SQLException e) {
            System.err.println("An error occoured while connecting to MySQL: " + e.getMessage());
            return;
        }
        boolean isGCommand = config.getTable("Options").getBoolean("changeToGCommands");
        // Register commands
        server.getCommandManager().register(isGCommand ? "gkick" : "kick", new CommandKick(this), "vkick");
        server.getCommandManager().register(isGCommand ? "gban" : "ban", new CommandBan(this), "vban");
        server.getCommandManager().register("tban", new CommandTempBan(this),isGCommand ? "gtempban" : "tempban", "vtempban", "vtban");
        server.getCommandManager().register("pban", new CommandPurgeBan(this), "vpurgeban", "purgeban", "delban");
        server.getCommandManager().register("reduceBan", new CommandReduce(this), "rban", isGCommand ? "gunban" : "unban", isGCommand ? "gpardon" : "pardon");
        server.getCommandManager().register(isGCommand ? "gbanhistory" : "banhistory", new CommandBanHistory(this), "bhistory", "bhist", "banh");
        // Luck Perms support
        if (server.getPluginManager().isLoaded("luckperms"))
            try {
                luckPermsApi = net.luckperms.api.LuckPermsProvider.get();
            } catch (NoClassDefFoundError e) {
                System.out.println("Luck perms is not installed. VBans will not use it to determine offline permissions.");
            }
        // bStats
        Metrics metrics = metricsFactory.make(this, 11543);
        metrics.addCustomChart(new SingleLineChart("banned_users", () -> databaseConnection.getBannedCount()));
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
