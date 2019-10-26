package de.strifel.vbans.commands;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.Util;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.Ban;
import de.strifel.vbans.database.DatabaseConnection;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CommandBan implements Command {
    private final ProxyServer server;
    private final DatabaseConnection database;
    private final VBans vBans;

    public CommandBan(ProxyServer server, VBans vBans) {
        this.server = server;
        this.database = vBans.getDatabaseConnection();
        this.vBans = vBans;
    }


    public void execute(CommandSource commandSource, @NonNull String[] strings) {
        if (strings.length > 0) {
            String reason = "The Ban Hammer has spoken!";
            if  (strings.length > 1 && commandSource.hasPermission("VBans.ban.reason")) {
                reason = String.join(" ", Arrays.copyOfRange(strings, 1, strings.length));
            }
            Optional<Player> oPlayer = server.getPlayer(strings[0]);
            if (oPlayer.isPresent()) {
                Player player = oPlayer.get();
                if (!player.hasPermission("VBans.prevent") || commandSource instanceof ConsoleCommandSource) {
                    player.disconnect(Util.formatBannedMessage(commandSource instanceof ConsoleCommandSource ? "Console" : ((Player)commandSource).getUsername(), reason, -1));
                    try {
                        database.addBan(player.getUniqueId().toString(), -1, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), reason);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        commandSource.sendMessage(TextComponent.of("Your ban can not be registered.").color(TextColor.RED));
                        return;
                    }
                    commandSource.sendMessage(TextComponent.of("You banned " + strings[0]).color(TextColor.YELLOW));
                } else {
                    commandSource.sendMessage(TextComponent.of("You are not allowed to ban this player!").color(TextColor.RED));
                }
            } else {
                try {
                    String uuid = database.getUUID(strings[0]);
                    if (uuid != null) {
                        if (!Util.hasOfflineProtectBanPermission(uuid, vBans) || commandSource instanceof ConsoleCommandSource) {
                            Ban currentBan = database.getBan(uuid);
                            if (currentBan != null && commandSource.hasPermission("VBans.ban.topPerm")) {
                                database.purgeActiveBans(uuid, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString());
                                commandSource.sendMessage(TextComponent.of(strings[0] + " was already banned until " + Util.UNBAN_DATE_FORMAT.format(currentBan.getUntil() * 1000) + ". I removed that and created a perm ban.").color(TextColor.YELLOW));
                                currentBan = null;
                            }
                            if (currentBan == null) {
                                database.addBan(uuid, -1, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), reason);
                                commandSource.sendMessage(TextComponent.of("You banned " + strings[0]).color(TextColor.YELLOW));
                            } else {
                                commandSource.sendMessage(TextComponent.of(strings[0] + " is already banned until " + Util.UNBAN_DATE_FORMAT.format(currentBan.getUntil() * 1000)).color(TextColor.RED));
                            }
                        } else {
                            commandSource.sendMessage(TextComponent.of("You are not allowed to ban this player!").color(TextColor.RED));
                        }
                    } else {
                        commandSource.sendMessage(TextComponent.of("Player not found!").color(TextColor.RED));
                    }
                } catch (SQLException e) {
                    commandSource.sendMessage(TextComponent.of("An database issue occurred!").color(TextColor.RED));
                }
            }
        } else {
            commandSource.sendMessage(TextComponent.of("Usage: /ban <player> [reason]").color(TextColor.RED));
        }
    }

    public List<String> suggest(CommandSource source, @NonNull String[] currentArgs) {
        if (currentArgs.length == 1) {
            return Util.getAllPlayernames(server);
        }
        return new ArrayList<String>();
    }

    public boolean hasPermission(CommandSource source, @NonNull String[] args) {
        return source.hasPermission("VBans.ban");
    }
}
