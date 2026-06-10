package com.xoxoac.modules.movement;

import org.bukkit.entity.Player;

public final class FlyB implements MovementCheck {

    @Override
    public String name() {
        return "FlyB";
    }

    @Override
    public String check(Player player, MovementContext context) {
        // Riptide can momentarily desync packet states, so we bypass it here as well
        if (player.isRiptiding()) {
            return null;
        }

        // Check if the client claims ground status, but the server physics engine disagrees
        if (context.clientGround() && !context.serverGround()) {

            // Condition 1: Client claims they are safely walking/jumping on ground, but they are rising rapidly in mid-air
            if (context.deltaY() > 0.2) {
                return String.format("Spoofed ground air jump (deltaY: %.3f)", context.deltaY());
            }

            // Condition 2: Client claims they are on the ground, but they are falling downwards through the air (No Fall Hack)
            if (context.deltaY() < -0.1 && context.airTicks() > 3) {
                return String.format("Spoofed ground while falling (deltaY: %.3f, airTicks: %d)",
                        context.deltaY(), context.airTicks());
            }
        }

        return null;
    }
}
