package de.strifel.vbans.database;

import com.velocitypowered.api.proxy.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("SqlResolve")
public class DatabaseConnection {
    private final Connection connection;

    private static final String INSERT_BAN = "INSERT INTO ban_bans (user, until, bannedBy, reason, bannedTime) VALUES (?, ?, ?, ?, ?)";
    private static final String IS_BANNED = "SELECT reason, until, bannedBy FROM ban_bans WHERE purged IS NULL and (until = -1 or until > ?) and user = ? LIMIT 1";
    private static final String SET_USERNAME = "INSERT INTO ban_nameCache (user, username) VALUES (?, ?)";
    private static final String UPDATE_USERNAME = "UPDATE ban_nameCache SET username=? WHERE user=?";
    private static final String GET_USERNAME = "SELECT username FROM ban_nameCache WHERE user=? LIMIT 1";
    private static final String GET_UUID = "SELECT user FROM ban_nameCache WHERE username=? LIMIT 1";
    private static final String PURGE_BANS = "UPDATE ban_bans SET purged=? WHERE purged IS NULL and (until = -1 or until > ?) and user = ?";
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
        statement.setString(2, userUUID);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return new Ban(userUUID, result.getString("bannedBy"), result.getString("reason"), result.getLong("until"));
        } else {
            return null;
        }
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
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS ban_bans (id int(12) NOT NULL AUTO_INCREMENT, user text(36) NOT NULL, bannedBy text(36) NOT NULL, until int(64), bannedTime int(64), reason text(512), purged text(36), primary key (id))");
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS ban_nameCache (user text(36), username text(16))");
    }


    public void purgeActiveBans(String userUUID, String purgerUUID) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(PURGE_BANS);
        statement.setString(1, purgerUUID);
        statement.setLong(2, System.currentTimeMillis() / 1000);
        statement.setString(3, userUUID);
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
