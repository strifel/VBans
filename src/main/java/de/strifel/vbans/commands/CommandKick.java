package de.strifel.vbans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.Util;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.DatabaseConnection;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static de.strifel.vbans.Util.COLOR_RED;
import static de.strifel.vbans.Util.COLOR_YELLOW;

public class CommandKick implements SimpleCommand {
    private final ProxyServer server;
    private final DatabaseConnection database;
    private final String DEFAULT_REASON;
    private final String KICK_LAYOUT;

    public CommandKick(VBans vbans) {
        this.server = vbans.getServer();
        database = vbans.getDatabaseConnection();
        DEFAULT_REASON = vbans.getMessages().getString("StandardKickMessage");
        KICK_LAYOUT = vbans.getMessages().getString("KickLayout");
    }

    @Override
    public void execute(Invocation commandInvocation) {
        String[] strings = commandInvocation.arguments();
        CommandSource commandSource = commandInvocation.source();

        if (strings.length > 0) {
            Optional<Player> oPlayer = server.getPlayer(strings[0]);
            if (oPlayer.isPresent()) {
                Player player = oPlayer.get();
                if (!player.hasPermission("VBans.prevent") || commandSource instanceof ConsoleCommandSource) {
                    String reason = DEFAULT_REASON;
                    if (strings.length > 1 && commandSource.hasPermission("VBans.kick.reason")) {
                        reason = String.join(" ", Arrays.copyOfRange(strings, 1, strings.length));
                    }
                    player.disconnect(Component.text(KICK_LAYOUT.replace("$reason", reason)));
                    try {
                        database.addBan(player.getUniqueId().toString(), System.currentTimeMillis() / 1000, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), reason);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        commandSource.sendMessage(Component.text("Your kick can not be registered.").color(COLOR_RED));
                    }
                    commandSource.sendMessage(Component.text("You kicked " + strings[0]).color(COLOR_YELLOW));
                } else {
                    commandSource.sendMessage(Component.text("You are not allowed to kick this player!").color(COLOR_RED));
                }
            } else {
                commandSource.sendMessage(Component.text("Player not found!").color(COLOR_RED));
            }
        } else {
            commandSource.sendMessage(Component.text("Usage: /kick <player> [reason]").color(COLOR_RED));
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
        return commandInvocation.source().hasPermission("VBans.kick");
    }
}
