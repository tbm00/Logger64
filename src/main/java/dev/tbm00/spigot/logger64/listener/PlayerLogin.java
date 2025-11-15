package dev.tbm00.spigot.logger64.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import fr.xephi.authme.events.LoginEvent;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.Logger64;

public class PlayerLogin implements Listener {
    private final LogManager logManager;
    
    private final boolean logLoginEnabled;
    private final String method;

    public PlayerLogin(Logger64 javaPlugin, LogManager logManager) {
        this.logManager = logManager;
        this.method = javaPlugin.getConfig().getString("logger.logJoinEventMethod", "timer").toLowerCase();

        if (this.method.equals("authme")) this.logLoginEnabled = javaPlugin.getConfig().getBoolean("logger.enabled", true);
        else this.logLoginEnabled = false;
    }

    @EventHandler
    public void onPlayerLogin(LoginEvent event) {
        if (logLoginEnabled) logManager.logNewEntry(event.getPlayer().getName(), 
                                                    event.getPlayer().getAddress().getAddress().getHostAddress());
    }
}