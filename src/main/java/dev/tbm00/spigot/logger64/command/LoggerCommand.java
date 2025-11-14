package dev.tbm00.spigot.logger64.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import dev.tbm00.spigot.logger64.LogManager;
import dev.tbm00.spigot.logger64.model.PlayerEntry;
import dev.tbm00.spigot.logger64.model.IPEntry;
import dev.tbm00.spigot.logger64.model.TableGenerator;
import dev.tbm00.spigot.logger64.model.TableGenerator.Alignment;
import dev.tbm00.spigot.logger64.model.TableGenerator.Receiver;

public class LoggerCommand implements TabExecutor {
    private final LogManager logManager;
    private final String[] subCommands = new String[]{"seen"};
    private final String[] subAdminCommands = new String[]{"user", "ip", "cidr", "cidrp"};
    private final String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.WHITE + "Logger" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    public LoggerCommand(LogManager logManager) {
        this.logManager = logManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /logger seen <player>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "seen":
                Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Logger64"), () -> handleSeenCommand(sender, args));
                return true;
            case "ip":
                Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Logger64"), () -> handleIPCommand(sender, args));
                return true;
            case "user":
                Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Logger64"), () -> handleUserCommand(sender, args));
                return true;
            case "cidr":
                Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Logger64"), () -> handleCidrCommand(sender, args));
                return true;
            case "cidrp":
                Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Logger64"), () -> handleCidrPlayerCommand(sender, args));
                return true;
            default:
                sender.sendMessage(prefix + ChatColor.RED + "Unknown subcommand!");
                return false;
        }
    }

    private boolean handleSeenCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("logger64.seen")) {
            sender.sendMessage(prefix + ChatColor.RED + "No permission!");
            return false;
        }

        if (args.length == 2) {
            String targetName = args[1];
            PlayerEntry targetPlayerEntry = logManager.getPlayerEntry(targetName);
            if (targetPlayerEntry != null) {
                sender.sendMessage(prefix + ChatColor.GRAY + targetPlayerEntry.getUsername() + " last logged in on " + targetPlayerEntry.getLatestDate() 
                                + ". They first connected to our new host on " + targetPlayerEntry.getFirstDate() + ".");
                return true;
            } else {
                sender.sendMessage(prefix + ChatColor.RED + "Could not find target player!");
                return false;
            }
        } else {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /logger seen <username>");
            return false;
        }
    }

    private boolean handleUserCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("logger64.admin")) {
            sender.sendMessage(prefix + ChatColor.RED + "No permission!");
            return false;
        }

        if (args.length == 2) {
            String targetName = args[1];
            PlayerEntry targetPlayerEntry = logManager.getPlayerEntry(targetName);
            if (targetPlayerEntry != null) {
                if (printUserInfo(sender, targetName, targetPlayerEntry)) {
                    sender.sendMessage("\n");
                    return true;
                } else {
                    return false;
                }
            } else {
                sender.sendMessage(prefix + ChatColor.RED + "Could not find target player!");
                return false;
            }
        } else {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /logger user <username>");
            return false;
        }
    }

    private boolean handleIPCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("logger64.admin")) {
            sender.sendMessage(prefix + ChatColor.RED + "No permission!");
            return false;
        }

        if (args.length == 2) {
            String targetIP = args[1];
            IPEntry targetIPEntry = logManager.getIPEntry(targetIP);
            if (targetIPEntry != null) {
                if (printIPInfo(sender, targetIP, targetIPEntry)) {
                    sender.sendMessage("\n");
                    return true;
                } else {
                    return false;
                }
            } else {
                sender.sendMessage(prefix + ChatColor.RED + "Could not find target IP!");
                return false;
            }
        } else {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /logger ip <IP>");
            return false;
        }
    }

    private boolean handleCidrCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("logger64.admin")) {
            sender.sendMessage(prefix + ChatColor.RED + "No permission!");
            return false;
        }

        if (args.length == 2) {
            String targetCIDR = args[1]; // comes in the form of "X.X.X.X/X"
            if (targetCIDR != null) {
                if (printCIDRInfo(sender, targetCIDR)) {
                    sender.sendMessage("\n");
                    return true;
                } else {
                    return false;
                }
            } else {
                sender.sendMessage(prefix + ChatColor.RED + "Could not read CIDR block!");
                return false;
            }
        } else {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /logger cidr <network>/<prefix>");
            return false;
        }
    }

    private boolean handleCidrPlayerCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("logger64.admin")) {
            sender.sendMessage(prefix + ChatColor.RED + "No permission!");
            return false;
        }

        if (args.length == 2 || args.length == 3) {

            String targetName = args[1];

            int prefixLength = 16;
            if (args.length == 3) {
                try {
                    prefixLength = Integer.parseInt(args[2]);
                } catch (Exception e) {
                    prefixLength = 16;
                }
            } if (prefixLength < 0 || prefixLength > 32) {
                sender.sendMessage(prefix + ChatColor.RED + "Prefix length must be between 0 and 32.");
                return false;
            }

            PlayerEntry targetPlayerEntry = logManager.getPlayerEntry(targetName);
            if (targetPlayerEntry == null) {
                sender.sendMessage(prefix + ChatColor.RED + "Could not find target player!");
                return false;
            }

            if (!printUserInfo(sender, targetName, targetPlayerEntry)) return false;
                
            List<String> knownIPs = new ArrayList<>(logManager.getKnownIPs(targetName));
            sortIps(knownIPs);

            Set<String> seenCidrs = new HashSet<>();

            // CIDR tables
            for (String ip : knownIPs) {
                if (ip == null || ip.isEmpty()) continue;

                String targetCIDR = applyPrefix(ip, prefixLength);
                if (targetCIDR == null) continue;

                if (seenCidrs.add(targetCIDR)) {
                    printCIDRInfo(sender, targetCIDR);
                }
            }

            sender.sendMessage("\n");
            return true;
        } else {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /logger cidrp <username> [length]");
            return false;
        }
    }

    public boolean printUserInfo(CommandSender sender, String targetName, PlayerEntry targetPlayerEntry) {
        String dp = "yyyy-MM-dd";
        DateFormat df = new SimpleDateFormat(dp);
        String dateFirst = df.format(targetPlayerEntry.getFirstDate());
        String dateLatest = df.format(targetPlayerEntry.getLatestDate());
        
        // User: targetName
        TableGenerator tgt1 = new TableGenerator(Alignment.RIGHT, Alignment.CENTER, Alignment.LEFT);
        tgt1.addRow(" ----", "§4User: §6" + targetName + "§r", "---- ");
        for (String line : tgt1.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        // User table
        TableGenerator tg = new TableGenerator(Alignment.LEFT, Alignment.LEFT,
                                                Alignment.LEFT, Alignment.LEFT);
        tg.addRow("§cFirstIP§r", "§cLatestIP§r", "§cFirstDate§r", "§cLatestDate§r");
        tg.addRow("§7" + targetPlayerEntry.getFirstIP(), "§7" + targetPlayerEntry.getLatestIP(), "§7" + dateFirst, "§7" + dateLatest);
        for (String line : tg.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        // Associated IPs: targetName
        TableGenerator tgt2 = new TableGenerator(Alignment.RIGHT, Alignment.CENTER, Alignment.LEFT);
        tgt2.addRow(" ----", "§4Associated IPs:§r", "---- ");
        for (String line : tgt2.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        // IPs table
        TableGenerator tg2 = new TableGenerator(Alignment.LEFT, Alignment.LEFT, Alignment.LEFT);
        List<String> unsortedKnownIPs = logManager.getKnownIPs(targetName);
        if (unsortedKnownIPs == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Error: logManager.getKnownIPs(targetName) returned null\n");
            return false;
        }

        // Sort IPs
        List<String> knownIPs = new ArrayList<>(unsortedKnownIPs);
        sortIps(knownIPs);

        tg2.addRow("§cIP§r", "§cFirstUser§r", "§cLatestUser§r");
        for(String ip : knownIPs) {
            if (ip != null) {
                IPEntry targetIP = logManager.getIPEntry(ip);
                if (targetIP != null) {
                    tg2.addRow("§7" + ip, "§7" + targetIP.getfirstUsername(), "§7" + targetIP.getlatestUsername());
                } else {
                    tg2.addRow("§7§o" + ip, "§7§oNULL", "§7§oNULL");
                }
            } else {
                tg2.addRow("§7§oNULL", "§7§oNULL", "§7§oNULL");
            }
        }
        for (String line : tg2.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        return true;
    }

    public boolean printIPInfo(CommandSender sender, String targetIP, IPEntry targetIPEntry) {
        String dp = "yyyy-MM-dd";
        DateFormat df = new SimpleDateFormat(dp);
        String dateFirst = df.format(targetIPEntry.getFirstDate());
        String dateLatest = df.format(targetIPEntry.getLatestDate());

        // IP: targetIP
        TableGenerator tgt = new TableGenerator(Alignment.RIGHT, Alignment.CENTER, Alignment.LEFT);
        tgt.addRow(" ----", "§4IP: §6" + targetIP + "§r", "---- ");
        for (String line : tgt.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        // IP table
        TableGenerator tg = new TableGenerator(Alignment.LEFT, Alignment.LEFT,
                                                Alignment.LEFT, Alignment.LEFT);
        tg.addRow("§cFirstUser§r", "§cLatestUser§r", "§cFirstDate§r", "§cLatestDate§r");
        tg.addRow("§7" + targetIPEntry.getfirstUsername(), "§7" + targetIPEntry.getlatestUsername(), "§7" + dateFirst, "§7" + dateLatest);
        for (String line : tg.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        // Associated users: targetIP
        TableGenerator tgt2 = new TableGenerator(Alignment.RIGHT, Alignment.CENTER, Alignment.LEFT);
        tgt2.addRow(" ----", "§4Associated Users:§r", "---- ");
        for (String line : tgt2.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        // Users table
        TableGenerator tg2 = new TableGenerator(Alignment.LEFT, Alignment.LEFT, Alignment.LEFT);
        List<String> unsortedKnownNames = logManager.getKnownUsernames(targetIP);
        if (unsortedKnownNames == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Error: logManager.getKnownUsernames(targetIP) returned null\n");
            return false;
        }

        // Sort Names
        List<String> knownNames = new ArrayList<>(unsortedKnownNames);
        Collections.sort(knownNames);

        tg2.addRow("§cUser§r", "§cFirstIP§r", "§cLatestIP§r");
        for(String name : knownNames) {
            if (name != null) {
                PlayerEntry targetName = logManager.getPlayerEntry(name);
                if (targetIP != null) {
                    tg2.addRow("§7" + name, "§7" + targetName.getFirstIP(), "§7" + targetName.getLatestIP());
                } else {
                    tg2.addRow("§7§o" + name, "§7§oNULL", "§7§oNULL");
                }
            } else {
                tg2.addRow("§7§oNULL", "§7§oNULL", "§7§oNULL");
            }
        }
        for (String line : tg2.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        return true;
    }

    public boolean printCIDRInfo(CommandSender sender, String targetCIDR) {
        // Associated to: targetCIDR
        TableGenerator tgt2 = new TableGenerator(Alignment.RIGHT, Alignment.CENTER, Alignment.LEFT);
        tgt2.addRow(" ----", "§4Associated to §6" + targetCIDR + "§r", "---- ");
        for (String line : tgt2.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        // CIDR table
        TableGenerator tg = new TableGenerator(Alignment.LEFT, Alignment.LEFT, Alignment.LEFT);
        List<String> unsortedKnownIPs = logManager.getKnownIPsByCidr(targetCIDR);
        if (unsortedKnownIPs == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Error: logManager.getKnownIPsByCidr(targetCIDR) returned null\n");
            return false;
        }

        // Sort IPs
        List<String> knownIPs = new ArrayList<>(unsortedKnownIPs);
        sortIps(knownIPs);

        tg.addRow("§cIP§r", "§cLatestUser§r", "§cLatestDate§r");
        for(String ip : knownIPs) {
            if (ip != null) {
                IPEntry targetIP = logManager.getIPEntry(ip);
                if (targetIP != null) {
                    tg.addRow("§7" + ip, "§7" + targetIP.getlatestUsername(), "§7" + targetIP.getLatestDate());
                } else {
                    tg.addRow("§7§o" + ip, "§7§o" + "§7§oNULL", "§7§oNULL");
                }
            } else {
                tg.addRow("§7§oNULL", "§7§oNULL", "§7§oNULL");
            }
        }
        for (String line : tg.generate(Receiver.CLIENT, true, true)) {
            sender.sendMessage(line);
        }

        return true;
    }

    private void sortIps(List<String> ips) {
        ips.sort(Comparator.comparingLong(ip -> {
            String[] octets = ip.split("\\.");
            return (Long.parseLong(octets[0]) << 24)
                 | (Long.parseLong(octets[1]) << 16)
                 | (Long.parseLong(octets[2]) <<  8)
                 |  Long.parseLong(octets[3]);
        }));
    }

    private String applyPrefix(String ip, int prefixLength) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return null;

            long value = (Long.parseLong(parts[0]) << 24)
                    | (Long.parseLong(parts[1]) << 16)
                    | (Long.parseLong(parts[2]) << 8)
                    |  Long.parseLong(parts[3]);

            long mask = (prefixLength == 0) ? 0 : ~((1L << (32 - prefixLength)) - 1) & 0xFFFFFFFFL;

            long network = value & mask;

            long o1 = (network >> 24) & 0xFF;
            long o2 = (network >> 16) & 0xFF;
            long o3 = (network >> 8) & 0xFF;
            long o4 = network & 0xFF;

            return o1 + "." + o2 + "." + o3 + "." + o4 + "/" + prefixLength;
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        
        if (args.length == 1) {
            for (String n : subCommands) {
                if (n != null && n.startsWith(args[0])) {
                    list.add(n);
                }
            }
            if (sender.hasPermission("logger64.admin")) {
                for (String n : subAdminCommands) {
                    if (n != null && n.startsWith(args[0])) {
                        list.add(n);
                    }
                }
            }
        }
        
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("seen")) {
                if (sender.hasPermission("logger64.seen")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p != null && p.getName().startsWith(args[1])) {
                            list.add(p.getName());
                        }
                    }
                }
            }
            if (args[0].equalsIgnoreCase("user") || args[0].equalsIgnoreCase("cidrp")) {
                if (sender.hasPermission("logger64.admin")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p != null && p.getName().startsWith(args[1])) {
                            list.add(p.getName());
                        }
                    }
                }
            }
        }
        
        return list; 
    }
}