package com.xoxoac.modules.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class InteractReachA {

    public String name() {
        return "InteractReachA";
    }

    public String check(Player player, Block block) {
        double distance = player.getEyeLocation().distance(block.getLocation().add(0.5, 0.5, 0.5));
        if (distance > 5.4) {
            return "Interact reach: " + String.format("%.2f", distance) + " blocks";
        }
        return null;
    }
}
