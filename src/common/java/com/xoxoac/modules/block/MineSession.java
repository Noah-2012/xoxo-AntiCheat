package com.xoxoac.modules.block;

import org.bukkit.Location;

public final class MineSession {

    private final Location blockLocation;
    private final long startTimeMs;

    public MineSession(Location blockLocation, long startTimeMs) {
        this.blockLocation = blockLocation.clone();
        this.startTimeMs = startTimeMs;
    }

    public Location blockLocation() {
        return blockLocation.clone();
    }

    public long startTimeMs() {
        return startTimeMs;
    }
}
