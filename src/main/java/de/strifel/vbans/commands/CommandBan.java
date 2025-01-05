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
import net.kyori.adventure.text.format.TextColor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static de.strifel.vbans.Util.COLOR_RED;
import static de.strifel.vbans.Util.COLOR_YELLOW;

public class CommandBan implements SimpleCommand {
    private final ProxyServer server;
    private final DatabaseConnection database;
    private final VBans vBans;
    private final String DEFAULT_REASON;
    private final String BANNED_BROADCAST;
    private final String END_OF_HIS_LIFE;

    public CommandBan(VBans vBans) {
        this.server = vBans.getServer();
        this.database = vBans.getDatabaseConnection();
        this.vBans = vBans;
        DEFAULT_REASON = vBans.getMessages().getString("StandardBanMessage");
        BANNED_BROADCAST = vBans.getMessages().getString("BannedBroadcast");
        if (vBans.getMessages().getString("EndOfHisLife")==null) {
            END_OF_HIS_LIFE="";
        } else {
            END_OF_HIS_LIFE=vBans.getMessages().getString("EndOfHisLife");
        }
    }

    @Override
    public void execute(Invocation commandInvocation) {
        CommandSource commandSource = commandInvocation.source();
        String[] strings = commandInvocation.arguments();

        if (strings.length > 0) {
            String reason = DEFAULT_REASON;
            if (strings.length > 1 && commandSource.hasPermission("VBans.ban.reason")) {
                reason = String.join(" ", Arrays.copyOfRange(strings, 1, strings.length));
            }
            Optional<Player> oPlayer = server.getPlayer(strings[0]);
            if (oPlayer.isPresent()) {
                Player player = oPlayer.get();
                if (!player.hasPermission("VBans.prevent") || commandSource instanceof ConsoleCommandSource) {
                    player.disconnect(Util.formatBannedMessage(commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUsername(), reason, -1));
                    try {
                        database.addBan(player.getUniqueId().toString(), -1, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), reason);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        commandSource.sendMessage(Component.text("Your ban can not be registered.").color(COLOR_RED));
                        return;
                    }
                    commandSource.sendMessage(Component.text("You banned " + strings[0]).color(COLOR_RED));
                    Util.broadcastMessage(BANNED_BROADCAST
                                    .replace("$bannedBy", commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUsername())
                                    .replace("$player", strings[0])
                                    .replace("$bannedUntil", END_OF_HIS_LIFE)
                                    .replace("$reason", reason)
                            , "VBans.bannedBroadcast", server);
                } else {
                    commandSource.sendMessage(Component.text("You are not allowed to ban this player!").color(COLOR_RED));
                }
            } else {
                try {
                    String uuid = database.getUUID(strings[0]);
                    if (uuid != null) {
                        if (!Util.hasOfflineProtectBanPermission(uuid, vBans) || commandSource instanceof ConsoleCommandSource) {
                            Ban currentBan = database.getBan(uuid);
                            if (currentBan != null && commandSource.hasPermission("VBans.ban.topPerm") && currentBan.getUntil() != -1) {
                                database.purgeActiveBans(uuid, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString());
                                commandSource.sendMessage(Component.text(strings[0] + " was already banned until " + Util.UNBAN_DATE_FORMAT.format(currentBan.getUntil() * 1000) + ". I removed that and created a perm ban.").color(COLOR_RED));
                                currentBan = null;
                            }
                            if (currentBan == null) {
                                database.addBan(uuid, -1, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), reason);
                                commandSource.sendMessage(Component.text("You banned " + strings[0]).color(COLOR_YELLOW));
                                Util.broadcastMessage(BANNED_BROADCAST
                                        .replace("$bannedBy", commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUsername())
                                        .replace("$player", strings[0])
                                        .replace("$bannedUntil", END_OF_HIS_LIFE)
                                        .replace("$reason", reason)
                                , "VBans.bannedBroadcast", server);
                            } else {
                                commandSource.sendMessage(Component.text(strings[0] + " is already banned until " + (currentBan.getUntil() == -1 ? END_OF_HIS_LIFE : Util.UNBAN_DATE_FORMAT.format(currentBan.getUntil() * 1000))).color(COLOR_RED));
                            }
                        } else {
                            commandSource.sendMessage(Component.text("You are not allowed to ban this player!").color(COLOR_RED));
                        }
                    } else {
                        commandSource.sendMessage(Component.text("Player not found!").color(TextColor.fromHexString("")));
                    }
                } catch (SQLException e) {
                    commandSource.sendMessage(Component.text("An database issue occurred!").color(COLOR_RED));
                }
            }
        } else {
            commandSource.sendMessage(Component.text("Usage: /ban <player> [reason]").color(COLOR_RED));
        }
    }

    @Override
    public List<String> suggest(Invocation commandInvocation) {
        if (commandInvocation.arguments().length == 1) {
            return Util.getAllPlayernames(server);
        }
        return new ArrayList<String>();
    }

    @Override
    public boolean hasPermission(Invocation commandInvocation) {
        return commandInvocation.source().hasPermission("VBans.ban");
    }
}
