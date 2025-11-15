package dev.tbm00.spigot.logger64.listener;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import fr.xephi.authme.api.v3.AuthMeApi;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PremiumStatus;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.Logger64;
import dev.tbm00.spigot.logger64.StaticUtils;

public class PlayerJoin implements Listener {
    private final Logger64 javaPlugin;
    private final LogManager logManager;

    private final boolean logJoinEnabled;
    private final String method;
    private final int tickDelay;

    private final boolean authmeEnabled;
    private final boolean fastloginEnabled;

    private final boolean premiumRequired;

    private final Set<UUID> playerWhitelist;
    private final Set<String> cidrBlacklistAll;
    private final Set<String> cidrBlacklistUnseen;

    public PlayerJoin(Logger64 javaPlugin, LogManager logManager) {
        this.javaPlugin = javaPlugin;
        this.logManager = logManager;

        this.authmeEnabled = javaPlugin.getConfig().getBoolean("hook.AuthMe", false);
        this.fastloginEnabled = javaPlugin.getConfig().getBoolean("hook.FastLogin", false);
        
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
        this.premiumRequired = fastloginEnabled && authmeEnabled && javaPlugin.getConfig().getBoolean("protection.requirePremiumToRegister", false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String username = event.getPlayer().getName();
        UUID uuid = event.getPlayer().getUniqueId();
        String ipHostAddress = event.getPlayer().getAddress().getAddress().getHostAddress();

        if (!isPlayerAllowedInNetwork(username, uuid, ipHostAddress)) {
            javaPlugin.getServer().getPlayer(uuid).kickPlayer(ChatColor.translateAlternateColorCodes('&',  "&cAccess Denied\n&eYou are connecting from a restricted network... &aGet support on our Discord: &adiscord.gg/acQjaAEBb9"));
            javaPlugin.log("Kicked "+username+" on join -- restricted network: "+ipHostAddress);
            return;
        }

        javaPlugin.getServer().getScheduler().runTaskLater(javaPlugin, () -> {
            if (premiumRequired && !isPlayerAllowedInMojang(username, uuid)) {
                javaPlugin.getServer().getPlayer(uuid).kickPlayer(ChatColor.translateAlternateColorCodes('&',  "&cAccess Denied\n&eWe are currently NOT allowing new registrations from cracked/offline accounts... &aGet support on our Discord: &adiscord.gg/acQjaAEBb9"));
                javaPlugin.log("Kicked "+username+" on join -- cracked player: "+ipHostAddress);
                return;
            }

            if (logJoinEnabled) logPlayerJoin(username, uuid, ipHostAddress);
        }, 25);
    }

    public void logPlayerJoin(String username, UUID uuid, String ipHostAddress) {
        switch (method) {
            case "immediate": {
                if (javaPlugin.getServer().getOfflinePlayer(uuid).isOnline()) logManager.logNewEntry(username, ipHostAddress);
                return;
            }
            case "timer": {
                javaPlugin.getServer().getScheduler().runTaskLaterAsynchronously(javaPlugin, () -> {
                    if (javaPlugin.getServer().getOfflinePlayer(uuid).isOnline()) logManager.logNewEntry(username, ipHostAddress);
                }, tickDelay);
                return;
            }
            default:
                javaPlugin.getLogger().warning("logJoinEventMethod '"+method+"' path does not exist in PlayerJoin.java!");
                return;
        }
    }

    public boolean isPlayerAllowedInNetwork(String username, UUID uuid, String ipHostAddress) {
        if (playerWhitelist.contains(uuid)) return true;

        if (StaticUtils.isIpInCidrBlocks(cidrBlacklistAll, ipHostAddress)) return false;
        if ((!javaPlugin.getServer().getOfflinePlayer(uuid).hasPlayedBefore() || (authmeEnabled && !isPlayerRegisteredInAuthme(username))) 
                                                    && StaticUtils.isIpInCidrBlocks(cidrBlacklistUnseen, ipHostAddress)) { 
            return false;
        }
        
        return true;
    }

    public boolean isPlayerAllowedInMojang(String username, UUID uuid) {
        return isPlayerRegisteredInAuthme(username) || isPlayerPremium(username, uuid);
    }

    private boolean isPlayerRegisteredInAuthme(String username) {
        Plugin authMePlugin = javaPlugin.getServer().getPluginManager().getPlugin("AuthMe");
        if (authMePlugin == null || !authMePlugin.isEnabled()) {
            javaPlugin.getLogger().warning("AuthMe plugin not found or not enabled --- treating "+username+" as unregistered!");
            return false;
        }

        return AuthMeApi.getInstance().isRegistered(username);
    }

    private boolean isPlayerPremium(String username, UUID uuid) {
        PremiumStatus status = JavaPlugin.getPlugin(FastLoginBukkit.class).getStatus(uuid); 

        //if (status.equals(PremiumStatus.UNKNOWN)) javaPlugin.getLogger().warning("Unknown authentication status for "+username+"..!");
        //if (status.equals(PremiumStatus.PREMIUM)) javaPlugin.getLogger().warning("Premium authentication status for "+username+"..!");
        //if (status.equals(PremiumStatus.CRACKED)) javaPlugin.getLogger().warning("Cracked authentication status for "+username+"..!");

        return status == PremiumStatus.PREMIUM;
    }
}