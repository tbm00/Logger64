package dev.tbm00.spigot.logger64;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.logger64.command.LoggerCommand;
import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.listener.PlayerJoinLeave;

public class Logger64 extends JavaPlugin {

    private MySQLConnection mysqlConnection;
    private LogManager repManager;

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
        FileConfiguration fileConfig = this.getConfig();

        // Connect to MySQL
        this.mysqlConnection = new MySQLConnection(fileConfig);

        // Connect RepManager
        this.repManager = new LogManager(this.mysqlConnection, fileConfig);

        // Register Listener
        getServer().getPluginManager().registerEvents(new PlayerJoinLeave(this.repManager), this);

        // Register Commands
        getCommand("repadmin").setExecutor(new LoggerCommand(this.repManager, fileConfig));
    }

    @Override
    public void onDisable() {
        this.mysqlConnection.closeConnection();
    }

    public MySQLConnection getDatabase() {
        return mysqlConnection;
    }

    public LogManager getRepManager() {
        return repManager;
    }

    private void log(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + s);
	}
}