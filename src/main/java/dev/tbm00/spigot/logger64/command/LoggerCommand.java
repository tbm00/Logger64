package dev.tbm00.spigot.logger64.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

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
    private final String[] subAdminCommands = new String[]{"user", "ip", "cidr"};
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
                List<String> knownIPs = logManager.getKnownIPs(targetName);
                tg2.addRow("§cIP§r", "§cFirstUser§r", "§cLatestUser§r");
                for(String ip : knownIPs) {
                    IPEntry targetIP = logManager.getIPEntry(ip);
                    tg2.addRow("§7" + ip, "§7" + targetIP.getfirstUsername(), "§7" + targetIP.getlatestUsername());
                }
                for (String line : tg2.generate(Receiver.CLIENT, true, true)) {
                    sender.sendMessage(line);
                }

                sender.sendMessage("\n");
                return true;
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
                List<String> knownNames = logManager.getKnownUsernames(targetIP);
                tg2.addRow("§cUser§r", "§cFirstIP§r", "§cLatestIP§r");
                for(String name : knownNames) {
                    PlayerEntry targetName = logManager.getPlayerEntry(name);
                    tg2.addRow("§7" + name, "§7" +  targetName.getFirstIP(), "§7" + targetName.getLatestIP());
                }
                for (String line : tg2.generate(Receiver.CLIENT, true, true)) {
                    sender.sendMessage(line);
                }

                sender.sendMessage("\n");
                return true;
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
            String targetCIDR = args[1]; // comes in the form of "X.X.X.X/X", i.e. "158.15.0.0/16" or "158.15.7.0/24"
            if (targetCIDR != null) {
                // IPs table
                TableGenerator tg = new TableGenerator(Alignment.LEFT, Alignment.LEFT, Alignment.LEFT);
                tg.addRow(" ----", "§4Associated to §6" + targetCIDR + "§r", "---- ");
                List<String> knownIPs = logManager.getKnownIPsByCidr(targetCIDR);
                if (knownIPs == null) {
                    sender.sendMessage(prefix + ChatColor.RED + "Could not getKnownIPsByCidr!");
                    return false;
                }
                tg.addRow("§cIP§r", "§cFirstUser, LatestUser§r", "§cLatestDate§r");
                for(String ip : knownIPs) {
                    IPEntry targetIP = logManager.getIPEntry(ip);
                    tg.addRow("§7" + ip, "§7" + targetIP.getfirstUsername() + ", " + targetIP.getlatestUsername(), "§7" + targetIP.getLatestDate());
                }
                for (String line : tg.generate(Receiver.CLIENT, true, true)) {
                    sender.sendMessage(line);
                }

                sender.sendMessage("\n");
                return true;
            } else {
                sender.sendMessage(prefix + ChatColor.RED + "Could not read CIDR block!");
                return false;
            }
        } else {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /logger cidr <network>/<prefix>");
            return false;
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
            if (args[0].equalsIgnoreCase("user")) {
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