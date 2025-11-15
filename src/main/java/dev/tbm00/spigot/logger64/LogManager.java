package dev.tbm00.spigot.logger64;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.sql.*;

import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.IPEntry;

public class LogManager {
    private final JavaPlugin javaPlugin;
    private final MySQLConnection db;
    public final Set<String> nonLoggedIPs;

    public LogManager(JavaPlugin javaPlugin, MySQLConnection db) {
        this.javaPlugin = javaPlugin;
        this.db = db;
        this.nonLoggedIPs = new HashSet<>(javaPlugin.getConfig().getStringList("logger.nonLoggedIPs"));
    }

    public void logNewEntry(String username, String ipHostAddress) {
        if (nonLoggedIPs.contains(ipHostAddress)) return;

        PlayerEntry playerEntry = getPlayerEntry(username);
        IPEntry ipEntry = getIPEntry(ipHostAddress);
        java.util.Date date = new java.util.Date();

        savePlayerEntry(playerEntry, ipHostAddress, username, date);
        saveIPEntry(ipEntry, ipHostAddress, username, date);
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

    // returns player's last join date from sql
    // if not found, returns null
    public Date getLastSeen(String username) {
        String query = "SELECT * FROM logger64_players WHERE username = ?";
        
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("latest_date");
                }
            }
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Exception when getting player entry: " + username);
            e.printStackTrace();
        }
        return null;
    }

    // returns player's first join date from sql
    // if not found, returns null
    public Date getFirstJoin(String username) {
        String query = "SELECT * FROM logger64_players WHERE username = ?";
        
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("first_date");
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

    // returns list of known IPs from sql
    // if empty or DNE, returns null
    public List<String> getKnownIPsByCidr(String block) {
        // convert string block to usable data
        InetAddress cidrAddress = StaticUtils.getCidrInetAddress(block);
        if (cidrAddress == null) return null;
        byte[] addressBytes = cidrAddress.getAddress();

        Integer prefixLengthObj = StaticUtils.getCidrPrefixLength(block);
        if (prefixLengthObj == null) return null;
        int prefixLength = prefixLengthObj;
        
        if (addressBytes.length != 4) {
            javaPlugin.getLogger().severe("Only IPv4 CIDR blocks are supported for SQL lookup: " + block);
            return null;
        } if (prefixLength < 0 || prefixLength > 32) {
            javaPlugin.getLogger().severe("Invalid CIDR prefix length for IPv4: " + prefixLength + " (block: " + block + ")");
            return null;
        }
    
        BigInteger ipValue = new BigInteger(1, addressBytes);
        BigInteger mask = BigInteger.valueOf(-1).shiftLeft(32 - prefixLength); // Assuming IPv4 here
    
        BigInteger networkAddress = ipValue.and(mask);
        BigInteger broadcastAddress = networkAddress.add(mask.not());
    
        String lowerBound = StaticUtils.convertBigIntegerToIp(networkAddress);
        String upperBound = StaticUtils.convertBigIntegerToIp(broadcastAddress);
        if (lowerBound == null || upperBound == null) {
            javaPlugin.getLogger().severe("Failed to compute IP bounds for CIDR: " + block);
            return null;
        }
    
        // query sql for in-bounds ips
        String query = "SELECT ip FROM logger64_ips WHERE INET_ATON(ip) BETWEEN INET_ATON(?) AND INET_ATON(?)";
        List<String> matchedIPs = new ArrayList<>();
    
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, lowerBound);
            statement.setString(2, upperBound);
    
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    matchedIPs.add(rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Exception when querying known IPs by CIDR: " + block);
            e.printStackTrace();
        }

        return matchedIPs;
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