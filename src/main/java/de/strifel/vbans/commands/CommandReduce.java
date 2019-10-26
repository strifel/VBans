package de.strifel.vbans.commands;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.Ban;
import de.strifel.vbans.database.DatabaseConnection;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static de.strifel.vbans.commands.CommandTempBan.getBanDuration;

public class CommandReduce implements Command {
    private final DatabaseConnection database;


    public CommandReduce(VBans vBans) {
        this.database = vBans.getDatabaseConnection();
    }

    @Override
    public void execute(CommandSource commandSource, @NonNull String[] strings) {
        if (strings.length >= 1 && strings.length <= 2) {
            try {
                String uuid = database.getUUID(strings[0]);
                Ban ban = database.getBan(uuid);
                if (uuid != null && ban != null) {
                    long duration = strings.length == 2 ? getBanDuration(strings[1]) : -1;
                    if (duration == 0) {
                        commandSource.sendMessage(TextComponent.of("Invalid duration! Us d, m, h or s as suffix for time!").color(TextColor.RED));
                        return;
                    }
                    database.reduceBanTo(uuid, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), duration == -1 ? (System.currentTimeMillis() / 1000) : ban.getBannedAt() + duration);
                    commandSource.sendMessage(TextComponent.of("All active bans for " + strings[0] + " are reduced!").color(TextColor.YELLOW));
                } else {
                    commandSource.sendMessage(TextComponent.of("This Player is not banned!").color(TextColor.RED));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                commandSource.sendMessage(TextComponent.of("An database error occurred!").color(TextColor.RED));
            }
        } else {
            commandSource.sendMessage(TextComponent.of("Usage: /reduce <username> [duration]").color(TextColor.RED));
        }
    }

    @Override
    public List<String> suggest(CommandSource source, @NonNull String[] currentArgs) {
        try {
            return database.getUsernamesByQuery(DatabaseConnection.BANED_CRITERIA);
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasPermission(CommandSource source, @NonNull String[] args) {
        return source.hasPermission("VBans.reduce");
    }
}
