package com.xoxoac.modules.block;

import org.bukkit.entity.Player;

public final class BlockReachA {

    public String name() {
        return "BlockReachA";
    }

    public String check(Player player, BlockBreakContext context) {
        if (context.reachDistance() > 4.5) {
            return "Break reach: " + String.format("%.2f", context.reachDistance()) + " blocks";
        }
        return null;
    }

    public String check(Player player, BlockPlaceContext context) {
        if (context.reachDistance() > 4.5) {
            return "Place reach: " + String.format("%.2f", context.reachDistance()) + " blocks";
        }
        return null;
    }
}
