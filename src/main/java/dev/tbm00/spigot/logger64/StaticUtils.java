package dev.tbm00.spigot.logger64;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.TextComponent;

public class StaticUtils {
    private static Logger64 javaPlugin;

    public static void init(Logger64 javaPlugin) {
        StaticUtils.javaPlugin = javaPlugin;
    }

    /**
     * Formats String to title case (replaces `_` with ` `)
     */
    public static String formatTitleCase(String string) {
        StringBuilder builder = new StringBuilder();
        for(String word : string.toString().split("_")) {
            if (word.isEmpty()) continue;
            builder.append(word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase() + " ");
        }
     
        return builder.toString().trim();
    }
    /**
     * Retrieves a player by their name.
     * 
     * @param arg the name of the player to retrieve
     * @return the Player object, or null if not found
     */
    public static Player getPlayer(String arg) {
        return javaPlugin.getServer().getPlayer(arg);
    }

    /**
     * Checks if the sender has a specific permission.
     * 
     * @param sender the command sender
     * @param perm the permission string
     * @return true if the sender has the permission, false otherwise
     */
    public static boolean hasPermission(CommandSender sender, String perm) {
        if (sender instanceof Player && ((Player)sender).getGameMode()==GameMode.CREATIVE) return false;
        return sender.hasPermission(perm) || sender instanceof ConsoleCommandSender;
    }

    /**
     * Sends a message to a target CommandSender.
     * 
     * @param target the CommandSender to send the message to
     * @param string the message to send
     */
    public static void sendMessage(CommandSender target, String string) {
        if (string!=null && !string.isBlank())
            target.spigot().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&8[&fLogger&8] &7" + string)));
    }

    /**
     * Executes a command as the console.
     * 
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean runCommand(String command) {
        ConsoleCommandSender console = javaPlugin.getServer().getConsoleSender();
        try {
            return Bukkit.dispatchCommand(console, command);
        } catch (Exception e) {
            javaPlugin.getLogger().severe("Caught exception running command " + command + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a command as a specific player.
     * 
     * @param target the player to execute the command as
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean sudoCommand(Player target, String command) {
        try {
            return Bukkit.dispatchCommand(target, command);
        } catch (Exception e) {
            javaPlugin.getLogger().severe("Caught exception sudoing command: " + target.getName() + " : /" + command + ": " + e.getMessage());
            return false;
        }
    }

   /**
     * Executes a command as a specific human entity.
     * 
     * @param target the player to execute the command as
     * @param command the command to execute
     * @return true if the command was successfully executed, false otherwise
     */
    public static boolean sudoCommand(HumanEntity target, String command) {
        try {
            return Bukkit.dispatchCommand(target, command);
        } catch (Exception e) {
            javaPlugin.getLogger().severe("Caught exception sudoing command: " + target.getName() + " : /" + command + ": " + e.getMessage());
            return false;
        }
    }

    public static InetAddress getCidrInetAddress(String cidrBlock) {
        String[] parts = cidrBlock.split("/");
        if (parts.length != 2) {
            javaPlugin.getLogger().severe("Invalid CIDR block format: " + cidrBlock);
            return null;
        }
        
        try {
            return InetAddress.getByName(parts[0].trim());
        } catch (Exception e) {
            javaPlugin.getLogger().severe("Invalid IP address in CIDR block: " + cidrBlock);
            return null;
        }
    }

    public static Integer getCidrPrefixLength(String cidrBlock) {
        String[] parts = cidrBlock.split("/");
        if (parts.length != 2) {
            javaPlugin.getLogger().severe("Invalid CIDR block format: " + cidrBlock);
            return null;
        }
        
        try {
            return Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            javaPlugin.getLogger().warning("Invalid CIDR prefix in block: " + cidrBlock);
            return null;
        }
    }

    public static String convertBigIntegerToIp(BigInteger ip) {
        byte[] bytes = ip.toByteArray();
        // ensure 4 bytes for IPv4 (pad with leading zeroes if necessary)
        if (bytes.length > 4) {
            bytes = java.util.Arrays.copyOfRange(bytes, bytes.length - 4, bytes.length);
        } else if (bytes.length < 4) {
            byte[] paddedBytes = new byte[4];
            System.arraycopy(bytes, 0, paddedBytes, 4 - bytes.length, bytes.length);
            bytes = paddedBytes;
        }
    
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static boolean isInSubnet(byte[] ipBytes, byte[] networkBytes, int prefixLength) {
        if (ipBytes == null || networkBytes == null) return false;
        if (ipBytes.length != networkBytes.length) return false;
        if (prefixLength < 0 || prefixLength > ipBytes.length * 8) return false;

        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;

        // Compare all full bytes
        for (int i = 0; i < fullBytes; i++) {
            if (ipBytes[i] != networkBytes[i]) {
                return false;
            }
        }

        if (remainingBits == 0) {
            return true;
        }

        // Compare remaining bits in the next byte
        int mask = 0xFF << (8 - remainingBits);
        return (ipBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
    }

    // cidrBlocks look like: `144.54.0.0/16`, `144.54.0.0/15`, `144.54.10.0/24`, etc.
    public static boolean isIpInCidrBlocks(Set<String> cidrBlocks, String ip) {
        if (cidrBlocks == null || cidrBlocks.isEmpty() || ip == null) return false;

        InetAddress ipAddress;
        try {
            ipAddress = InetAddress.getByName(ip);
        } catch (Exception e) {
            javaPlugin.getLogger().warning("Failed to parse IP address: " + ip);
            return false;
        } byte[] ipBytes = ipAddress.getAddress();

        for (String cidrBlock : cidrBlocks) {
            try {
                InetAddress networkAddress = getCidrInetAddress(cidrBlock);
                if (networkAddress == null) continue; 

                Integer prefixLength = StaticUtils.getCidrPrefixLength(cidrBlock);
                if (prefixLength == null) continue;
                
                if (StaticUtils.isInSubnet(ipBytes, networkAddress.getAddress(), prefixLength.intValue())) return true;
            } catch (Exception e) {
                javaPlugin.getLogger().warning("Caught exception checking  CIDR IP in entry: " + cidrBlock);
            }
        }

        return false;
    }
}