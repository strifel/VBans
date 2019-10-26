package de.strifel.vbans;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.database.Ban;
import net.kyori.text.TextComponent;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Util {

    public static List<String> getAllPlayernames(ProxyServer server) {
        List<String> players = new ArrayList<>();
        for (Player player : server.getAllPlayers()) players.add(player.getUsername());
        return players;
    }

    private final static String BAN_TEMPLATE = "§4§lBANNED:§r\n§aUntil: $bannedUntil§r\n§3Reason: §r§b$reason§r\n§cBanned by: $bannedBy§r";
    public final static SimpleDateFormat UNBAN_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public static TextComponent formatBannedMessage(String bannedBy, String reason, long expires) {
        String bannedUntil = "forever";
        if (expires != -1) {
            bannedUntil = UNBAN_DATE_FORMAT.format(expires * 1000);
        }
        return TextComponent.of(BAN_TEMPLATE.replace("$bannedUntil", bannedUntil).replace("$reason", reason).replace("$bannedBy", bannedBy));
    }

    public static TextComponent formatBannedMessage(Ban ban, VBans vbans) {
        return formatBannedMessage(ban.getBannedByUsername(vbans), ban.getReason(), ban.getUntil());
    }
    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
}
