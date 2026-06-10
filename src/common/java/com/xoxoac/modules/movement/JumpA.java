package com.xoxoac.modules.movement;

import org.bukkit.entity.Player;

public final class JumpA implements MovementCheck {

    @Override
    public String name() {
        return "JumpA";
    }

    @Override
    public String check(Player player, MovementContext context) {
        double maxJump = context.naturalJumpVelocity() + 0.2;
        if (context.deltaY() > maxJump && context.airTicks() <= 2 && context.serverGround()) {
            return "High jump: " + String.format("%.2f", context.deltaY());
        }
        return null;
    }
}
