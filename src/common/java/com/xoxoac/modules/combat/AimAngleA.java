package com.xoxoac.modules.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class AimAngleA implements CombatCheck {

    private static final double MIN_ATTACK_DOT = 0.19;

    @Override
    public String name() {
        return "AimAngleA";
    }

    @Override
    public String check(Player player, LivingEntity target, CombatContext context) {
        double dot = context.lookDot();
        if (dot < MIN_ATTACK_DOT) {
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
            return String.format("Out-of-view attack (dot=%.2f, %.1f deg)", dot, angle);
        }
        return null;
    }

    @Override
    public boolean cancelHit() {
        return true;
    }
}
