package dev.tbm00.spigot.logger64.data;

import java.sql.*;

import org.bukkit.plugin.java.JavaPlugin;

public class MySQLConnection {

    private Connection connection;
    private JavaPlugin javaPlugin;

    public MySQLConnection(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        openConnection();
        initializeDatabase();
    }

    public synchronized void openConnection() {
        try {
            String host = javaPlugin.getConfig().getString("database.host");
            String port = javaPlugin.getConfig().getString("database.port");
            String database = javaPlugin.getConfig().getString("database.database");
            String username = javaPlugin.getConfig().getString("database.username");
            String password = javaPlugin.getConfig().getString("database.password");
            String options = javaPlugin.getConfig().getString("database.options");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + options, username, password);
            System.out.println("Connected MySQL database!");
        } catch (SQLException e) {
            System.out.println("Exception: Could not connect to the database...");
            e.printStackTrace();
        }
    }

    public synchronized Connection getConnection() {
        //openConnection();
        return this.connection;
    }

    public synchronized void closeConnection() {
        if (this.connection != null) {
            try {
                this.connection.close();
                System.out.println("Disconnected database!");
            } catch (SQLException e) {
                System.out.println("Exception: Could not close the database connection...");
                e.printStackTrace();
            }
        }
    }

    public void initializeDatabase() {
        try (Statement statement = getConnection().createStatement()) {
            String playerTable = "CREATE TABLE IF NOT EXISTS logger64_players (username VARCHAR(20) PRIMARY KEY, known_ips TEXT, first_ip VARCHAR(45), latest_ip VARCHAR(45), first_date DATE, latest_date DATE)";
            String ipTable = "CREATE TABLE IF NOT EXISTS logger64_ips (ip VARCHAR(45) PRIMARY KEY, known_usernames TEXT, first_username VARCHAR(20), latest_username VARCHAR(20), first_date DATE, latest_date DATE)";
            statement.execute(playerTable);
            statement.execute(ipTable);
            System.out.println("Initialized database!");
        } catch (SQLException e) {
            System.out.println("Exception: Could not initialize the database...");
            e.printStackTrace();
        }
    }
}

