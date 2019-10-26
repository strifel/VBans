package de.strifel.vbans.database;

import de.strifel.vbans.VBans;

import java.sql.SQLException;

public class Ban {
    private String player, by, reason;
    long until, bannedAt, reducedUntil, id;

    Ban(long id, String player, String by, String reason, long until, long bannedAt, long reducedUntil) {
        this.player = player;
        this.by = by;
        this.reason = reason;
        this.until = until;
        this.bannedAt = bannedAt;
        this.reducedUntil = reducedUntil;
        this.id = id;
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
        if (reducedUntil != 0) return reducedUntil;
        return until;
    }

    public long getId() {
        return id;
    }

    public long getBannedAt() {
        return bannedAt;
    }
}
