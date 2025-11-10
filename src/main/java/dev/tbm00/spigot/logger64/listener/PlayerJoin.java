package dev.tbm00.spigot.logger64.listener;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import fr.xephi.authme.api.v3.AuthMeApi;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.Logger64;
import dev.tbm00.spigot.logger64.StaticUtils;

public class PlayerJoin implements Listener {
    private Logger64 javaPlugin;
    private final LogManager logManager;

    private final boolean logJoinEnabled;
    private final String method;
    private final int tickDelay;

    private final Set<UUID> playerWhitelist;
    private final Set<String> cidrBlacklistAll;
    private final Set<String> cidrBlacklistUnseen;
    private final boolean authmeEnabled;

    public PlayerJoin(Logger64 javaPlugin, LogManager logManager) {
        this.javaPlugin = javaPlugin;
        this.logManager = logManager;
        this.method = javaPlugin.getConfig().getString("logger.logJoinEventMethod", "timer").toLowerCase();

        if (this.method.equals("authme")) this.logJoinEnabled = false;
        else this.logJoinEnabled = javaPlugin.getConfig().getBoolean("logger.enabled", true);

        this.tickDelay = javaPlugin.getConfig().getInt("logger.timerTicks", 3600);
        
        this.playerWhitelist = new HashSet<>(javaPlugin.getConfig().getStringList("protection.playerWhitelist").stream().map(s -> {
                                    try {
                                        return UUID.fromString(s);
                                    } catch (IllegalArgumentException e) {
                                        javaPlugin.getLogger().warning("Invalid UUID in protection.playerWhitelist: " + s);
                                        return null;
                                    }}).filter(Objects::nonNull).collect(Collectors.toList()));
        this.cidrBlacklistAll = new HashSet<>(javaPlugin.getConfig().getStringList("protection.cidrBlacklistAll"));
        this.cidrBlacklistUnseen = new HashSet<>(javaPlugin.getConfig().getStringList("protection.cidrBlacklistUnseen"));
        this.authmeEnabled = javaPlugin.getConfig().getBoolean("hook.AuthMe", false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (isPlayerAllowedIn(player)) {
            if (logJoinEnabled) logPlayerJoin(player);
        } else {
            player.kickPlayer("Network restricted!");
            javaPlugin.log("Kicked "+player.getName()+" on join -- network restricted..: "+player.getAddress().getAddress().getHostAddress());
        }
    }

    public void logPlayerJoin(Player player) {
        switch (method) {
            case "immediate": {
                if (player.isOnline()) logManager.logNewEntry(player);
                return;
            }
            case "timer": {
                BukkitScheduler scheduler = javaPlugin.getServer().getScheduler();
                scheduler.runTaskLaterAsynchronously(javaPlugin, () -> {
                    if (player.isOnline()) logManager.logNewEntry(player);
                }, tickDelay);
                return;
            }
            default:
                javaPlugin.log("logJoinEventMethod '"+method+"' path does not exist in PlayerJoin.java!");
                return;
        }
    }

    public boolean isPlayerAllowedIn(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerWhitelist.contains(uuid)) return true;

        InetSocketAddress addr = player.getAddress();
        if (addr == null) {
            javaPlugin.getLogger().warning("Player " + player.getName() + " has a NULL IP address..!");
            return false;
        }

        String ip = addr.getAddress().getHostAddress();
        if (StaticUtils.isIpInCidrBlocks(cidrBlacklistAll, ip)) return false;
        if ((!player.hasPlayedBefore() || (authmeEnabled && !isPlayerRegisteredInAuthme(player))) 
                                                    && StaticUtils.isIpInCidrBlocks(cidrBlacklistUnseen, ip)) { 
            return false;
        }

        javaPlugin.log("player allowed to join!");
        return true;
    }

    private boolean isPlayerRegisteredInAuthme(Player player) {
        Plugin authMePlugin = javaPlugin.getServer().getPluginManager().getPlugin("AuthMe");
        if (authMePlugin == null || !authMePlugin.isEnabled()) {
            javaPlugin.getLogger().warning("AuthMe plugin not found or not enabled --- treating player as unregistered:" + player.getName());
            return false;
        }

        AuthMeApi authMeApi = AuthMeApi.getInstance();
        return authMeApi.isRegistered(player.getName());
    }
}