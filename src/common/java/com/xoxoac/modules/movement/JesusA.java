package com.xoxoac.modules.movement;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JesusA implements MovementCheck {

    private final Map<UUID, Integer> buffer = new HashMap<>();

    @Override
    public String name() {
        return "JesusA";
    }

    @Override
    public String check(Player player, MovementContext c) {

        Location loc = player.getLocation();

        boolean onLiquid = loc.clone()
                .subtract(0, 0.1, 0)
                .getBlock()
                .isLiquid();

        //boolean serverGround = isActuallyOnGround(loc);

        boolean serverGround = c.serverGround();

        if (!onLiquid || serverGround) {
            buffer.remove(player.getUniqueId());
            return null;
        }

        UUID id = player.getUniqueId();

        int vl = buffer.getOrDefault(id, 0);

        boolean horizontalMotion = c.deltaXZ() > 0.18;
        boolean noVerticalStability = Math.abs(c.deltaY()) < 0.08;

        if (horizontalMotion && noVerticalStability) {
            vl += 2;
        } else {
            vl = Math.max(0, vl - 1);
        }

        buffer.put(id, vl);

        if (vl >= 12) {
            return "Jesus (water walk pattern detected)";
        }

        return null;
    }

    private boolean isActuallyOnGround(Location loc) {
        double[] offsets = {-0.3, 0.0, 0.3};

        for (double x : offsets) {
            for (double z : offsets) {
                Block b = loc.clone().add(x, -0.1, z).getBlock();
                if (b.getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }
}