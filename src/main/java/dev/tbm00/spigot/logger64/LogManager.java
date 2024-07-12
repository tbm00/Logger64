package dev.tbm00.spigot.logger64;

import java.util.ArrayList;
import java.util.List;

import java.sql.*;

import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.IPEntry;

public class LogManager {

    private final MySQLConnection db;

    public LogManager(MySQLConnection db) {
        this.db = db;
    }

    public void reloadConnection() {
        // reload MySQL connection
        db.closeConnection();
        db.openConnection();
    }

    // returns player entry from sql
    // if not found, returns null
    public PlayerEntry getPlayerEntry(String username) {
        try (PreparedStatement statement = db.getConnection()
        .prepareStatement("SELECT * FROM logger64_players WHERE username = ?")) {
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
            e.printStackTrace();
        }
        return null;
    }

    // creates player entry in sql if DNE
    // updates player entry in sql if it does exist
    public void savePlayerEntry(PlayerEntry playerEntry, String ip, String username, java.util.Date date) {
        String query;
        boolean exists = getPlayerEntry(username) != null;
        if (exists) {
            query = "UPDATE logger64_players SET known_ips = ?, latest_ip = ?, latest_date = ? WHERE username = ?";
        } else {
            query = "INSERT INTO logger64_players (username, known_ips, first_ip, latest_ip, first_date, latest_date) VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (PreparedStatement statement = db.getConnection().prepareStatement(query)) {
            if (exists) {
                List<String> knownIPs = new ArrayList<>(playerEntry.getKnownIPs());
                if (!knownIPs.contains(ip)) {
                    knownIPs.add(ip);
                }
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
            System.out.println("Exception: Could not save player entry to SQL!");
            e.printStackTrace();
        }
    }

    // returns list of known IPs from sql
    // if empty or DNE, returns null
    public List<String> getKnownIPs(String username) {
        PlayerEntry playerEntry = getPlayerEntry(username);
        if (playerEntry != null) {
            return playerEntry.getKnownIPs();
        }
        return null;
    }

    // returns ip entry from sql
    // if not found, returns null
    public IPEntry getIPEntry(String ip) {
        try (PreparedStatement statement = db.getConnection()
        .prepareStatement("SELECT * FROM logger64_ips WHERE ip = ?")) {
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
            e.printStackTrace();
        }
        return null;
    }
    
    // creates ip entry in sql if DNE
    // updates ip entry in sql if it does exist
    public void saveIPEntry(IPEntry ipEntry, String ip, String username, java.util.Date date) {
        String query;
        boolean exists = getIPEntry(ip) != null;
        if (exists) {
            query = "UPDATE logger64_ips SET known_usernames = ?, latest_username = ?, latest_date = ? WHERE ip = ?";
        } else {
            query = "INSERT INTO logger64_ips (ip, known_usernames, first_username, latest_username, first_date, latest_date) VALUES (?, ?, ?, ?, ?, ?)";
        }
        
        try (PreparedStatement statement = db.getConnection().prepareStatement(query)) {
            if (exists) {
                List<String> knownUsernames = new ArrayList<>(ipEntry.getKnownUsernames());
                if (!knownUsernames.contains(username)) {
                    knownUsernames.add(username);
                }
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
            e.printStackTrace();
        }
    }

    // returns list of known usernames from sql
    // if empty or DNE, returns null
    public List<String> getKnownUsernames(String ip) {
        IPEntry ipEntry = getIPEntry(ip);
        if (ipEntry != null) {
            return ipEntry.getKnownUsernames();
        }
        return null;
    }
}