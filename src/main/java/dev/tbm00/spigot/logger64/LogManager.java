package dev.tbm00.spigot.logger64;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.IPEntry;

public class LogManager {
    private final JavaPlugin javaPlugin;
    private final MySQLConnection db;

    public LogManager(JavaPlugin javaPlugin, MySQLConnection db) {
        this.javaPlugin = javaPlugin;
        this.db = db;
    }

    // returns if the player entry for username exists
    public boolean playerEntryExists(String username) {
        String query = "SELECT 1 FROM logger64_players WHERE username = ? LIMIT 1";
    
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Exception when finding player entry: " + username);
            e.printStackTrace();
        }
        return false;
    }

    // returns player entry from sql
    // if not found, returns null
    public PlayerEntry getPlayerEntry(String username) {
        String query = "SELECT * FROM logger64_players WHERE username = ?";
        
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String knownIPs = rs.getString("known_ips");
                    String firstIP = rs.getString("first_ip");
                    String latestIP = rs.getString("latest_ip");
                    Date firstDate = rs.getDate("first_date");
                    Date latestDate = rs.getDate("latest_date");

                    return new PlayerEntry(username, List.of(knownIPs.split(",")), firstIP, latestIP, firstDate, latestDate);
                }
            }
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Exception when getting player entry: " + username);
            e.printStackTrace();
        }
        return null;
    }

    // creates player entry in sql if DNE
    // updates player entry in sql if it does exist
    @SuppressWarnings("all")
    public void savePlayerEntry(PlayerEntry playerEntry, String ip, String username, java.util.Date date) {
        boolean exists = (playerEntry != null);
        //boolean exists = playerEntryExists(username);

        List<String> knownIPs;
        String query;

        if (exists) {
            query = "UPDATE logger64_players SET known_ips = ?, latest_ip = ?, latest_date = ? WHERE username = ?";
            knownIPs = new ArrayList<>(playerEntry.getKnownIPs());
            if (!knownIPs.contains(ip)) knownIPs.add(ip);
        } else {
            query = "INSERT INTO logger64_players (username, known_ips, first_ip, latest_ip, first_date, latest_date) VALUES (?, ?, ?, ?, ?, ?)";
            knownIPs = new ArrayList<>();
            knownIPs.add(ip);
        }

        try (Connection connection = db.getConnection();
            PreparedStatement statement = connection.prepareStatement(query)) {
            if (exists) {
                statement.setString(1, String.join(",", knownIPs));
                statement.setString(2, ip);
                statement.setDate(3, new java.sql.Date(date.getTime()));
                statement.setString(4, username);
            } else {
                statement.setString(1, username);
                statement.setString(2, ip);
                statement.setString(3, ip);
                statement.setString(4, ip);
                statement.setDate(5, new java.sql.Date(date.getTime()));
                statement.setDate(6, new java.sql.Date(date.getTime()));
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Exception when saving player entry: " + username);
            e.printStackTrace();
        }
    }

    // returns list of known usernames from sql
    // if empty or DNE, returns null
    public List<String> getKnownUsernames(String ip) {
        IPEntry ipEntry = getIPEntry(ip);
        return ipEntry != null ? ipEntry.getKnownUsernames() : null;
    }

    // returns list of known IPs from sql
    // if empty or DNE, returns null
    public List<String> getKnownIPs(String username) {
        PlayerEntry playerEntry = getPlayerEntry(username);
        return playerEntry != null ? playerEntry.getKnownIPs() : null;
    }

    // returns if the ip entry for ip exists
    public boolean IPEntryExists(String ip) {
        String query = "SELECT 1 FROM logger64_ips WHERE ip = ? LIMIT 1";
    
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, ip);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Exception when finding ip entry: " + ip);
            e.printStackTrace();
        }
        return false;
    }

    // returns ip entry from sql
    // if not found, returns null
    public IPEntry getIPEntry(String ip) {
        String query = "SELECT * FROM logger64_ips WHERE ip = ?";
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, ip);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String knownUsernames = rs.getString("known_usernames");
                    String firstUsername = rs.getString("first_username");
                    String latestUsername = rs.getString("latest_username");
                    Date firstDate = rs.getDate("first_date");
                    Date latestDate = rs.getDate("latest_date");

                    return new IPEntry(ip, List.of(knownUsernames.split(",")), firstUsername, latestUsername, firstDate, latestDate);
                }
            }
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Exception when getting ip entry: " + ip);
            e.printStackTrace();
        }
        return null;
    }
    
    // creates ip entry in sql if DNE
    // updates ip entry in sql if it does exist
    @SuppressWarnings("all")
    public void saveIPEntry(IPEntry ipEntry, String ip, String username, java.util.Date date) {
        boolean exists = (ipEntry != null);
        //boolean exists = IPEntryExists(ip);
        
        List<String> knownUsernames;
        String query;
        
        if (exists) {
            query = "UPDATE logger64_ips SET known_usernames = ?, latest_username = ?, latest_date = ? WHERE ip = ?";
            knownUsernames = new ArrayList<>(ipEntry.getKnownUsernames());
            if (!knownUsernames.contains(username)) knownUsernames.add(username);
        } else {
            query = "INSERT INTO logger64_ips (ip, known_usernames, first_username, latest_username, first_date, latest_date) VALUES (?, ?, ?, ?, ?, ?)";
            knownUsernames = new ArrayList<>();
            knownUsernames.add(username);
        }
        
        try (Connection connection = db.getConnection();
            PreparedStatement statement = connection.prepareStatement(query)) {
            if (exists) {
                statement.setString(1, String.join(",", knownUsernames));
                statement.setString(2, username);
                statement.setDate(3, new java.sql.Date(date.getTime()));
                statement.setString(4, ip);
            } else {
                statement.setString(1, ip);
                statement.setString(2, username);
                statement.setString(3, username);
                statement.setString(4, username);
                statement.setDate(5, new java.sql.Date(date.getTime()));
                statement.setDate(6, new java.sql.Date(date.getTime()));
            }

            statement.executeUpdate();
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Exception when saving ip entry: " + ip);
            e.printStackTrace();
        }
    }
}