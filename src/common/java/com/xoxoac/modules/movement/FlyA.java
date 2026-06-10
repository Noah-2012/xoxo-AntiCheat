package com.xoxoac.modules.movement;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class FlyA implements MovementCheck {

    @Override
    public String name() {
        return "FlyA";
    }

    @Override
    public String check(Player player, MovementContext context) {
        // 1. Core Riptide Speed Protection
        if (player.isRiptiding()) {
            double maxRiptideY = 21.5;
            if (context.deltaY() > maxRiptideY) {
                return String.format("Riptide speed too high: %.2f bpt (max %.2f)",
                        context.deltaY(), maxRiptideY);
            }
            return null;
        }

        // 2. Water Exit Exemption
        boolean nearWater = player.getLocation().getBlock().getType() == Material.WATER
                || player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.WATER;

        if (nearWater) {
            return null;
        }

        // 3. Physical Flight Checks (Gravity and Air Logic Only)
        if (context.airTicks() > 8) {
            if (context.deltaY() > 0) {
                return "Ascending without support (Gravity Defiance)";
            }
            if (context.deltaY() == 0.0 && !context.clientGround()) {
                return "Hovering mid-air (Zero vertical motion)";
            }
        }

        return null;
    }
}
