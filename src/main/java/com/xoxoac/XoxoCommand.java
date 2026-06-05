package com.xoxoac;

import com.xoxoac.modules.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class XoxoCommand implements CommandExecutor, TabCompleter {

    private final ViolationManager violationManager;

    public XoxoCommand(ViolationManager violationManager) {
        this.violationManager = violationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // /xoxo exceptions <add|get|remove> [player]
        if (args[0].equalsIgnoreCase("exceptions")) {
            if (args.length < 2) {
                sender.sendMessage("§eUsage: §f/xoxo exceptions <add|get|remove> [player]");
                return true;
            }

            String sub = args[1].toLowerCase();

            switch (sub) {
                case "get" -> {
                    Set<UUID> exceptions = violationManager.getExceptions();
                    if (exceptions.isEmpty()) {
                        sender.sendMessage("§7No players are currently excepted from xoxo-AntiCheat.");
                        return true;
                    }
                    sender.sendMessage("§d§l[xoxo-AC] §7Current exceptions:");
                    for (UUID uuid : exceptions) {
                        Player online = Bukkit.getPlayer(uuid);
                        String name = (online != null) ? online.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                        sender.sendMessage("  §8- §f" + (name != null ? name : uuid.toString()));
                    }
                }

                case "add" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eUsage: §f/xoxo exceptions add <player>");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        sender.sendMessage("§cPlayer §f" + args[2] + " §cis not online.");
                        return true;
                    }
                    if (violationManager.isExcepted(target.getUniqueId())) {
                        sender.sendMessage("§f" + target.getName() + " §cis already excepted.");
                        return true;
                    }
                    violationManager.addException(target.getUniqueId());
                    sender.sendMessage("§d§l[xoxo-AC] §f" + target.getName() + " §7has been §aadded §7to the exception list.");
                }

                case "remove" -> {
                    if (args.length < 3) {
                        sender.sendMessage("§eUsage: §f/xoxo exceptions remove <player>");
                        return true;
                    }
                    // Support offline players for remove since they may have logged off
                    UUID targetUUID = null;
                    String targetName = args[2];

                    Player online = Bukkit.getPlayerExact(targetName);
                    if (online != null) {
                        targetUUID = online.getUniqueId();
                    } else {
                        // Try to match by name from exception list
                        for (UUID uuid : violationManager.getExceptions()) {
                            String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
                            if (targetName.equalsIgnoreCase(offlineName)) {
                                targetUUID = uuid;
                                break;
                            }
                        }
                    }

                    if (targetUUID == null || !violationManager.isExcepted(targetUUID)) {
                        sender.sendMessage("§cNo excepted player found with name §f" + targetName + "§c.");
                        return true;
                    }

                    violationManager.removeException(targetUUID);
                    sender.sendMessage("§d§l[xoxo-AC] §f" + targetName + " §7has been §cremoved §7from the exception list.");
                }

                default -> sender.sendMessage("§eUsage: §f/xoxo exceptions <add|get|remove> [player]");
            }

            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§d§l[xoxo-AntiCheat] §7Commands:");
        sender.sendMessage("  §f/xoxo exceptions add §8<player> §7— Exempt a player from all checks");
        sender.sendMessage("  §f/xoxo exceptions remove §8<player> §7— Remove a player's exemption");
        sender.sendMessage("  §f/xoxo exceptions get §7— List all excepted players");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!sender.isOp()) return suggestions;

        if (args.length == 1) {
            suggestions.add("exceptions");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("exceptions")) {
            suggestions.addAll(List.of("add", "get", "remove"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("exceptions")) {
            String sub = args[1].toLowerCase();
            if (sub.equals("add")) {
                // Suggest online players not already excepted
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!violationManager.isExcepted(p.getUniqueId())) {
                        suggestions.add(p.getName());
                    }
                }
            } else if (sub.equals("remove")) {
                // Suggest only currently excepted players
                for (UUID uuid : violationManager.getExceptions()) {
                    Player p = Bukkit.getPlayer(uuid);
                    String name = (p != null) ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                    if (name != null) suggestions.add(name);
                }
            }
        }

        // Filter by what the user has typed so far
        String typed = args[args.length - 1].toLowerCase();
        suggestions.removeIf(s -> !s.toLowerCase().startsWith(typed));
        return suggestions;
    }
}