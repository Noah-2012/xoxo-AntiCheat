package com.xoxoac.modules.block;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class BlockBreakContext {

    private final Block block;
    private final Location blockCenter;
    private final MineSession mineSession;
    private final long nowMs;
    private final double reachDistance;
    private final double lookDot;

    private BlockBreakContext(Player player, Block block, MineSession mineSession, long nowMs) {
        this.block = block;
        this.blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
        this.mineSession = mineSession;
        this.nowMs = nowMs;
        this.reachDistance = player.getEyeLocation().distance(blockCenter);

        Vector lookDirection = player.getLocation().getDirection().normalize();
        Vector blockDirection = blockCenter.toVector().subtract(player.getEyeLocation().toVector()).normalize();
        this.lookDot = lookDirection.dot(blockDirection);
    }

    public static BlockBreakContext from(Player player, Block block, MineSession mineSession, long nowMs) {
        return new BlockBreakContext(player, block, mineSession, nowMs);
    }

    public Block block() {
        return block;
    }

    public Location blockCenter() {
        return blockCenter.clone();
    }

    public MineSession mineSession() {
        return mineSession;
    }

    public long nowMs() {
        return nowMs;
    }

    public double reachDistance() {
        return reachDistance;
    }

    public double lookDot() {
        return lookDot;
    }
}
