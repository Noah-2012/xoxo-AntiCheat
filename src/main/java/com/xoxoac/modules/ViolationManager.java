package com.xoxoac.modules;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class ViolationManager {

    private final Plugin plugin;

    private final Map<UUID, Map<String, Integer>> violations = new HashMap<>();
    private final Set<UUID> exceptions = new HashSet<>(); // NEW

    public ViolationManager(Plugin plugin) {
        this.plugin = plugin;
        startViolationDecayTask();
    }

    // --- Exception API ---

    public void addException(UUID uuid) {
        exceptions.add(uuid);
    }

    public void removeException(UUID uuid) {
        exceptions.remove(uuid);
    }

    public boolean isExcepted(UUID uuid) {
        return exceptions.contains(uuid);
    }

    public Set<UUID> getExceptions() {
        return Collections.unmodifiableSet(exceptions);
    }

    // --- Flagging ---

    public void flag(Player player, String module, String details) {
        UUID uuid = player.getUniqueId();

        // Skip flagging entirely for excepted players
        if (exceptions.contains(uuid)) return; // NEW

        if (player.isOp()) {
            plugin.getLogger().warning("[xoxo-AC Silent] " + player.getName() + " flagged " + module + ": " + details);
        } else {
            String flagMsg = "§d§l[xoxo-AC] §c" + player.getName() + " §7flagged §e" + module + " §8(" + details + ")";
            for (Player op : Bukkit.getOnlinePlayers()) {
                if (op.isOp()) op.sendMessage(flagMsg);
            }
        }

        violations.putIfAbsent(uuid, new HashMap<>());

        int currentVl = violations.get(uuid).getOrDefault(module, 0);
        currentVl++;
        violations.get(uuid).put(module, currentVl);

        if (currentVl >= 40) {
            executeBan(player, module);
            violations.get(uuid).remove(module);
        }
    }

    private void executeBan(Player player, String module) {
        UUID uuid = player.getUniqueId();

        int pastBans = plugin.getConfig().getInt("bans." + uuid.toString(), 0);
        pastBans++;
        plugin.getConfig().set("bans." + uuid.toString(), pastBans);
        plugin.saveConfig();

        Duration banLength;
        if (pastBans == 1) banLength = Duration.ofDays(30);
        else if (pastBans == 2) banLength = Duration.ofDays(60);
        else banLength = Duration.ofDays(90);

        Component banMessage = Component.text("\n\n")
            .append(Component.text(" xoxo-AntiCheat \n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
            .append(Component.text("You have been suspended from the SMP.\n\n", NamedTextColor.GRAY))
            .append(Component.text("Reason: ", NamedTextColor.WHITE))
            .append(Component.text("Unfair Advantage (" + module + ")\n", NamedTextColor.RED))
            .append(Component.text("Duration: ", NamedTextColor.WHITE))
            .append(Component.text(banLength.toDays() + " Days\n", NamedTextColor.YELLOW))
            .append(Component.text("Offense Number: ", NamedTextColor.WHITE))
            .append(Component.text(pastBans + "\n\n", NamedTextColor.GOLD))
            .append(Component.text("Please play fair.", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC));

        String serializedReason = LegacyComponentSerializer.legacySection().serialize(banMessage);
        player.ban(serializedReason, banLength, "xoxo-AntiCheat", true);
    }

    private void startViolationDecayTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : violations.keySet()) {
                    Map<String, Integer> playerViolations = violations.get(uuid);
                    for (Map.Entry<String, Integer> entry : playerViolations.entrySet()) {
                        if (entry.getValue() > 0) {
                            playerViolations.put(entry.getKey(), entry.getValue() - 1);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}