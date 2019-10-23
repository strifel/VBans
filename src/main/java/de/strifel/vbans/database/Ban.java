package de.strifel.vbans.database;

import de.strifel.vbans.VBans;

import java.sql.SQLException;

public class Ban {
    private String player, by, reason;
    private long until;

    Ban(String player, String by, String reason, long until) {
        this.player = player;
        this.by = by;
        this.reason = reason;
        this.until = until;
    }

    public String getUsername(VBans vbans) {
        try {
            return vbans.getDatabaseConnection().getUsername(player);
        } catch (SQLException e) {
            return "ERROR";
        }
    }

    public String getBannedByUsername(VBans vbans) {
        try {
            if (by.equals("Console")) return by;
            return vbans.getDatabaseConnection().getUsername(by);
        } catch (SQLException e) {
            return "ERROR";
        }
    }

    public String getPlayer() {
        return player;
    }

    public String getBannedBy() {
        return by;
    }

    public String getReason() {
        return reason;
    }

    public long getUntil() {
        return until;
    }
}
