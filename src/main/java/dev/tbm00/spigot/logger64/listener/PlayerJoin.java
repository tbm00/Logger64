package dev.tbm00.spigot.logger64.listener;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.geysermc.floodgate.api.FloodgateApi;

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

    private boolean authmeEnabled;
    private boolean fastloginEnabled;
    private boolean floodgateEnabled;

    private boolean premiumRequired;
    private boolean bedrockBypass;

    private final Set<UUID> playerWhitelist;
    private final Set<String> cidrBlacklistAll;
    private final Set<String> cidrBlacklistUnseen;

    private static final Map<UUID, Long> sessionJoinTimes = new ConcurrentHashMap<>();

    public PlayerJoin(Logger64 javaPlugin, LogManager logManager) {
        this.javaPlugin = javaPlugin;
        this.logManager = logManager;

        this.method = javaPlugin.getConfig().getString("logger.logJoinEventMethod", "timer").toLowerCase();
        if (this.method.equals("authme")) this.logJoinEnabled = false;
        else this.logJoinEnabled = javaPlugin.getConfig().getBoolean("logger.enabled", true);
        this.tickDelay = javaPlugin.getConfig().getInt("logger.timerTicks", 3600);


        authmeEnabled = javaPlugin.getConfig().getBoolean("hook.AuthMe", false) && isPluginAvailable("AuthMe");
        fastloginEnabled = javaPlugin.getConfig().getBoolean("hook.FastLogin", false) && isPluginAvailable("FastLogin");
        floodgateEnabled = javaPlugin.getConfig().getBoolean("hook.Floodgate", false) && isPluginAvailable("Floodgate");
        
        premiumRequired = javaPlugin.getConfig().getBoolean("protection.requirePremiumToRegister", false);
        if (premiumRequired) {
            if (!authmeEnabled) {
                javaPlugin.getLogger().warning("AuthMe hook not enabled... disabling premium requirement!");
                premiumRequired = false;
            }
            if (!fastloginEnabled) {
                javaPlugin.getLogger().warning("FastLogin hook not enabled... disabling premium requirement!");
                premiumRequired = false;
            }
        }
        
        bedrockBypass = premiumRequired && javaPlugin.getConfig().getBoolean("protection.allowBedrockToo", false);
        if (bedrockBypass) {
            if (!floodgateEnabled) {
                javaPlugin.getLogger().warning("Floodgate hook not enabled... disabling bedrock bypass!");
                bedrockBypass = false;
            }
        }
        
        this.playerWhitelist = new HashSet<>(javaPlugin.getConfig().getStringList("protection.playerWhitelist").stream().map(s -> {
                                    try {
                                        return UUID.fromString(s);
                                    } catch (IllegalArgumentException e) {
                                        javaPlugin.getLogger().warning("Invalid UUID in protection.playerWhitelist: " + s);
                                        return null;
                                    }}).filter(Objects::nonNull).collect(Collectors.toList()));
        this.cidrBlacklistAll = new HashSet<>(javaPlugin.getConfig().getStringList("protection.cidrBlacklistAll"));
        this.cidrBlacklistUnseen = new HashSet<>(javaPlugin.getConfig().getStringList("protection.cidrBlacklistUnseen"));
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        sessionJoinTimes.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String username = event.getPlayer().getName();
        UUID uuid = event.getPlayer().getUniqueId();
        String ipHostAddress = event.getPlayer().getAddress().getAddress().getHostAddress();

        sessionJoinTimes.put(uuid, System.currentTimeMillis());

        if (!isPlayerAllowedInNetwork(username, uuid, ipHostAddress)) {
            Player player = event.getPlayer();

            if (player.isOnline()) {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&',  "&cAccess Denied\n&eYou are connecting from a restricted network... &aGet support on our Discord: &adiscord.gg/acQjaAEBb9"));
                javaPlugin.log("Kicked "+username+" on join -- restricted network: "+ipHostAddress);
                return;
            } else {
                javaPlugin.log("Player "+username+" was offline on join -- not logging: "+ipHostAddress);
                return;
            }
        }

        javaPlugin.getServer().getScheduler().runTaskLater(javaPlugin, () -> {
            if (premiumRequired) {
                boolean isRegistered = isPlayerRegisteredInAuthme(username);
                boolean isBedrock = bedrockBypass && isPlayerOnBedrock(uuid);

                if (!isRegistered && !isBedrock) {
                    if (!isPlayerPremium(username, uuid)) {
                        Player player = javaPlugin.getServer().getPlayer(uuid);

                        if (player!=null && player.isOnline()) {
                            player.kickPlayer(ChatColor.translateAlternateColorCodes('&',  "&cAccess Denied\n&eWe are currently NOT allowing new registrations from cracked/offline accounts... &aGet support on our Discord: &adiscord.gg/acQjaAEBb9"));
                            javaPlugin.log("Kicked "+username+" on join -- cracked player: "+ipHostAddress);
                            return;
                        } else {
                            javaPlugin.log("Player "+username+" was null or offline shortly after join -- not logging: "+ipHostAddress);
                            return;
                        }
                    }
                }
            }

            if (logJoinEnabled) logPlayerJoin(username, uuid, ipHostAddress);
        }, 40);
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

    public boolean isPlayerOnBedrock(UUID uuid) {
        return floodgateEnabled && FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }

    public boolean isPlayerRegisteredInAuthme(String username) {
        return authmeEnabled && AuthMeApi.getInstance().isRegistered(username);
    }

    public static boolean isPlayerPremium(String username, UUID uuid) {
        return JavaPlugin.getPlugin(FastLoginBukkit.class).getStatus(uuid) == PremiumStatus.PREMIUM;
    }

    private boolean isPluginAvailable(String pluginName) {
        Plugin pl = javaPlugin.getServer().getPluginManager().getPlugin(pluginName);
        if (pl == null) {
            javaPlugin.getLogger().warning(pluginName+" plugin not found when expecting it!");
            return false;
        } else if (!pl.isEnabled()) {
            javaPlugin.getLogger().warning(pluginName+" plugin not enabled when expecting it!");
            return false;
        } else return true;
    }

    public static long getOnlineSeconds(UUID playerUuid) {
        Long joinedAt = sessionJoinTimes.get(playerUuid);
        if (joinedAt == null) return 0L;
        
        long now = System.currentTimeMillis();
        return (now - joinedAt) / 1000L;
    }
}