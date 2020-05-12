package de.strifel.vbans;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.database.Ban;
import net.kyori.text.TextComponent;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


public class Util {

    public final static SimpleDateFormat UNBAN_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    static String BAN_TEMPLATE = "";

    public static List<String> getAllPlayernames(ProxyServer server) {
        List<String> players = new ArrayList<>();
        for (Player player : server.getAllPlayers()) players.add(player.getUsername());
        return players;
    }

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
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void broadcastMessage(String message, String permission, ProxyServer server) {
        for (Player player : server.getAllPlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                player.sendMessage(TextComponent.of(message));
            }
        }
        server.getConsoleCommandSource().sendMessage(TextComponent.of(message));
    }

    public static boolean hasOfflineProtectBanPermission(String uuid, VBans vBans) {
        if (vBans.luckPermsApi != null) {
            try {
                User user = vBans.luckPermsApi.getUserManager().loadUser(UUID.fromString(uuid)).get();
                if (user != null) {

                    ContextManager contextManager = vBans.luckPermsApi.getContextManager();
                    ImmutableContextSet contextSet = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);

                    CachedPermissionData permissionData = user.getCachedData().getPermissionData(QueryOptions.contextual(contextSet));

                    return permissionData.checkPermission("*").asBoolean()
                            || permissionData.checkPermission("VBans.*").asBoolean()
                            || permissionData.checkPermission("VBans.prevent").asBoolean();
                }
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }
        return false;
    }
}
