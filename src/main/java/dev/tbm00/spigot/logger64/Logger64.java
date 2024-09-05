package dev.tbm00.spigot.logger64;

import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.logger64.command.LoggerCommand;
import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.listener.PlayerJoin;

public class Logger64 extends JavaPlugin {

    private MySQLConnection mysqlConnection;
    private LogManager logManager;

    @Override
    public void onEnable() {
        // Startup Message
        final PluginDescriptionFile pdf = getDescription();
		log(
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
            pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
		);

        // Load Config
        saveDefaultConfig();

        // Connect to MySQL
        try {
            mysqlConnection = new MySQLConnection(this);
        } catch (Exception e) {
            getLogger().severe("Failed to connect to MySQL. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        // Connect LogManager
        logManager = new LogManager(mysqlConnection);

        // Register Listener
        getServer().getPluginManager().registerEvents(new PlayerJoin(this, logManager), this);

        // Register Commands
        getCommand("logger").setExecutor(new LoggerCommand(logManager));
    }

    @Override
    public void onDisable() {
        mysqlConnection.closeConnection();
    }

    public MySQLConnection getDatabase() {
        return mysqlConnection;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    private void log(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + s);
	}
}