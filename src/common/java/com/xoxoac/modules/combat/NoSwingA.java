package com.xoxoac.modules.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NoSwingA implements CombatCheck {

    private static final long SWING_WINDOW_MS = 200L;

    private final Map<UUID, Long> lastSwing = new HashMap<>();

    @Override
    public String name() {
        return "NoSwingA";
    }

    public void recordSwing(UUID uuid, long nowMs) {
        lastSwing.put(uuid, nowMs);
    }

    @Override
    public String check(Player player, LivingEntity target, CombatContext context) {
        Long swingTs = lastSwing.get(player.getUniqueId());
        if (swingTs == null || context.timestampMs() - swingTs > SWING_WINDOW_MS) {
            return "Hit without prior arm-swing packet ("
                    + (swingTs == null ? "never" : (context.timestampMs() - swingTs) + "ms ago") + ")";
        }
        return null;
    }
}
