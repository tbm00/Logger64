package dev.tbm00.spigot.logger64;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.shared.FastLoginCore;

import dev.tbm00.spigot.logger64.command.LoggerCommand;
import dev.tbm00.spigot.logger64.data.MySQLConnection;
import dev.tbm00.spigot.logger64.listener.PlayerJoin;
import dev.tbm00.spigot.logger64.listener.PlayerLogin;

public class Logger64 extends JavaPlugin {

    private MySQLConnection mysqlConnection;
    private LogManager logManager;
    public FastLoginBukkit fastLoginHook;

    @Override
    public void onEnable() {
        // Startup Message
        final PluginDescriptionFile pdf = getDescription();
		log(
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
            pdf.getName() + " v" + pdf.getVersion() + " created by tbm00",
            ChatColor.DARK_PURPLE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"
		);

        if (!setupHooks()) {
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
        getCommand("logger").setExecutor(new LoggerCommand(logManager));
    }

    private boolean setupHooks() {
        if (getConfig().getBoolean("hook.AuthMe", false) && !setupAuthMe()) {
            getLogger().severe("AuthMe hook failed -- disabling plugin!");
            return false;
        }

        if (getConfig().getBoolean("hook.FastLogin", false) && !setupFastLogin()) {
            getLogger().severe("FastLogin hook failed -- disabling plugin!");
            return false;
        }
        return true;
    }

    private boolean setupAuthMe() {
        if (!isPluginAvailable("AuthMe")) {
            log(ChatColor.RED + "AuthMe not avaliable");
            return false;
        }
        
        log("AuthMe hooked.");
        return true;
    }

    private boolean setupFastLogin() {
        if (!isPluginAvailable("FastLogin")) {
            log(ChatColor.RED + "FastLogin not avaliable");
            return false;
        }
        
        Plugin fastloginp = Bukkit.getPluginManager().getPlugin("FastLogin");
        if (!fastloginp.isEnabled()) {
            log(ChatColor.RED + "FastLogin not enabled");
            return false;
        }

        if (fastloginp instanceof FastLoginBukkit) {
            log(ChatColor.GREEN + "fastloginp type is FastLoginBukkit...");
            fastLoginHook = (FastLoginBukkit) fastloginp;
        } else if (fastloginp instanceof FastLoginCore) {
            log(ChatColor.RED + "fastloginp type is FastLoginCore...");
            return false;
        } else {
            log(ChatColor.RED + "fastloginp type not found...");
            return false;
        }

        log("FastLogin hooked.");
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