package com.xoxoac.modules.movement;

import org.bukkit.entity.Player;

public final class StepA implements MovementCheck {

    @Override
    public String name() {
        return "StepA";
    }

    @Override
    public String check(Player player, MovementContext context) {
        if (!player.isInsideVehicle()
                && context.airTicks() <= 1
                && context.deltaY() > 0.65
                && context.serverGround()
                && context.currentVelocityY() < context.naturalJumpVelocity() - 0.05) {
            return "Illegal step: " + String.format("%.2f", context.deltaY()) + " blocks Y";
        }
        return null;
    }
}
