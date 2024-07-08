package dev.tbm00.spigot.logger64;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.sql.*;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.LogEntry;

public class LogManager {

    private final MySQLConnection db;
    public final Map<String, String> username_map; // key = username
    private final Map<String, PlayerEntry> player_map; // key = UUID
    private final Map<String, Map<String, LogEntry>> rep_map; // key1 = initiatorUUID, key2= receiverUUID
    private final double defaultRep;

    public LogManager(MySQLConnection database, FileConfiguration fileConfig) {
        this.db = database;
        this.player_map = new HashMap<>();
        this.rep_map = new HashMap<>();
        this.username_map = new HashMap<>();
        this.defaultRep = fileConfig.getInt("repScoring.defaultRep");
    }

    public void reload() {
        // reload MySQL connection
        db.closeConnection();
        db.openConnection();

        // refresh caches with information for online players
        player_map.clear();
        username_map.clear();
        rep_map.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerCache(player.getName());
        }
    }

    public void reloadCache() {
        // refresh caches with information for online players
        player_map.clear();
        username_map.clear();
        rep_map.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerCache(player.getName());
        }
    }

    public void loadPlayerCache(String username) {
        String UUID = getPlayerUUID(username);
        if (UUID == null) {
            System.out.println("Error: Could not find UUID for username!");
            return;
        }

        // save to username_map
        username_map.put(username, UUID);

        // save to player_map
        PlayerEntry playerEntry = getPlayerEntry(UUID);
        if (playerEntry != null) {
            player_map.put(UUID, playerEntry);
        } else {
            System.out.println("Error: Could not find PlayerEntry for UUID!");
            return;
        }

        // save to rep_map
        try (PreparedStatement statement = db.getConnection()
                .prepareStatement("SELECT * FROM logger64_reps WHERE receiver_UUID = ? OR initiator_UUID = ?")) {
            statement.setString(1, UUID);
            statement.setString(2, UUID);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String initiatorUUID = resultSet.getString("initiator_UUID");
                    String receiverUUID = resultSet.getString("receiver_UUID");
                    int rep = resultSet.getInt("rep");
                    LogEntry repEntry = new LogEntry(id, initiatorUUID, receiverUUID, rep);

                    rep_map.putIfAbsent(initiatorUUID, new HashMap<>());
                    rep_map.get(initiatorUUID).put(receiverUUID, repEntry);
                }
            }
        } catch (SQLException e) {
            System.out.println("Exception: Could not load rep entries from SQL to cache!");
            e.printStackTrace();
        }
    }

    public void unloadPlayerCache(String username) {
        String UUID = getPlayerUUID(username);
        if (UUID == null) {
            System.out.println("Error: Could not find UUID for username!");
            return;
        }

        // remove from username_map
        username_map.remove(username);

        // remove from player_map
        player_map.remove(UUID);

        // remove from rep_map
        List<String> onlinePlayersUUIDs = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayersUUIDs.add(player.getUniqueId().toString());
        }
        for (Map.Entry<String, Map<String, LogEntry>> entry : rep_map.entrySet()) {
            if (!onlinePlayersUUIDs.contains(entry.getKey())) {
                entry.getValue().remove(UUID);
            }
        }
        if (!onlinePlayersUUIDs.contains(UUID)) {
            rep_map.remove(UUID);
        }
    }

    // returns player name from cache(map) first
    // if not found, returns player name from sql
    // if not found, returns null
    public String getPlayerUUID(String username) {
        if (username_map.containsKey(username)) {
            return username_map.get(username); // return UUID from map
        } else {
            try (PreparedStatement statement = db.getConnection()
                    .prepareStatement("SELECT * FROM logger64_players WHERE username = ?")) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) return resultSet.getString("uuid");
                }
            } catch (SQLException e) {
                System.out.println("Exception: Could not find UUID!");
                e.printStackTrace();
                return null;
            }
            System.out.println("Error: Could not find UUID!");
            return null;
        }
    }

    public String getPlayerUsername(String UUID) {
        try (PreparedStatement statement = db.getConnection()
                .prepareStatement("SELECT * FROM logger64_players WHERE uuid = ?")) {
            statement.setString(1, UUID);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getString("username");
            }
        } catch (SQLException e) {
            System.out.println("Exception: Could not find username!");
            e.printStackTrace();
            return null;
        }
        System.out.println("Error: Could not find username!");
        return null;
    }

    // returns player entry from cache(map) first
    // if not found, returns player entry from sql
    // if not found, returns null
    public PlayerEntry getPlayerEntry(String UUID) {
        if (player_map.containsKey(UUID)) {
            return player_map.get(UUID);
        } else {
            try (PreparedStatement statement = db.getConnection()
                    .prepareStatement("SELECT * FROM logger64_players WHERE uuid = ?")) {
                statement.setString(1, UUID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    PlayerEntry entry;
                    if (resultSet.next()) {
                        entry = new PlayerEntry(resultSet.getString("uuid"),
                                resultSet.getString("username"),
                                resultSet.getDouble("rep_avg"),
                                resultSet.getDouble("rep_avg_last"),
                                resultSet.getInt("rep_staff_modifier"),
                                resultSet.getDouble("rep_shown"),
                                resultSet.getDouble("rep_shown_last"),
                                resultSet.getInt("rep_count"),
                                resultSet.getDate("last_login"),
                                resultSet.getDate("last_logout"));
                        return entry;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Exception: Could not find player entry!");
                e.printStackTrace();
                return null;
            }
            System.out.println("Error: Could not find player entry!");
            return null;
        }
    }

    // saves player entry to cache(map) and database(SQL)
    public void savePlayerEntry(PlayerEntry playerEntryPassed) {
        PlayerEntry playerEntry = calculateRepAverage(playerEntryPassed);
        
        // check if player entry exists in cache(map)
        if (!player_map.containsKey(playerEntry.getPlayerUUID())) {
            // create new player entry in cache(map)
            player_map.put(playerEntry.getPlayerUUID(), playerEntry);
            username_map.put(playerEntry.getPlayerUsername(), playerEntry.getPlayerUUID());
        } else {
            // update existing entry in cache(map)
            player_map.put(playerEntry.getPlayerUUID(), playerEntry);
            username_map.put(playerEntry.getPlayerUsername(), playerEntry.getPlayerUUID());
        }

        // save to sql
        try (PreparedStatement statement = db.getConnection()
                .prepareStatement("REPLACE INTO logger64_players (uuid, username, rep_avg, rep_avg_last, rep_staff_modifier, rep_shown, rep_shown_last, rep_count, last_login, last_logout) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, playerEntry.getPlayerUUID());
            statement.setString(2, playerEntry.getPlayerUsername());
            statement.setDouble(3, playerEntry.getRepAverage());
            statement.setDouble(4, playerEntry.getRepAverageLast());
            statement.setInt(5, playerEntry.getRepStaffModifier());
            statement.setDouble(6, playerEntry.getRepShown());
            statement.setDouble(7, playerEntry.getRepShownLast());
            statement.setInt(8, playerEntry.getRepCount());
            statement.setDate(9, new java.sql.Date(playerEntry.getLastLogin().getTime()));
            statement.setDate(10, new java.sql.Date(playerEntry.getLastLogout().getTime()));
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Exception: Could not save player entry!");
            e.printStackTrace();
        }
    }

    // saves player entry to cache(map) and database(SQL)
    public void savePlayerEntry(PlayerEntry playerEntryPassed, int newModifier) {
        playerEntryPassed.setRepStaffModifier(newModifier);
        PlayerEntry playerEntry = calculateRepAverage(playerEntryPassed);

        // check if player entry exists in cache(map)
        if (!player_map.containsKey(playerEntry.getPlayerUUID())) {
            // create new player entry in cache(map)
            player_map.put(playerEntry.getPlayerUUID(), playerEntry);
            username_map.put(playerEntry.getPlayerUsername(), playerEntry.getPlayerUUID());
        } else {
            // update existing entry in cache(map)
            player_map.put(playerEntry.getPlayerUUID(), playerEntry);
            username_map.put(playerEntry.getPlayerUsername(), playerEntry.getPlayerUUID());
        }

        // save to sql
        try (PreparedStatement statement = db.getConnection()
                .prepareStatement("REPLACE INTO logger64_players (uuid, username, rep_avg, rep_avg_last, rep_staff_modifier, rep_shown, rep_shown_last, rep_count, last_login, last_logout) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, playerEntry.getPlayerUUID());
            statement.setString(2, playerEntry.getPlayerUsername());
            statement.setDouble(3, playerEntry.getRepAverage());
            statement.setDouble(4, playerEntry.getRepAverageLast());
            statement.setInt(5, playerEntry.getRepStaffModifier());
            statement.setDouble(6, playerEntry.getRepShown());
            statement.setDouble(7, playerEntry.getRepShownLast());
            statement.setInt(8, playerEntry.getRepCount());
            statement.setDate(9, new java.sql.Date(playerEntry.getLastLogin().getTime()));
            statement.setDate(10, new java.sql.Date(playerEntry.getLastLogout().getTime()));
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Exception: Could not save player entry!");
            e.printStackTrace();
        }
    }

    // returns rep entry from cache(map) first
    // if not found, returns rep entry from sql
    // if not found, returns null
    public LogEntry getRepEntry(String initiatorUUID, String receiverUUID) {
        if (rep_map.containsKey(initiatorUUID) && rep_map.get(initiatorUUID).containsKey(receiverUUID)) {
            return rep_map.get(initiatorUUID).get(receiverUUID);
        } else {
            try (PreparedStatement statement = db.getConnection()
                    .prepareStatement("SELECT * FROM logger64_reps WHERE initiator_UUID = ? AND receiver_UUID = ?")) {
                statement.setString(1, initiatorUUID);
                statement.setString(2, receiverUUID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    LogEntry repEntry = null;
                    if (resultSet.next()) {
                        repEntry = new LogEntry(resultSet.getInt("id"),
                                resultSet.getString("initiator_UUID"),
                                resultSet.getString("receiver_UUID"),
                                resultSet.getInt("rep"));
                    }
                    return repEntry;
                }
            } catch (SQLException e) {
                System.out.println("Exception: Could not find rep entry in SQL!");
                e.printStackTrace();
            }
            System.out.println("Error: Could not find rep entry in SQL!");
            return null;
        }
    }

    public Set<String> getRepInitiators(String receiverUUID) {
        Set<String> initiatorList = new HashSet<>();

        // get receiver list from SQL
        try (PreparedStatement selectStatement = db.getConnection()
            .prepareStatement("SELECT initiator_UUID FROM logger64_reps WHERE receiver_UUID = ?")) {
            selectStatement.setString(1, receiverUUID);
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    initiatorList.add(getPlayerUsername(resultSet.getString("initiator_UUID")));
                }
            }
            return initiatorList;
        } catch (SQLException e) {
            System.out.println("Exception: Could not fetch initiator list!");
            e.printStackTrace();
            return null;
        }
    }

    public Set<String> getRepReceivers(String initiatorUUID) {
        Set<String> receiverList = new HashSet<>();
        // get receiver list from SQL
        try (PreparedStatement selectStatement = db.getConnection()
            .prepareStatement("SELECT receiver_UUID FROM logger64_reps WHERE initiator_UUID = ?")) {
            selectStatement.setString(1, initiatorUUID);
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    receiverList.add(getPlayerUsername(resultSet.getString("receiver_UUID")));
                }
            }
            return receiverList;
        } catch (SQLException e) {
            System.out.println("Exception: Could not fetch receiver list!");
            e.printStackTrace();
            return null;
        }
    }

    public PlayerEntry calculateRepAverage(PlayerEntry entry) {
        String UUID = entry.getPlayerUUID();
        try (PreparedStatement statement = db.getConnection()
                .prepareStatement("SELECT * FROM logger64_reps WHERE receiver_UUID = ?")) {
            statement.setString(1, UUID);
    
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Double> repList = new ArrayList<>();

                while(resultSet.next()) {
                    repList.add((double) resultSet.getInt("rep"));
                }

                // save prior rep averages
                double lastRepAvg = entry.getRepAverage();
                double lastRepShown = entry.getRepShown();
                entry.setRepAverageLast(lastRepAvg);
                entry.setRepShownLast(lastRepShown);

                // calcuate new rep count
                int newCount = 0;
                if (repList.size() != 0) {
                    newCount = repList.size();
                }

                // calculate new average
                double newAverage = defaultRep;
                if (!repList.isEmpty()) {
                    // calculate new rep average
                    double sum = 0.0;
                    for (double rep : repList) sum += rep;
                    newAverage = sum / newCount;
                }
                
                // apply staff modifier and save
                double staffMod = entry.getRepStaffModifier();
                entry.setRepShown(newAverage + staffMod);
                entry.setRepAverage(newAverage);
                entry.setRepCount(newCount);
                return entry;
            }
        } catch (SQLException e) {
            System.out.println("Exception: Could not calculate rep average!");
            e.printStackTrace();
            return null;
        }
    }

    // saves rep entry to database(SQL) then refreshes cache(maps)
    public void saveRepEntry(LogEntry repEntry) {
        String initiatorUUID = repEntry.getInitiatorUUID();
        String receiverUUID = repEntry.getReceiverUUID();

        boolean isNewEntry = (!rep_map.containsKey(initiatorUUID) || 
                              !rep_map.get(initiatorUUID).containsKey(receiverUUID));
    
        if (isNewEntry) {
            // create new rep entry in cache(map)
            rep_map.putIfAbsent(initiatorUUID, new HashMap<>());
            rep_map.get(initiatorUUID).put(receiverUUID, repEntry);
    
            // increase rep_count in cache(map)
            if (player_map.containsKey(receiverUUID)) {
                PlayerEntry playerEntry = player_map.get(receiverUUID);
                playerEntry.setRepCount(playerEntry.getRepCount() + 1);
                player_map.put(receiverUUID, playerEntry);
            }
            // increase rep_count in sql
            try (PreparedStatement updateStatement = db.getConnection()
                    .prepareStatement("UPDATE logger64_players SET rep_count = rep_count + 1 WHERE UUID = ?")) {
                updateStatement.setString(1, receiverUUID);
                updateStatement.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Exception: Could not save increased rep count to SQL!");
                e.printStackTrace();
            }

            // create new rep entry in sql
            try (PreparedStatement statement = db.getConnection()
                    .prepareStatement("INSERT INTO logger64_reps (initiator_UUID, receiver_UUID, rep) VALUES (?, ?, ?)", 
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, initiatorUUID);
                statement.setString(2, receiverUUID);
                statement.setInt(3, repEntry.getRep());
                statement.executeUpdate();

                // retrieve generated id
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        repEntry.setId(generatedKeys.getInt(1));
                        rep_map.get(initiatorUUID).put(receiverUUID, repEntry);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Exception: Could not save rep entry to SQL!");
                e.printStackTrace();
                return;
            }
        } else {
            // update existing rep entry in cache(map)
            rep_map.get(initiatorUUID).put(receiverUUID, repEntry);

            // update existing rep entry in sql
            try (PreparedStatement statement = db.getConnection()
                    .prepareStatement("UPDATE logger64_reps SET rep = ? WHERE initiator_UUID = ? AND receiver_UUID = ?")) {
                statement.setInt(1, repEntry.getRep());
                statement.setString(2, initiatorUUID);
                statement.setString(3, receiverUUID);
                statement.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Exception: Could not update rep entry to SQL!");
                e.printStackTrace();
                return;
            }
        }
        
        // recalculate and save
        savePlayerEntry(getPlayerEntry(receiverUUID));
    }

    public void deleteRepEntry(String initiatorUUID, String receiverUUID) {
        if (rep_map.containsKey(initiatorUUID) && rep_map.get(initiatorUUID).containsKey(receiverUUID)) {
            // remove rep from rep map
            rep_map.get(initiatorUUID).remove(receiverUUID);

            // if initiator key is empty, remove it
            if (rep_map.get(initiatorUUID).isEmpty()) {
                rep_map.remove(initiatorUUID);
            }
        }

        // delete from sql
        try (PreparedStatement statement = db.getConnection()
            .prepareStatement("DELETE FROM logger64_reps WHERE initiator_UUID = ? AND receiver_UUID = ?")) {
            statement.setString(1, initiatorUUID);
            statement.setString(2, receiverUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Exception: Could not delete rep entry!");
            e.printStackTrace();
        }
        
        // recalculate and save
        unloadPlayerCache(getPlayerUsername(receiverUUID));
        loadPlayerCache(getPlayerUsername(receiverUUID));
        savePlayerEntry(getPlayerEntry(receiverUUID));
    }

    public void deleteRepEntriesByInitiator(String initiatorUUID) {
        if (initiatorUUID==null) {
            System.out.println("Error: Could not find initiator UUID when deleting!");
            return;
        }

        Set<String> receiverList = getRepReceivers(initiatorUUID);
        if (receiverList == null || receiverList.isEmpty()) {
            System.out.println("Error: Could not find receiver list when deleting!");
            return;
        }
                
        // remove any entry in cache(map) created by initiaor uuid
        if (rep_map.containsKey(initiatorUUID)) {
            rep_map.remove(initiatorUUID);
        }

        // delete from sql
        try (PreparedStatement statement = db.getConnection()
                .prepareStatement("DELETE FROM logger64_reps WHERE initiator_UUID = ?")) {
            statement.setString(1, initiatorUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Exception: Could not delete rep entries by initiator!");
            e.printStackTrace();
            return;
        }

        // recalculate and save
        for (String n : receiverList) {
            if (n != null) {
                PlayerEntry targetPlayerEntry = getPlayerEntry(getPlayerUUID(n));
                if (targetPlayerEntry != null) {
                    unloadPlayerCache(targetPlayerEntry.getPlayerUsername());
                    loadPlayerCache(targetPlayerEntry.getPlayerUsername());
                    savePlayerEntry(targetPlayerEntry);
                } else {
                    System.out.println("Error: Could not find player entry for UUID when saving!");
                }
            } else {
                System.out.println("Error: Could not find username when when saving!");
            }
        }
    }

    public void deleteRepEntriesByReceiver(String receiverUUID) {
        if (receiverUUID==null) {
            System.out.println("Error: Could not find receiver UUID when deleting!");
            return;
        }

        // iterate thru outer map to find and remove entries with receiverUUID
        rep_map.values().forEach(innerMap -> innerMap.remove(receiverUUID));

        // remove any outer keys that have empty inner maps
        rep_map.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // delete from sql
        try (PreparedStatement statement = db.getConnection()
                .prepareStatement("DELETE FROM logger64_reps WHERE receiver_UUID = ?")) {
            statement.setString(1, receiverUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Exception: Could not delete rep entries by receiver!");
            e.printStackTrace();
            return;
        }

        // recalculate and save
        PlayerEntry targetPlayerEntry = getPlayerEntry(receiverUUID);
        if (targetPlayerEntry != null) {
            unloadPlayerCache(getPlayerUsername(receiverUUID));
            loadPlayerCache(getPlayerUsername(receiverUUID));
            savePlayerEntry(targetPlayerEntry);
        } else {
            System.out.println("Error: Could not find player entry for UUID when saving!");
        }
    }

    public void resetPlayerEntry(String targetUUID) {
        if (targetUUID==null) {
            System.out.println("Error: Could not find target UUID when resetting!");
            return;
        }

        Set<String> receiverList = getRepReceivers(targetUUID);
        if (receiverList != null && !receiverList.isEmpty()) {
            for (String n : receiverList) {
                deleteRepEntry(targetUUID, getPlayerUUID(n));
            }
        }
        Set<String> initiatorList = getRepInitiators(targetUUID);
        if (initiatorList != null && !initiatorList.isEmpty()) {
            for (String n : initiatorList) {
                deleteRepEntry(getPlayerUUID(n), targetUUID);
            }
        }

        // recalculate and save
        PlayerEntry targetPlayerEntry = getPlayerEntry(targetUUID);
        if (targetPlayerEntry != null) {
            unloadPlayerCache(targetPlayerEntry.getPlayerUsername());
            loadPlayerCache(targetPlayerEntry.getPlayerUsername());
            savePlayerEntry(targetPlayerEntry, 0);
        } else {
            System.out.println("Error: Could not find player entry for UUID when saving!");
        }
    }
}