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

public class CommandBan implements Command {
    private final ProxyServer server;

    public CommandBan(ProxyServer server) {
        this.server = server;
    }


    public void execute(CommandSource commandSource, @NonNull String[] strings) {
        if (strings.length > 0) {
            Optional<Player> oPlayer = server.getPlayer(strings[0]);
            if (oPlayer.isPresent()) {
                Player player = oPlayer.get();
                if (!player.hasPermission("VBans.prevent") || commandSource instanceof ConsoleCommandSource) {
                    String reason = "The Ban Hammer has spoken!";
                    if  (strings.length > 1 && commandSource.hasPermission("VBans.ban.reason")) {
                        reason = String.join(" ", Arrays.copyOfRange(strings, 1, strings.length));
                    }
                    player.disconnect(Util.formatBannedMessage(commandSource instanceof ConsoleCommandSource ? "Console" : ((Player)commandSource).getUsername(), reason, -1));
                    commandSource.sendMessage(TextComponent.of("You banned " + strings[0]).color(TextColor.YELLOW));
                } else {
                    commandSource.sendMessage(TextComponent.of("You are not allowed to ban this player!").color(TextColor.RED));
                }
            } else {
                commandSource.sendMessage(TextComponent.of("Player not found!").color(TextColor.RED));
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
