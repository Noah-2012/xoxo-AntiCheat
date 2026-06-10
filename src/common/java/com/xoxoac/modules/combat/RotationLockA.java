package com.xoxoac.modules.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RotationLockA implements CombatCheck {

    private final Map<UUID, float[]> lastHitRotation = new HashMap<>();
    private final Map<UUID, Integer> violationBuffer = new HashMap<>();

    @Override
    public String name() {
        return "RotationLockA";
    }

    @Override
    public String check(Player player, LivingEntity target, CombatContext context) {
        UUID uuid = player.getUniqueId();
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        float[] previous = lastHitRotation.put(uuid, new float[]{yaw, pitch});

        if (previous == null) {
            return null;
        }

        float deltaYaw = Math.abs(yaw - previous[0]);
        if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;
        float deltaPitch = Math.abs(pitch - previous[1]);

        if (deltaYaw < 0.01f && deltaPitch < 0.01f) {
            int vl = violationBuffer.merge(uuid, 1, Integer::sum);
            if (vl >= 3) {
                violationBuffer.put(uuid, 0);
                return "Rotation lock across " + vl + " hits (dyaw="
                        + String.format("%.4f", deltaYaw) + " deg)";
            }
        } else {
            violationBuffer.computeIfPresent(uuid, (key, value) -> Math.max(0, value - 1));
        }

        return null;
    }
}
