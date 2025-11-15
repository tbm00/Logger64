package dev.tbm00.spigot.logger64.listener;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import fr.xephi.authme.api.v3.AuthMeApi;

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!isPlayerAllowedInNetwork(player)) {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&',  "&cAccess Denied\n&eYou are connecting from a restricted network... &aGet support on our Discord: &adiscord.gg/acQjaAEBb9"));
            javaPlugin.log("Kicked "+player.getName()+" on join -- restricted network: "+player.getAddress().getAddress().getHostAddress());
            return;
        }
        if (premiumRequired && !isPlayerAllowedInMojang(player)) {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&',  "&cAccess Denied\n&eWe are currently NOT allowing new registrations from cracked/offline accounts... &aGet support on our Discord: &adiscord.gg/acQjaAEBb9"));
            javaPlugin.log("Kicked "+player.getName()+" on join -- cracked player: "+player.getAddress().getAddress().getHostAddress());
            return;
        }

        if (logJoinEnabled) logPlayerJoin(player);
    }

    public void logPlayerJoin(Player player) {
        switch (method) {
            case "immediate": {
                if (player.isOnline()) logManager.logNewEntry(player);
                return;
            }
            case "timer": {
                javaPlugin.getServer().getScheduler().runTaskLaterAsynchronously(javaPlugin, () -> {
                    if (player.isOnline()) logManager.logNewEntry(player);
                }, tickDelay);
                return;
            }
            default:
                javaPlugin.getLogger().warning("logJoinEventMethod '"+method+"' path does not exist in PlayerJoin.java!");
                return;
        }
    }

    public boolean isPlayerAllowedInNetwork(Player player) {
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
        
        return true;
    }

    public boolean isPlayerAllowedInMojang(Player player) {
        return isPlayerRegisteredInAuthme(player) || isPlayerPremium(player);
    }

    private boolean isPlayerRegisteredInAuthme(Player player) {
        Plugin authMePlugin = javaPlugin.getServer().getPluginManager().getPlugin("AuthMe");
        if (authMePlugin == null || !authMePlugin.isEnabled()) {
            javaPlugin.getLogger().warning("AuthMe plugin not found or not enabled --- treating "+player.getName()+" as unregistered!");
            return false;
        }

        return AuthMeApi.getInstance().isRegistered(player.getName());
    }

    private boolean isPlayerPremium(Player player) {
        if (javaPlugin.fastLoginHook == null) {
            javaPlugin.getLogger().warning("FastLogin hook is null when checking premium status for " + player.getName());
            return false;
        }

        PremiumStatus status = javaPlugin.fastLoginHook.getPremiumPlayers().get(player.getUniqueId());
        return status == PremiumStatus.PREMIUM;
    }
}