package com.xoxoac.modules.block;

import org.bukkit.entity.Player;

public final class BlockSightA {

    public String name() {
        return "BlockSightA";
    }

    public String check(Player player, BlockBreakContext context) {
        if (context.lookDot() < 0.65 && context.block().getType().isSolid()) {
            return "Broke block outside vision profile";
        }
        return null;
    }
}
