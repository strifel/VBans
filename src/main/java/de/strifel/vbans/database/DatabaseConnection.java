package de.strifel.vbans.database;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("SqlResolve")
public class DatabaseConnection {
    private final Connection connection;

    public static final String BANED_CRITERIA = "purged IS NULL and ((reducedUntil is NULL and (until = -1 or until > ?)) or (reducedUntil = -1 or reducedUntil > ?))";

    private static final String INSERT_BAN = "INSERT INTO ban_bans (user, until, bannedBy, reason, issuedAt) VALUES (?, ?, ?, ?, ?)";
    private static final String IS_BANNED = "SELECT reason, until, bannedBy, reducedUntil, issuedAt FROM ban_bans WHERE " + BANED_CRITERIA + " and user = ? LIMIT 1";
    private static final String GET_BAN_HISTORY = "SELECT reason, until, bannedBy, reducedUntil, issuedAt, purged, reducedBy  FROM ban_bans WHERE user = ?";
    private static final String SET_USERNAME = "INSERT INTO ban_nameCache (user, username) VALUES (?, ?)";
    private static final String UPDATE_USERNAME = "UPDATE ban_nameCache SET username=? WHERE user=?";
    private static final String GET_USERNAME = "SELECT username FROM ban_nameCache WHERE user=? LIMIT 1";
    private static final String GET_UUID = "SELECT user FROM ban_nameCache WHERE username=? LIMIT 1";
    private static final String PURGE_BANS = "UPDATE ban_bans SET purged=? WHERE " + BANED_CRITERIA + " and user = ?";
    private static final String REDUCE_BANS = "UPDATE ban_bans SET reducedUntil=?, reducedBy=?, reducedAt=? WHERE " + BANED_CRITERIA + " AND user=?";
    private static final String GET_USERNAMES_BASE = "SELECT username FROM ban_bans INNER JOIN ban_nameCache ON ban_bans.user = ban_nameCache.user WHERE GROUP BY username";

    public DatabaseConnection(String server, int port, String username, String password, String database) throws ClassNotFoundException, SQLException {
        synchronized (this) {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + server + ":" + port + "/" + database, username, password);
            createDefaultTable();
        }
    }

    public void addBan(String banned, long until, String by, String reason) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(INSERT_BAN);
        statement.setString(1, banned);
        statement.setLong(2, until);
        statement.setString(3, by);
        statement.setString(4, reason);
        statement.setLong(5, System.currentTimeMillis() / 1000);
        statement.executeUpdate();
    }

    public Ban getBan(String userUUID) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(IS_BANNED);
        statement.setLong(1, System.currentTimeMillis() / 1000);
        statement.setLong(2, System.currentTimeMillis() / 1000);
        statement.setString(3, userUUID);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return new Ban(userUUID, result.getString("bannedBy"), result.getString("reason"), result.getLong("until"), result.getLong("issuedAt"), result.getLong("reducedUntil"));
        } else {
            return null;
        }
    }

    public ArrayList<HistoryBan> getBanHistory(String userUUID, boolean includePurged) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(GET_BAN_HISTORY + (includePurged ? "" : " AND PURGED IS NULL"));
        statement.setString(1, userUUID);
        ResultSet result = statement.executeQuery();
        ArrayList<HistoryBan> bans = new ArrayList<>();
        if (result.next()) {
            do {
                bans.add(new HistoryBan(userUUID, result.getString("bannedBy"), result.getString("reason"), result.getLong("until"), result.getLong("issuedAt"), result.getLong("reducedUntil"), result.getString("purged"), result.getString("reducedBy")));
            } while (result.next());
        }
        return bans;
    }

    String getUsername(String userUUID) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(GET_USERNAME);
        statement.setString(1, userUUID);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return result.getString("username");
        } else {
            return null;
        }
    }


    public String getUUID(String username) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(GET_UUID);
        statement.setString(1, username);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return result.getString("user");
        } else {
            return null;
        }

    }

    public void setUsername(String userUUID, String username) throws SQLException {
        String inDatabase = getUsername(userUUID);
        if (inDatabase == null) {
            PreparedStatement statement = connection.prepareStatement(SET_USERNAME);
            statement.setString(1, userUUID);
            statement.setString(2, username);
            statement.executeUpdate();
        } else if (!inDatabase.equals(username)) {
            PreparedStatement statement = connection.prepareStatement(UPDATE_USERNAME);
            statement.setString(1, username);
            statement.setString(2, userUUID);
            statement.executeUpdate();
        }
    }

    private void createDefaultTable() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS ban_bans (id int(12) NOT NULL AUTO_INCREMENT, user text(36) NOT NULL, bannedBy text(36) NOT NULL, until int(64), issuedAt int(64), reducedUntil int(64), reducedBy text(36), reducedAt int(64), reason text(512), purged text(36), primary key (id))");
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS ban_nameCache (user text(36), username text(16))");
    }


    public void purgeActiveBans(String userUUID, String purgerUUID) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(PURGE_BANS);
        statement.setString(1, purgerUUID);
        statement.setLong(2, System.currentTimeMillis() / 1000);
        statement.setLong(3, System.currentTimeMillis() / 1000);
        statement.setString(4, userUUID);
        statement.executeUpdate();
    }


    public void reduceBanTo(String userUUID, String reducerUUID, long reduceTo) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(REDUCE_BANS);
        statement.setLong(1, reduceTo);
        statement.setString(2, reducerUUID);
        statement.setLong(3, System.currentTimeMillis() / 1000);
        statement.setLong(4, System.currentTimeMillis() / 1000);
        statement.setLong(5, System.currentTimeMillis() / 1000);
        statement.setString(6, userUUID);
        statement.executeUpdate();
    }

    public List<String> getUsernamesByQuery(String query) throws SQLException {
        ResultSet results = connection.createStatement().executeQuery(GET_USERNAMES_BASE.replace("WHERE", query));
        List<String> usernames = new ArrayList<>();
        if (results.next()) {
            do {
                usernames.add(results.getString("username"));
            } while(results.next());
        }
        return usernames;
    }
}
