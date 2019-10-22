package de.strifel.vbans.commands;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.Util;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CommandTempBan implements Command {
    private final ProxyServer server;

    public CommandTempBan(ProxyServer server) {
        this.server = server;
    }

    public void execute(CommandSource commandSource, @NonNull String[] strings) {
        if (strings.length > 1) {
            Optional<Player> oPlayer = server.getPlayer(strings[0]);
            if (oPlayer.isPresent()) {
                Player player = oPlayer.get();
                if (!player.hasPermission("VBans.prevent") || commandSource instanceof ConsoleCommandSource) {
                    long duration;
                    {
                        String durationString = strings[1];
                        if (Util.isInt(durationString)) {
                            duration = 60 * 60 * 24 * Integer.parseInt(durationString);
                        } else if (durationString.endsWith("d")) {
                            durationString = durationString.replace("d", "");
                            if (!Util.isInt(durationString)) return;
                            duration = 60 * 60 * 24 * Integer.parseInt(durationString);
                        } else if (durationString.endsWith("h")) {
                            durationString = durationString.replace("h", "");
                            if (!Util.isInt(durationString)) return;
                            duration = 60 * 60 * Integer.parseInt(durationString);
                        } else if (durationString.endsWith("m")) {
                            durationString = durationString.replace("m", "");
                            if (!Util.isInt(durationString)) return;
                            duration = 60 * Integer.parseInt(durationString);
                        } else if (durationString.endsWith("s")) {
                            durationString = durationString.replace("s", "");
                            if (!Util.isInt(durationString)) return;
                            duration = 60 * Integer.parseInt(durationString);
                        } else {
                            commandSource.sendMessage(TextComponent.of("Time could not be read use d,h,m or s as suffix for time!").color(TextColor.RED));
                            return;
                        }
                    }
                    long end = (System.currentTimeMillis() / 1000) + duration;
                    String reason = "You are being kicked!";
                    if  (strings.length > 2 && commandSource.hasPermission("VBans.temp.reason")) {
                        reason = String.join(" ", Arrays.copyOfRange(strings, 2, strings.length));
                    }
                    player.disconnect(Util.formatBannedMessage(commandSource instanceof ConsoleCommandSource ? "Console" : ((Player)commandSource).getUsername(), reason, end));
                    commandSource.sendMessage(TextComponent.of("You banned " + strings[0] + " for " + duration + " seconds!").color(TextColor.YELLOW));
                } else {
                    commandSource.sendMessage(TextComponent.of("You are not allowed to Ban this player!").color(TextColor.RED));
                }
            } else {
                commandSource.sendMessage(TextComponent.of("Player not found!").color(TextColor.RED));
            }
        } else {
            commandSource.sendMessage(TextComponent.of("Usage: /tempban <player> <time> [reason]").color(TextColor.RED));
        }
    }

    public List<String> suggest(CommandSource source, @NonNull String[] currentArgs) {
        if (currentArgs.length == 1) {
            return Util.getAllPlayernames(server);
        }
        if (currentArgs.length == 2) {
            return Arrays.asList("30d", "12h", "30m", "5s");
        }
        return new ArrayList<String>();
    }

    public boolean hasPermission(CommandSource source, @NonNull String[] args) {
        return source.hasPermission("VBans.temp");
    }

}
