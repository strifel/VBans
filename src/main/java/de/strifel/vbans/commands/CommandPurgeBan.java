package de.strifel.vbans.commands;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.database.DatabaseConnection;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommandPurgeBan implements Command {
    private final ProxyServer server;
    private final DatabaseConnection database;

    public CommandPurgeBan(ProxyServer server, DatabaseConnection databaseConnection) {
        this.server = server;
        database = databaseConnection;
    }

    @Override
    public void execute(CommandSource commandSource, @NonNull String[] strings) {
        if (strings.length == 1) {
            try {
                String uuid = database.getUUID(strings[0]);
                if (uuid != null && database.getBan(uuid) != null) {
                    database.purgeActiveBans(uuid, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString());
                    commandSource.sendMessage(TextComponent.of("All active bans for " + strings[0] + " are deleted and will not affect his history anymore!").color(TextColor.YELLOW));
                } else {
                    commandSource.sendMessage(TextComponent.of("This Player is not banned!").color(TextColor.RED));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                commandSource.sendMessage(TextComponent.of("An database error occurred!").color(TextColor.RED));
            }
        } else {
            commandSource.sendMessage(TextComponent.of("Usage: /delban <username>").color(TextColor.RED));
        }
    }

    @Override
    public List<String> suggest(CommandSource source, @NonNull String[] currentArgs) {
        try {
            return database.getUsernamesByQuery(DatabaseConnection.BANED_CRITERIA.replace("?", (System.currentTimeMillis() / 1000) + ""));
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasPermission(CommandSource source, @NonNull String[] args) {
        return source.hasPermission("VBans.delete");
    }
}
