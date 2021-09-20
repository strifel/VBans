package de.strifel.vbans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.DatabaseConnection;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static de.strifel.vbans.Util.COLOR_RED;
import static de.strifel.vbans.Util.COLOR_YELLOW;

public class CommandPurgeBan implements SimpleCommand {
    private final ProxyServer server;
    private final DatabaseConnection database;

    public CommandPurgeBan(VBans vbans) {
        this.server = vbans.getServer();
        database = vbans.getDatabaseConnection();
    }

    @Override
    public void execute(Invocation commandInvocation) {
        CommandSource commandSource = commandInvocation.source();
        String[] strings = commandInvocation.arguments();
        
        if (strings.length >= 1 && strings.length <= 2) {
            try {
                String uuid = database.getUUID(strings[0]);
                if (uuid != null && (database.getBan(uuid) != null || strings.length == 2)) {
                    if (strings.length == 1) {
                        database.purgeActiveBans(uuid, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString());
                        commandSource.sendMessage(Component.text("All active bans for " + strings[0] + " are deleted and will not affect his history anymore!").color(COLOR_YELLOW));
                    } else {
                        try {
                            int id = Integer.parseInt(strings[1]);
                            database.purgeBanById(uuid, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), id);
                            commandSource.sendMessage(Component.text("The ban for " + strings[0] + " with the id " + id + " is deleted and will not affect the players history anymore!").color(COLOR_YELLOW));
                        } catch (NumberFormatException e) {
                            commandSource.sendMessage(Component.text("Please enter a valid id!").color(COLOR_RED));
                        }
                    }
                } else {
                    commandSource.sendMessage(Component.text("This Player is not banned or does not exists!").color(COLOR_RED));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                commandSource.sendMessage(Component.text("An database error occurred!").color(COLOR_RED));
            }
        } else {
            commandSource.sendMessage(Component.text("Usage: /delban <username> [id]").color(COLOR_RED));
        }
    }

    @Override
    public List<String> suggest(Invocation commandInvocation) {
        try {
            return database.getUsernamesByQuery(DatabaseConnection.BANED_CRITERIA.replace("?", (System.currentTimeMillis() / 1000) + ""));
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasPermission(Invocation commandInvocation) {
        return commandInvocation.source().hasPermission("VBans.delete");
    }
}
