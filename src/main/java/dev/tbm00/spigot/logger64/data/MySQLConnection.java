package dev.tbm00.spigot.logger64.data;

import java.sql.*;

import org.bukkit.configuration.file.FileConfiguration;

public class MySQLConnection {

    private Connection connection;
    private final FileConfiguration fileConfig;

    public MySQLConnection(FileConfiguration fileConfig) {
        this.fileConfig = fileConfig;
        openConnection();
        initializeDatabase();
    }

    public void openConnection() {
        try {
            String host = fileConfig.getString("database.host");
            String port = fileConfig.getString("database.port");
            String database = fileConfig.getString("database.database");
            String username = fileConfig.getString("database.username");
            String password = fileConfig.getString("database.password");
            String options = fileConfig.getString("database.options");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + options, username, password);
            System.out.println("Connected to MySQL database!");
        } catch (SQLException e) {
            System.out.println("Exception: Could not connect to the database...");
            e.printStackTrace();
        }

    }

    public Connection getConnection() {
        return this.connection;
    }

    public void closeConnection() {
        if (this.connection != null) {
            try {
                this.connection.close();
                System.out.println("Disconnected from database.");
            } catch (SQLException e) {
                System.out.println("Exception: Could not close the database connection...");
                e.printStackTrace();
            }
        }
    }

    public void initializeDatabase() {
        try (Statement statement = getConnection().createStatement()) {
            String playersTable = "CREATE TABLE IF NOT EXISTS logger64_players (uuid VARCHAR(36) PRIMARY KEY, username VARCHAR(16), rep_avg DOUBLE, rep_avg_last DOUBLE, rep_staff_modifier INT, rep_shown DOUBLE, rep_shown_last DOUBLE, rep_count INT, last_login DATE, last_logout DATE)";
            String repsTable = "CREATE TABLE IF NOT EXISTS logger64_reps (id INT AUTO_INCREMENT PRIMARY KEY, initiator_UUID VARCHAR(36), receiver_UUID VARCHAR(36), rep INT)";
            statement.execute(playersTable);
            statement.execute(repsTable);
        } catch (SQLException e) {
            System.out.println("Exception: Could not initialize the database...");
            e.printStackTrace();
        }
    }
}

