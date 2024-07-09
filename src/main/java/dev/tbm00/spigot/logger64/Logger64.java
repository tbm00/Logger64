package dev.tbm00.spigot.logger64;

import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.logger64.command.LoggerCommand;
import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.listener.PlayerJoinLeave;

public class Logger64 extends JavaPlugin {

    private MySQLConnection mysqlConnection;
    private LogManager logManager;

    @Override
    public void onEnable() {
        // Startup Message
        final PluginDescriptionFile pdf = this.getDescription();
		log(
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
            pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
		);

        // Load Config
        this.saveDefaultConfig();

        // Connect to MySQL
        this.mysqlConnection = new MySQLConnection(this);

        // Connect RepManager
        this.logManager = new LogManager(this.mysqlConnection);

        // Register Listener
        getServer().getPluginManager().registerEvents(new PlayerJoinLeave(this, this.logManager), this);

        // Register Commands
        getCommand("repadmin").setExecutor(new LoggerCommand(this.logManager));
    }

    @Override
    public void onDisable() {
        this.mysqlConnection.closeConnection();
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