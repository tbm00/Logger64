package dev.tbm00.spigot.logger64.listener;

import java.util.Date;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.model.PlayerEntry;

public class PlayerJoinLeave implements Listener {
    private final LogManager repManager;

    public PlayerJoinLeave(LogManager repManager) {
        this.repManager = repManager;
    }

    public PlayerEntry loadPlayerEntryFromData(Player player) {
        PlayerEntry playerEntry = repManager.getPlayerEntry(player.getUniqueId().toString());
        if (playerEntry == null) {
            playerEntry = new PlayerEntry(player.getUniqueId().toString(), player.getName(), 0.0, 0.0, 0, 5.0, 0.0, 0, new Date(), new Date());
            repManager.savePlayerEntry(playerEntry);
            System.out.println("Could not retrieve player entry... Creating new one!");
        }
        return playerEntry;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        PlayerEntry playerEntry = loadPlayerEntryFromData(p);
        playerEntry.setLastLogin(new Date());
        repManager.savePlayerEntry(playerEntry);
        repManager.loadPlayerCache(p.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player p = event.getPlayer();
        PlayerEntry playerEntry = loadPlayerEntryFromData(p);
        playerEntry.setLastLogout(new Date());
        repManager.savePlayerEntry(playerEntry);
        //repManager.unloadPlayerCache(p.getName());
    }
}


