package com.xoxoac.modules.combat;

import org.bukkit.entity.Player;

import java.util.*;

public final class AutoClickerA {

    private static final int MAX_CPS = 18;
    private static final long WINDOW_MS = 1000L;

    // erlaubt natürliche Stabilität - erhöht um False Positives beim Maushalten zu vermeiden
    private static final double MIN_STD_DEV = 5.0;

    private final Map<UUID, Deque<Long>> clicks = new HashMap<>();

    public String name() {
        return "AutoClickerA";
    }

    public String handleSwing(Player player, long now) {
        UUID id = player.getUniqueId();

        Deque<Long> list = clicks.computeIfAbsent(id, k -> new ArrayDeque<>());
        list.addLast(now);

        // cleanup
        while (!list.isEmpty() && now - list.peekFirst() > WINDOW_MS) {
            list.pollFirst();
        }

        if (list.size() < 6) return null; // zu wenig samples

        int cps = list.size();

        // CPS check (nur soft flag)
        if (cps > MAX_CPS + 3) {
            return "CPS=" + cps;
        }

        // variance check
        List<Long> intervals = new ArrayList<>();
        Long prev = null;

        for (Long t : list) {
            if (prev != null) {
                intervals.add(t - prev);
            }
            prev = t;
        }

        if (intervals.size() < 5) return null;

        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0);

        double variance = 0;
        for (long v : intervals) {
            variance += Math.pow(v - mean, 2);
        }
        variance /= intervals.size();

        double stdDev = Math.sqrt(variance);

        // Erhöhter CPS Schwellenwert um False Positives beim Maushalten zu vermeiden
        if (cps > 20 && stdDev < MIN_STD_DEV) {
            return "CPS=" + cps + ", STD=" + String.format("%.2f", stdDev);
        }

        return null;
    }

    public void clear(Player player) {
        clicks.remove(player.getUniqueId());
    }
}