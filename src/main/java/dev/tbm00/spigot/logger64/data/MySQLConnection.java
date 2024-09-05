package dev.tbm00.spigot.logger64.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.bukkit.plugin.java.JavaPlugin;

public class MySQLConnection {
    private HikariDataSource dataSource;
    private JavaPlugin javaPlugin;

    public MySQLConnection(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        setupConnectionPool();
        initializeDatabase();
    }

    private void setupConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + javaPlugin.getConfig().getString("mysql.host") + 
                        ":" + javaPlugin.getConfig().getInt("mysql.port") + 
                        "/" + javaPlugin.getConfig().getString("mysql.database") +
                        "?useSSL=" + javaPlugin.getConfig().getBoolean("mysql.useSSL", false));
        config.setUsername(javaPlugin.getConfig().getString("mysql.username"));
        config.setPassword(javaPlugin.getConfig().getString("mysql.password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes

        dataSource = new HikariDataSource(config);
        System.out.println("Finished setting up MySQL's Hikari connection pool.");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
    }

    private void initializeDatabase() {
        String playerTable = "CREATE TABLE IF NOT EXISTS logger64_players (" +
                "username VARCHAR(20) PRIMARY KEY, " +
                "known_ips TEXT, " +
                "first_ip VARCHAR(45), " +
                "latest_ip VARCHAR(45), " +
                "first_date DATE, " +
                "latest_date DATE)";

        String ipTable = "CREATE TABLE IF NOT EXISTS logger64_ips (" +
                "ip VARCHAR(45) PRIMARY KEY, " +
                "known_usernames TEXT, " +
                "first_username VARCHAR(20), " +
                "latest_username VARCHAR(20), " +
                "first_date DATE, " +
                "latest_date DATE)";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(playerTable);
            statement.execute(ipTable);
        } catch (SQLException e) {
            javaPlugin.getLogger().severe("Error initializing database: " + e.getMessage());
        }
    }
}

