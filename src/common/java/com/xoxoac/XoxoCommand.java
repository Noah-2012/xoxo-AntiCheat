package com.xoxoac;

import com.xoxoac.modules.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

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

        switch (args[0].toLowerCase()) {
            case "help"        -> sendHelp(sender);
            case "exceptions"  -> handleExceptions(sender, args);
            case "violations"  -> handleViolations(sender, args);
            case "alerts"      -> handleAlerts(sender);
            default            -> sendHelp(sender);
        }

        return true;
    }

    // ── /xoxo exceptions ─────────────────────────────────────────────────────

    private void handleExceptions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§eUsage: §f/xoxo exceptions <add|get|remove> [player]");
            return;
        }

        switch (args[1].toLowerCase()) {

            case "get" -> {
                Set<UUID> exceptions = violationManager.getExceptions();
                if (exceptions.isEmpty()) {
                    sender.sendMessage("§7No players are currently excepted from xoxo-AntiCheat.");
                    return;
                }
                sender.sendMessage("§d§l[xoxo-AC] §7Excepted players:");
                for (UUID uuid : exceptions) {
                    Player online = Bukkit.getPlayer(uuid);
                    String name = online != null ? online.getName()
                            : Bukkit.getOfflinePlayer(uuid).getName();
                    sender.sendMessage("  §8- §f" + (name != null ? name : uuid));
                }
            }

            case "add" -> {
                if (args.length < 3) { sender.sendMessage("§eUsage: §f/xoxo exceptions add <player>"); return; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage("§cPlayer §f" + args[2] + " §cis not online."); return; }
                if (violationManager.isExcepted(target.getUniqueId())) {
                    sender.sendMessage("§f" + target.getName() + " §cis already excepted.");
                    return;
                }
                violationManager.addException(target.getUniqueId());
                sender.sendMessage("§d§l[xoxo-AC] §f" + target.getName() + " §7→ §aadded §7to exceptions.");
            }

            case "remove" -> {
                if (args.length < 3) { sender.sendMessage("§eUsage: §f/xoxo exceptions remove <player>"); return; }
                String targetName = args[2];
                UUID targetUUID = null;

                Player online = Bukkit.getPlayerExact(targetName);
                if (online != null) {
                    targetUUID = online.getUniqueId();
                } else {
                    for (UUID uuid : violationManager.getExceptions()) {
                        String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
                        if (targetName.equalsIgnoreCase(offlineName)) {
                            targetUUID = uuid;
                            break;
                        }
                    }
                }

                if (targetUUID == null || !violationManager.isExcepted(targetUUID)) {
                    sender.sendMessage("§cNo excepted player found: §f" + targetName);
                    return;
                }
                violationManager.removeException(targetUUID);
                sender.sendMessage("§d§l[xoxo-AC] §f" + targetName + " §7→ §cremoved §7from exceptions.");
            }

            default -> sender.sendMessage("§eUsage: §f/xoxo exceptions <add|get|remove> [player]");
        }
    }

    // ── /xoxo violations <player> ─────────────────────────────────────────────

    private void handleViolations(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§eUsage: §f/xoxo violations <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer §f" + args[1] + " §cis not online.");
            return;
        }

        Map<String, Integer> vlMap = violationManager.getViolationMap(target.getUniqueId());
        if (vlMap.isEmpty()) {
            sender.sendMessage("§d§l[xoxo-AC] §f" + target.getName() + " §7has no active violations.");
            return;
        }

        sender.sendMessage("§d§l[xoxo-AC] §7Violations — §f" + target.getName() + "§7:");
        vlMap.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    int    vl  = e.getValue();
                    String bar = "§a";
                    if (vl >= 25) bar = "§c";
                    else if (vl >= 10) bar = "§e";
                    sender.sendMessage("  §8▸ §e" + e.getKey()
                            + " §8— " + bar + "VL " + vl + " §8/ §740");
                });
    }

    // ── /xoxo alerts ──────────────────────────────────────────────────────────

    private void handleAlerts(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly in-game staff can toggle alerts.");
            return;
        }
        boolean nowMuted = violationManager.toggleAlertMute(player.getUniqueId());
        player.sendMessage(nowMuted
                ? "§d§l[xoxo-AC] §7Flag alerts §cmuted§7. Use §f/xoxo alerts §7to re-enable."
                : "§d§l[xoxo-AC] §7Flag alerts §aenabled§7.");
    }

    // ── Help ──────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§d§l[xoxo-AntiCheat] §7Commands:");
        sender.sendMessage("  §f/xoxo exceptions add §8<player>    §7— Exempt a player from all checks");
        sender.sendMessage("  §f/xoxo exceptions remove §8<player> §7— Remove exemption");
        sender.sendMessage("  §f/xoxo exceptions get               §7— List all excepted players");
        sender.sendMessage("  §f/xoxo violations §8<player>        §7— View active violation levels");
        sender.sendMessage("  §f/xoxo alerts                       §7— Toggle your flag alert feed");
    }

    // ── Tab Completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!sender.isOp()) return suggestions;

        if (args.length == 1) {
            suggestions.addAll(List.of("help", "exceptions", "violations", "alerts"));

        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "exceptions" -> suggestions.addAll(List.of("add", "get", "remove"));
                case "violations" -> Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
            }

        } else if (args.length == 3 && args[0].equalsIgnoreCase("exceptions")) {
            switch (args[1].toLowerCase()) {
                case "add" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!violationManager.isExcepted(p.getUniqueId())) suggestions.add(p.getName());
                    }
                }
                case "remove" -> {
                    for (UUID uuid : violationManager.getExceptions()) {
                        Player p    = Bukkit.getPlayer(uuid);
                        String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                        if (name != null) suggestions.add(name);
                    }
                }
            }
        }

        String typed = args[args.length - 1].toLowerCase();
        suggestions.removeIf(s -> !s.toLowerCase().startsWith(typed));
        return suggestions;
    }
}
