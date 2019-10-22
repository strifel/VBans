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

public class CommandKick implements Command {
    private final ProxyServer server;

    public CommandKick(ProxyServer server) {
        this.server = server;
    }


    public void execute(CommandSource commandSource, @NonNull String[] strings) {
        if (strings.length > 0) {
            Optional<Player> oPlayer = server.getPlayer(strings[0]);
            if (oPlayer.isPresent()) {
                Player player = oPlayer.get();
                if (!player.hasPermission("VBans.prevent") || commandSource instanceof ConsoleCommandSource) {
                    String reason = "You are being kicked!";
                    if  (strings.length > 1 && commandSource.hasPermission("VBans.kick.reason")) {
                        reason = String.join(" ", Arrays.copyOfRange(strings, 1, strings.length));
                    }
                    player.disconnect(TextComponent.of(reason));
                    commandSource.sendMessage(TextComponent.of("You kicked " + strings[0]).color(TextColor.YELLOW));
                } else {
                    commandSource.sendMessage(TextComponent.of("You are not allowed to kick this player!").color(TextColor.RED));
                }
            } else {
                commandSource.sendMessage(TextComponent.of("Player not found!").color(TextColor.RED));
            }
        } else {
            commandSource.sendMessage(TextComponent.of("Usage: /kick <player> [reason]").color(TextColor.RED));
        }
    }

    public List<String> suggest(CommandSource source, @NonNull String[] currentArgs) {
        if (currentArgs.length == 1) {
            return Util.getAllPlayernames(server);
        }
        return new ArrayList<String>();
    }

    public boolean hasPermission(CommandSource source, @NonNull String[] args) {
        return source.hasPermission("VBans.kick");
    }
}
