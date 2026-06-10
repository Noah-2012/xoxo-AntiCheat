package com.xoxoac.modules.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class ReachA implements CombatCheck {

    private static final double MAX_REACH = 3.0;
    private static final double REACH_LENIENCY = 0.70;

    @Override
    public String name() {
        return "ReachA";
    }

    @Override
    public String check(Player player, LivingEntity target, CombatContext context) {
        double maxDistance = MAX_REACH + REACH_LENIENCY;
        if (context.distanceToCenter() > maxDistance) {
            return String.format("Combat reach %.2f > %.2f blocks", context.distanceToCenter(), maxDistance);
        }
        return null;
    }

    @Override
    public boolean cancelHit() {
        return true;
    }
}
