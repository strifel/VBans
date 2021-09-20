package de.strifel.vbans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.Ban;
import de.strifel.vbans.database.DatabaseConnection;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static de.strifel.vbans.Util.COLOR_RED;
import static de.strifel.vbans.Util.COLOR_YELLOW;
import static de.strifel.vbans.commands.CommandTempBan.getBanDuration;

public class CommandReduce implements SimpleCommand {
    private final DatabaseConnection database;


    public CommandReduce(VBans vBans) {
        this.database = vBans.getDatabaseConnection();
    }

    @Override
    public void execute(Invocation commandInvocation) {
        String[] strings = commandInvocation.arguments();
        CommandSource commandSource = commandInvocation.source();
        
        if (strings.length >= 1 && strings.length <= 2) {
            try {
                String uuid = database.getUUID(strings[0]);
                Ban ban = database.getBan(uuid);
                if (uuid != null && ban != null) {
                    long duration = strings.length == 2 ? getBanDuration(strings[1]) : -1;
                    if (duration == 0) {
                        commandSource.sendMessage(Component.text("Invalid duration! Us d, m, h or s as suffix for time!").color(COLOR_RED));
                        return;
                    }
                    database.reduceBanTo(uuid, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), duration == -1 ? (System.currentTimeMillis() / 1000) : ban.getBannedAt() + duration);
                    commandSource.sendMessage(Component.text("All active bans for " + strings[0] + " are reduced!").color(COLOR_YELLOW));
                } else {
                    commandSource.sendMessage(Component.text("This Player is not banned!").color(COLOR_RED));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                commandSource.sendMessage(Component.text("An database error occurred!").color(COLOR_RED));
            }
        } else {
            commandSource.sendMessage(Component.text("Usage: /reduce <username> [duration]").color(COLOR_RED));
        }
    }

    @Override
    public List<String> suggest(Invocation commandInvocation) {
        try {
            return database.getUsernamesByQuery(DatabaseConnection.BANED_CRITERIA);
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasPermission(Invocation commandInvocation) {
        return commandInvocation.source().hasPermission("VBans.reduce");
    }
}
