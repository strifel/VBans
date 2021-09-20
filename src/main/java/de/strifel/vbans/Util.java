package de.strifel.vbans;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.database.Ban;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


public class Util {

    public static final TextColor COLOR_RED = TextColor.fromCSSHexString("FF5555");
    public static final TextColor COLOR_YELLOW = TextColor.fromCSSHexString("FFFF55");
    public static final TextColor COLOR_DARK_GREEN = TextColor.fromCSSHexString("00AA00");


    public final static SimpleDateFormat UNBAN_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    static String BAN_TEMPLATE = "";

    public static List<String> getAllPlayernames(ProxyServer server) {
        List<String> players = new ArrayList<>();
        for (Player player : server.getAllPlayers()) players.add(player.getUsername());
        return players;
    }

    public static Component formatBannedMessage(String bannedBy, String reason, long expires) {
        String bannedUntil = "forever";
        if (expires != -1) {
            bannedUntil = UNBAN_DATE_FORMAT.format(expires * 1000);
        }
        return Component.text(BAN_TEMPLATE.replace("$bannedUntil", bannedUntil).replace("$reason", reason).replace("$bannedBy", bannedBy));
    }

    public static Component formatBannedMessage(Ban ban, VBans vbans) {
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
                player.sendMessage(Component.text(message));
            }
        }
        server.getConsoleCommandSource().sendMessage(Component.text(message));
    }

    public static boolean hasOfflineProtectBanPermission(String uuid, VBans vBans) {
        try {
            // Things are not imported here so the plugin can run without Luckperms
            if (vBans.luckPermsApi != null) {
                net.luckperms.api.LuckPerms luckPerms = (net.luckperms.api.LuckPerms) vBans.luckPermsApi;
                try {
                    net.luckperms.api.model.user.User user = luckPerms.getUserManager().loadUser(UUID.fromString(uuid)).get();
                    if (user != null) {
                        net.luckperms.api.context.ContextManager contextManager = luckPerms.getContextManager();
                        net.luckperms.api.context.ImmutableContextSet contextSet = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);

                        net.luckperms.api.cacheddata.CachedPermissionData permissionData = user.getCachedData().getPermissionData(net.luckperms.api.query.QueryOptions.contextual(contextSet));

                        return permissionData.checkPermission("*").asBoolean()
                                || permissionData.checkPermission("VBans.*").asBoolean()
                                || permissionData.checkPermission("VBans.prevent").asBoolean();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    return false;
                }
            }
        } catch(NoClassDefFoundError e) {
            // it does not exist on the classpath
        }
        return false;
    }
}
