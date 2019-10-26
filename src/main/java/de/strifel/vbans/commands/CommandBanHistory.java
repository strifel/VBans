package de.strifel.vbans.commands;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.Util;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.DatabaseConnection;
import de.strifel.vbans.database.HistoryBan;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class CommandBanHistory implements Command {
    private final DatabaseConnection database;
    private final ProxyServer server;
    private final VBans vbans;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    public CommandBanHistory(ProxyServer server, VBans vbans) {
        database = vbans.getDatabaseConnection();
        this.server = server;
        this.vbans = vbans;
    }


    @Override
    public void execute(CommandSource commandSource, @NonNull String[] strings) {
        if (strings.length == 1) {
            try {
                List<HistoryBan> bans = database.getBanHistory(server.getPlayer(strings[0]).isPresent() ? server.getPlayer(strings[0]).get().getUniqueId().toString() : database.getUUID(strings[0]), commandSource.hasPermission("VBans.history.seeDeleted"));
                commandSource.sendMessage(TextComponent.of("Ban history of " + strings[0]).color(TextColor.YELLOW));
                commandSource.sendMessage(TextComponent.of("----------------------------------------").color(TextColor.YELLOW));
                for (HistoryBan ban : bans) {
                    commandSource.sendMessage(generateBanText(ban));
                }
                commandSource.sendMessage(TextComponent.of("----------------------------------------").color(TextColor.YELLOW));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            commandSource.sendMessage(TextComponent.of("Usage: /banhistory <username>").color(TextColor.RED));
        }
    }

    @Override
    public List<String> suggest(CommandSource source, @NonNull String[] currentArgs) {
        try {
            List<String> users =  database.getUsernamesByQuery("");
            users.addAll(Util.getAllPlayernames(server));
            return users;
        } catch (SQLException e) {
            return Util.getAllPlayernames(server);
        }
    }

    @Override
    public boolean hasPermission(CommandSource source, @NonNull String[] args) {
        return source.hasPermission("VBans.history");
    }

    private TextComponent generateBanText(HistoryBan ban) {
        TextComponent banText =
                TextComponent.of("#" + ban.getId() + " " + DATE_FORMAT.format(ban.getBannedAt() * 1000) + ": ")
                .append(TextComponent.of("\"" + ban.getReason() + "\"").decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                .append(TextComponent.of(" by "))
                .append(TextComponent.of(ban.getBannedByUsername(vbans)))
                .append(TextComponent.of(" "));
        banText = banText.color(TextColor.YELLOW);
        TextComponent length = getBanLength(ban.getOriginalBanEnd(), ban.getBannedAt());
        if (ban.isReduced()) {
            length = length.decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.TRUE);
            banText = banText.append(length).append(TextComponent.of(" "));
            banText = banText.append(getBanLength(ban.getUntil(), ban.getBannedAt()));
            banText = banText.append(TextComponent.of("(Reduced by " + ban.getReducedByUsername(vbans) + ")").color(TextColor.DARK_GREEN));
        } else {
            banText = banText.append(length);
        }
        if (ban.isPurged()) {
            banText = banText.decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.TRUE);
            banText = TextComponent.of("").append(banText).append(TextComponent.of("(Deleted by " + ban.getPurgedByUsername(vbans) + ")").color(TextColor.LIGHT_PURPLE));
        }

        return banText;
    }


    private TextComponent getBanLength(long end, long start) {
        if (end == -1) {
           return TextComponent.of("(permanent)").color(TextColor.DARK_RED);
        } else if (start - end >= 0){
            return TextComponent.of("(kick)").color(TextColor.AQUA);
        } else {
            long duration = end - start;
            if (duration / (60 * 60 * 24) > 0) {
                return TextComponent.of("§c(" + ((int) duration / (60 * 60 * 24)) + "d)").color(TextColor.RED);
            } else if (duration / (60 * 60) > 0) {
                return TextComponent.of("§c(" + ((int) duration / (60 * 60)) + "h)").color(TextColor.RED);
            } else if (duration / 60 > 0) {
                return TextComponent.of("§c(" + ((int) duration / 60) + "m)").color(TextColor.RED);
            } else {
                return TextComponent.of("§c(" + duration + "s)");
            }
        }

    }
}
