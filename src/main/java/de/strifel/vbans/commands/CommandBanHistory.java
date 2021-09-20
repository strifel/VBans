package de.strifel.vbans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.Util;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.DatabaseConnection;
import de.strifel.vbans.database.HistoryBan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

import static de.strifel.vbans.Util.*;

public class CommandBanHistory implements SimpleCommand {
    private final DatabaseConnection database;
    private final ProxyServer server;
    private final VBans vbans;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    public CommandBanHistory(VBans vbans) {
        database = vbans.getDatabaseConnection();
        this.server = vbans.getServer();
        this.vbans = vbans;
    }


    @Override
    public void execute(Invocation commandInvocation) {
        String[] strings = commandInvocation.arguments();
        CommandSource commandSource = commandInvocation.source();

        if (strings.length == 1) {
            try {
                List<HistoryBan> bans = database.getBanHistory(server.getPlayer(strings[0]).isPresent() ? server.getPlayer(strings[0]).get().getUniqueId().toString() : database.getUUID(strings[0]), commandSource.hasPermission("VBans.history.seeDeleted"));
                commandSource.sendMessage(Component.text("Ban history of " + strings[0]).color(COLOR_YELLOW));
                commandSource.sendMessage(Component.text("----------------------------------------").color(COLOR_YELLOW));
                for (HistoryBan ban : bans) {
                    commandSource.sendMessage(generateBanText(ban));
                }
                commandSource.sendMessage(Component.text("----------------------------------------").color(COLOR_YELLOW));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            commandSource.sendMessage(Component.text("Usage: /banhistory <username>").color(COLOR_RED));
        }
    }

    @Override
    public List<String> suggest(Invocation commandInvocation) {
        try {
            List<String> users = database.getUsernamesByQuery("");
            users.addAll(Util.getAllPlayernames(server));
            return users;
        } catch (SQLException e) {
            return Util.getAllPlayernames(server);
        }
    }

    @Override
    public boolean hasPermission(Invocation commandInvocation) {
        return commandInvocation.source().hasPermission("VBans.history");
    }

    private Component generateBanText(HistoryBan ban) {
        Component banText =
                Component.text("#" + ban.getId() + " " + DATE_FORMAT.format(ban.getBannedAt() * 1000) + ": ")
                        .append(Component.text("\"" + ban.getReason() + "\"").decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                        .append(Component.text(" by "))
                        .append(Component.text(ban.getBannedByUsername(vbans)))
                        .append(Component.text(" "));
        banText = banText.color(COLOR_YELLOW);
        Component length = getBanLength(ban.getOriginalBanEnd(), ban.getBannedAt());
        if (ban.isReduced()) {
            length = length.decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.TRUE);
            banText = banText.append(length).append(Component.text(" "));
            banText = banText.append(getBanLength(ban.getUntil(), ban.getBannedAt()));
            banText = banText.append(Component.text("(Reduced by " + ban.getReducedByUsername(vbans) + ")").color(COLOR_DARK_GREEN));
        } else {
            banText = banText.append(length);
        }
        if (ban.isPurged()) {
            banText = banText.decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.TRUE);
            banText = Component.text("").append(banText).append(Component.text("(Deleted by " + ban.getPurgedByUsername(vbans) + ")"));
        }

        return banText;
    }


    private Component getBanLength(long end, long start) {
        if (end == -1) {
            return Component.text("(permanent)").color(COLOR_RED);
        } else if (start - end >= 0) {
            return Component.text("(kick)").color(COLOR_DARK_GREEN);
        } else {
            long duration = end - start;
            if (duration / (60 * 60 * 24) > 0) {
                return Component.text("§c(" + ((int) duration / (60 * 60 * 24)) + "d)").color(COLOR_RED);
            } else if (duration / (60 * 60) > 0) {
                return Component.text("§c(" + ((int) duration / (60 * 60)) + "h)").color(COLOR_RED);
            } else if (duration / 60 > 0) {
                return Component.text("§c(" + ((int) duration / 60) + "m)").color(COLOR_RED);
            } else {
                return Component.text("§c(" + duration + "s)");
            }
        }

    }
}
