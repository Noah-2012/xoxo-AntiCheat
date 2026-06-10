package com.xoxoac.modules.movement;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public final class SpeedA implements MovementCheck {

    @Override
    public String name() {
        return "SpeedA";
    }

    @Override
    public String check(Player player, MovementContext context) {
        if (player.isRiptiding()) return null;

        double maxSpeed = 0.66;
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            maxSpeed += (player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1) * 0.12;
        }

        if (context.deltaXZ() > maxSpeed) {
            return "Speed exceeded: " + String.format("%.2f", context.deltaXZ())
                    + " bpt (max " + String.format("%.2f", maxSpeed) + ")";
        }
        return null;
    }
}
