package com.xoxoac.modules.block;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class BlockPlaceContext {

    private final Block block;
    private final Location blockCenter;
    private final double reachDistance;

    private BlockPlaceContext(Player player, Block block) {
        this.block = block;
        this.blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
        this.reachDistance = player.getEyeLocation().distance(blockCenter);
    }

    public static BlockPlaceContext from(Player player, Block block) {
        return new BlockPlaceContext(player, block);
    }

    public Block block() {
        return block;
    }

    public Location blockCenter() {
        return blockCenter.clone();
    }

    public double reachDistance() {
        return reachDistance;
    }
}
