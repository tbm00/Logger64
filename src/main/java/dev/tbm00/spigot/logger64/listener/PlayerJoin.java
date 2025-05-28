package dev.tbm00.spigot.logger64.listener;

import java.util.Date;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitScheduler;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.Logger64;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.IPEntry;

public class PlayerJoin implements Listener {
    private Logger64 javaPlugin;
    private final LogManager logManager;
    private final boolean enabled;
    private final String method;
    private final int tickDelay;
    private final List<String> nonLoggedIPs;

    public PlayerJoin(Logger64 javaPlugin, LogManager logManager) {
        this.javaPlugin = javaPlugin;
        this.logManager = logManager;
        this.method = javaPlugin.getConfig().getString("logger.logJoinEventMethod", "timer").toLowerCase();
        if (this.method.equals("authme")) this.enabled = false;
        else this.enabled = javaPlugin.getConfig().getBoolean("logger.enabled", true);
        this.tickDelay = javaPlugin.getConfig().getInt("logger.timerTicks", 3600);
        this.nonLoggedIPs = javaPlugin.getConfig().getStringList("logger.nonLoggedIPs");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // check if enabled
        if (!enabled) return;

        // check non logged ips
        String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
        if (nonLoggedIPs.contains(ip)) return;

        String username = event.getPlayer().getName();

        switch (method) {
            case "immediate": {
                if (!event.getPlayer().isOnline()) return;
                else {
                    PlayerEntry playerEntry = logManager.getPlayerEntry(username);
                    IPEntry ipEntry = logManager.getIPEntry(ip);
                    Date date = new Date();

                    logManager.savePlayerEntry(playerEntry, ip, username, date);
                    logManager.saveIPEntry(ipEntry, ip, username, date);
                }
                return;
            }
            case "timer": {
                BukkitScheduler scheduler = javaPlugin.getServer().getScheduler();
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
                return;
            }
            case "authme":
                javaPlugin.log("No one should ever get this message!");
                return;
            default:
                javaPlugin.log("logJoinEventMethod '"+method+"' does not exist!");
                return;
        }
    }
}