package dev.tbm00.spigot.logger64.listener;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.IPEntry;

public class PlayerJoinLeave implements Listener {
    private JavaPlugin javaPlugin;
    private final LogManager logManager;
    private Map<String, BukkitTask> pendingTasks = new HashMap<>();

    public PlayerJoinLeave(JavaPlugin javaPlugin, LogManager logManager) {
        this.javaPlugin = javaPlugin;
        this.logManager = logManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!javaPlugin.getConfig().getBoolean("logger.enabled", true)) return;

        String ip = event.getPlayer().getAddress().getAddress().getHostAddress();

        List<String> nonLoggedIPs = javaPlugin.getConfig().getStringList("logger.nonLoggedIPs");
        if (nonLoggedIPs.contains(ip)) return;

        String username = event.getPlayer().getName();
        int tickWait = javaPlugin.getConfig().getInt("logger.ticksUntilConnectionLogged");

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                PlayerEntry playerEntry = logManager.getPlayerEntry(username);
                IPEntry ipEntry = logManager.getIPEntry(ip);
                Date date = new Date();

                logManager.savePlayerEntry(playerEntry, ip, username, date);
                logManager.saveIPEntry(ipEntry, ip, username, date);

                pendingTasks.remove(username);
            }
        }.runTaskLaterAsynchronously(javaPlugin, tickWait);

        pendingTasks.put(username, task);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!javaPlugin.getConfig().getBoolean("logger.enabled", true)) return;

        String username = event.getPlayer().getName();

        if (pendingTasks.containsKey(username)) {
            pendingTasks.get(username).cancel();
            pendingTasks.remove(username);
        }
    }
}


