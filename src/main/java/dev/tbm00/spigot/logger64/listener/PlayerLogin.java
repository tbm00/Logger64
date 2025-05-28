package dev.tbm00.spigot.logger64.listener;

import java.util.Date;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import fr.xephi.authme.events.LoginEvent;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.Logger64;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.IPEntry;

public class PlayerLogin implements Listener {
    private final LogManager logManager;
    private final boolean enabled;
    private final String method;
    private final List<String> nonLoggedIPs;

    public PlayerLogin(Logger64 javaPlugin, LogManager logManager) {
        this.logManager = logManager;
        this.method = javaPlugin.getConfig().getString("logger.logJoinEventMethod", "authme").toLowerCase();
        if (this.method.equals("authme")) this.enabled = javaPlugin.getConfig().getBoolean("logger.enabled", true);
        else this.enabled = false;
        this.nonLoggedIPs = javaPlugin.getConfig().getStringList("logger.nonLoggedIPs");
    }

    @EventHandler
    public void onPlayerLogin(LoginEvent event) {
        // check if enabled
        if (!enabled) return;

        // check non logged ips
        String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
        if (nonLoggedIPs.contains(ip)) return;

        if (!event.getPlayer().isOnline()) return;
        else {
            String username = event.getPlayer().getName();
            PlayerEntry playerEntry = logManager.getPlayerEntry(username);
            IPEntry ipEntry = logManager.getIPEntry(ip);
            Date date = new Date();

            logManager.savePlayerEntry(playerEntry, ip, username, date);
            logManager.saveIPEntry(ipEntry, ip, username, date);
        }
        return;
    }
}