package dev.tbm00.spigot.logger64.listener;

import java.util.Date;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.IPEntry;

public class PlayerJoin implements Listener {
    private JavaPlugin javaPlugin;
    private final LogManager logManager;
    private final boolean enabled;
    private final int tickDelay;
    private final List<String> nonLoggedIPs;

    public PlayerJoin(JavaPlugin javaPlugin, LogManager logManager) {
        this.javaPlugin = javaPlugin;
        this.logManager = logManager;
        this.enabled = javaPlugin.getConfig().getBoolean("logger.enabled", true);
        this.tickDelay = javaPlugin.getConfig().getInt("logger.ticksUntilConnectionLogged");
        this.nonLoggedIPs = javaPlugin.getConfig().getStringList("logger.nonLoggedIPs");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // check if enabled
        if (!enabled) return;

        // check non logged ips
        String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
        if (nonLoggedIPs.contains(ip)) return;

        // initialize for task
        String username = event.getPlayer().getName();
        BukkitScheduler scheduler = javaPlugin.getServer().getScheduler();

        // run task later
        scheduler.runTaskLaterAsynchronously(javaPlugin, () -> {
            if (!event.getPlayer().isOnline()) return;
            else {
                PlayerEntry playerEntry = logManager.getPlayerEntry(username);
                IPEntry ipEntry = logManager.getIPEntry(ip);
                Date date = new Date();

                logManager.savePlayerEntry(playerEntry, ip, username, date);
                logManager.saveIPEntry(ipEntry, ip, username, date);
            }
        }, tickDelay);
    }
}