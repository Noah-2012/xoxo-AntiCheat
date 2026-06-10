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

    private final Map<UUID, Map<String, Integer>> violations  = new HashMap<>();
    private final Set<UUID>                       exceptions  = new HashSet<>();
    // Staff members who have silenced their own alert feed
    private final Set<UUID>                       mutedAlerts = new HashSet<>();

    public ViolationManager(Plugin plugin) {
        this.plugin = plugin;
        startViolationDecayTask();
    }

    // ── Exception API ─────────────────────────────────────────────────────────

    public void addException(UUID uuid)    { exceptions.add(uuid);    }
    public void removeException(UUID uuid) { exceptions.remove(uuid); }
    public boolean isExcepted(UUID uuid)   { return exceptions.contains(uuid); }
    public Set<UUID> getExceptions()       { return Collections.unmodifiableSet(exceptions); }

    // ── Alert Mute API ────────────────────────────────────────────────────────

    /** Toggles alert muting for the given staff UUID. Returns true if now muted. */
    public boolean toggleAlertMute(UUID uuid) {
        if (mutedAlerts.contains(uuid)) {
            mutedAlerts.remove(uuid);
            return false;
        }
        mutedAlerts.add(uuid);
        return true;
    }

    public boolean isAlertMuted(UUID uuid) { return mutedAlerts.contains(uuid); }

    // ── Violation Query API ───────────────────────────────────────────────────

    /** Returns an unmodifiable snapshot of the player's per-module VL map. */
    public Map<String, Integer> getViolationMap(UUID uuid) {
        return Collections.unmodifiableMap(
                violations.getOrDefault(uuid, Collections.emptyMap())
        );
    }

    // ── Flagging ──────────────────────────────────────────────────────────────

    public void flag(Player player, String module, String details) {
        UUID uuid = player.getUniqueId();
        if (exceptions.contains(uuid)) return;

        violations.putIfAbsent(uuid, new HashMap<>());
        int vl = violations.get(uuid).merge(module, 1, Integer::sum);

        String alertLine = "§d§l[xoxo-AC] §c" + player.getName()
                + " §7▸ §e" + module
                + " §8[VL:" + vl + "/40] §7(" + details + ")";

        if (player.isOp()) {
            // Silent console log for operators — avoids alerting peers to an op's actions.
            plugin.getLogger().warning("[xoxo-AC/Silent] "
                    + player.getName() + " » " + module + " VL:" + vl + " | " + details);
        } else {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.isOp() && !mutedAlerts.contains(staff.getUniqueId())) {
                    staff.sendMessage(alertLine);
                }
            }
        }

        if (vl >= 40) {
            executeBan(player, module);
            violations.get(uuid).remove(module);
        }
    }

    // ── Banning ───────────────────────────────────────────────────────────────

    private void executeBan(Player player, String module) {
        UUID uuid    = player.getUniqueId();
        int pastBans = plugin.getConfig().getInt("bans." + uuid, 0) + 1;
        plugin.getConfig().set("bans." + uuid, pastBans);
        plugin.saveConfig();

        Duration banLength = pastBans == 1 ? Duration.ofDays(30)
                : pastBans == 2 ? Duration.ofDays(60)
                :                 Duration.ofDays(90);

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

        player.ban(
                LegacyComponentSerializer.legacySection().serialize(banMessage),
                banLength, "xoxo-AntiCheat", true
        );
    }

    // ── Violation Decay ───────────────────────────────────────────────────────

    private void startViolationDecayTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map<String, Integer> pv : violations.values()) {
                    // Decrement each module VL by 1 per second, floor at 0.
                    pv.replaceAll((mod, vl) -> Math.max(0, vl - 1));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
