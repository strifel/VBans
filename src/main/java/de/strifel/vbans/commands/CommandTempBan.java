package de.strifel.vbans.commands;

import com.velocitypowered.api.command.*;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.Util;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.Ban;
import de.strifel.vbans.database.DatabaseConnection;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static de.strifel.vbans.Util.COLOR_RED;
import static de.strifel.vbans.Util.COLOR_YELLOW;

public class CommandTempBan implements SimpleCommand {
    private final ProxyServer server;
    private final DatabaseConnection database;
    private final VBans vBans;
    private final String DEFAULT_REASON;
    private final String END_OF_HIS_LIFE;
    private final String BANNED_BROADCAST;

    public CommandTempBan(VBans vBans) {
        this.server = vBans.getServer();
        database = vBans.getDatabaseConnection();
        this.vBans = vBans;
        this.DEFAULT_REASON = vBans.getMessages().getString("StandardBanMessage");
        this.BANNED_BROADCAST = vBans.getMessages().getString("BannedBroadcast");
        this.END_OF_HIS_LIFE = vBans.getMessages().getString("EndOfHisLife", "the end of their life.");
    }

    public void execute(Invocation commandInvocation) {
        CommandSource commandSource = commandInvocation.source();
        String[] strings = commandInvocation.arguments();

        if (strings.length > 1) {
            long duration = getBanDuration(strings[1]);
            if (duration == 0) {
                commandSource.sendMessage(Component.text("Invalid duration! Us d, m, h or s as suffix for time!").color(COLOR_RED));
                return;
            }
            long end = (System.currentTimeMillis() / 1000) + duration;
            String reason = DEFAULT_REASON;
            if (strings.length > 2 && commandSource.hasPermission("VBans.temp.reason")) {
                reason = String.join(" ", Arrays.copyOfRange(strings, 2, strings.length));
            }
            Optional<Player> oPlayer = server.getPlayer(strings[0]);
            if (oPlayer.isPresent()) {
                Player player = oPlayer.get();
                if (!player.hasPermission("VBans.prevent") || commandSource instanceof ConsoleCommandSource) {

                    player.disconnect(Util.formatBannedMessage(commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUsername(), reason, end));
                    try {
                        database.addBan(player.getUniqueId().toString(), end, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), reason);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        commandSource.sendMessage(Component.text("Your ban can not be registered.").color(COLOR_RED));
                        return;
                    }
                    commandSource.sendMessage(Component.text("You banned " + strings[0] + " for " + duration + " seconds!").color(COLOR_YELLOW));
                    Util.broadcastMessage(BANNED_BROADCAST
                                    .replace("$bannedBy", commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUsername())
                                    .replace("$player", strings[0])
                                    .replace("$bannedUntil", Util.UNBAN_DATE_FORMAT.format(end * 1000))
                                    .replace("$reason", reason)
                            , "VBans.bannedBroadcast", server);
                } else {
                    commandSource.sendMessage(Component.text("You are not allowed to Ban this player!").color(COLOR_RED));
                }
            } else {
                try {
                    String uuid = database.getUUID(strings[0]);
                    if (uuid != null) {
                        if (!Util.hasOfflineProtectBanPermission(uuid, vBans) || commandSource instanceof ConsoleCommandSource) {
                            Ban currentBan = database.getBan(uuid);
                            if (currentBan == null) {
                                database.addBan(uuid, end, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), reason);
                                commandSource.sendMessage(Component.text("You banned " + strings[0] + " for " + duration + " seconds!").color(COLOR_YELLOW));
                                Util.broadcastMessage(BANNED_BROADCAST
                                                .replace("$bannedBy", commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUsername())
                                                .replace("$player", strings[0])
                                                .replace("$bannedUntil", Util.UNBAN_DATE_FORMAT.format(end * 1000))
                                                .replace("$reason", reason)
                                        , "VBans.bannedBroadcast", server);
                            } else {
                                commandSource.sendMessage(Component.text(strings[0] + " is already banned until " + (currentBan.getUntil() == -1 ? END_OF_HIS_LIFE+"." : Util.UNBAN_DATE_FORMAT.format(currentBan.getUntil() * 1000))).color(COLOR_RED));
                            }
                        } else {
                            commandSource.sendMessage(Component.text("You are not allowed to ban this player!").color(COLOR_RED));
                        }
                    } else {
                        commandSource.sendMessage(Component.text("Player not found!").color(COLOR_RED));
                    }
                } catch (SQLException e) {
                    commandSource.sendMessage(Component.text("An database issue occurred!").color(COLOR_RED));
                }
            }
        } else {
            commandSource.sendMessage(Component.text("Usage: /tempban <player> <time> [reason]").color(COLOR_RED));
        }
    }

    public List<String> suggest(Invocation commandInvocation) {
        String[] strings = commandInvocation.arguments();

        if (strings.length == 1) {
            return Util.getAllPlayernames(server);
        }
        if (strings.length == 2) {
            return Arrays.asList("30d", "12h", "30m", "5s");
        }
        return new ArrayList<String>();
    }

    public boolean hasPermission(Invocation commandInvocation) {
        return commandInvocation.source().hasPermission("VBans.temp");
    }

    public static long getBanDuration(String durationString) {
        if (Util.isInt(durationString)) {
            return 60 * 60 * 24 * Integer.parseInt(durationString);
        } else if (durationString.endsWith("d")) {
            durationString = durationString.replace("d", "");
            if (!Util.isInt(durationString)) return 0;
            return 60 * 60 * 24 * Integer.parseInt(durationString);
        } else if (durationString.endsWith("h")) {
            durationString = durationString.replace("h", "");
            if (!Util.isInt(durationString)) return 0;
            return 60 * 60 * Integer.parseInt(durationString);
        } else if (durationString.endsWith("m")) {
            durationString = durationString.replace("m", "");
            if (!Util.isInt(durationString)) return 0;
            return 60 * Integer.parseInt(durationString);
        } else if (durationString.endsWith("s")) {
            durationString = durationString.replace("s", "");
            if (!Util.isInt(durationString)) return 0;
            return Integer.parseInt(durationString);
        } else {
            return 0;
        }
    }

}
