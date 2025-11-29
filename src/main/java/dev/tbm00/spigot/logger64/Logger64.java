package dev.tbm00.spigot.logger64;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import dev.tbm00.spigot.logger64.command.LoggerCommand;
import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.listener.PlayerJoin;
import dev.tbm00.spigot.logger64.listener.PlayerLogin;

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

        if (!checkOnHooks()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Load Config
        saveDefaultConfig();

        // Initialize Utils
        StaticUtils.init(this);

        // Connect to MySQL
        try {
            mysqlConnection = new MySQLConnection(this);
        } catch (Exception e) {
            getLogger().severe("Failed to connect to MySQL. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Connect LogManager
        logManager = new LogManager(this, mysqlConnection);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this, logManager), this);
        if (getConfig().getString("logger.logJoinEventMethod", "timer").toLowerCase().equals("authme")) {
            getServer().getPluginManager().registerEvents(new PlayerLogin(this, logManager), this);
        }

        // Register Commands
        getCommand("logger").setExecutor(new LoggerCommand(this, logManager));
    }

    private boolean checkOnHooks() {
        if (getConfig().getBoolean("hook.AuthMe", false) && !checkOnAuthMe()) {
            getLogger().severe("AuthMe hook failed -- disabling plugin!");
            return false;
        }

        if (getConfig().getBoolean("hook.FastLogin", false) && !checkOnFastLogin()) {
            getLogger().severe("FastLogin hook failed -- disabling plugin!");
            return false;
        }

        if (getConfig().getBoolean("hook.Floodgate", false) && !checkOnFloodgate()) {
            getLogger().severe("Floodgate hook failed -- disabling plugin!");
            return false;
        }
        return true;
    }

    private boolean checkOnAuthMe() {
        if (!isPluginAvailable("AuthMe")) {
            log(ChatColor.RED + "AuthMe not avaliable");
            return false;
        }

        Plugin authmep = Bukkit.getPluginManager().getPlugin("AuthMe");
        if (!authmep.isEnabled()) {
            log(ChatColor.RED + "AuthMe not enabled");
            return false;
        }
        
        log("AuthMe hooked.");
        return true;
    }

    private boolean checkOnFastLogin() {
        if (!isPluginAvailable("FastLogin")) {
            log(ChatColor.RED + "FastLogin not avaliable");
            return false;
        }
        
        Plugin fastloginp = Bukkit.getPluginManager().getPlugin("FastLogin");
        if (!fastloginp.isEnabled()) {
            log(ChatColor.RED + "FastLogin not enabled");
            return false;
        }

        log("FastLogin hooked.");
        return true;
    }

    private boolean checkOnFloodgate() {
        if (!isPluginAvailable("Floodgate")) {
            log(ChatColor.RED + "Floodgate not avaliable");
            return false;
        }
        
        Plugin floodgatep = Bukkit.getPluginManager().getPlugin("Floodgate");
        if (!floodgatep.isEnabled()) {
            log(ChatColor.RED + "Floodgate not enabled");
            return false;
        }

        log("Floodgate hooked.");
        return true;
    }

    private boolean isPluginAvailable(String pluginName) {
		final Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);
		return plugin != null && plugin.isEnabled();
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

    public void log(String... strings) {
		for (String s : strings)
            getServer().getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + s);
	}
}