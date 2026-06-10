package com.xoxoac.core;

import org.bukkit.Location;

public final class MoveSnapshot {

    private final Location from;
    private final Location to;
    private final double deltaX;
    private final double deltaY;
    private final double deltaZ;
    private final double deltaXZ;
    private final long timestampMs;

    public MoveSnapshot(Location from, Location to, long timestampMs) {
        this.from = from.clone();
        this.to = to.clone();
        this.timestampMs = timestampMs;
        this.deltaX = to.getX() - from.getX();
        this.deltaY = to.getY() - from.getY();
        this.deltaZ = to.getZ() - from.getZ();
        this.deltaXZ = Math.hypot(deltaX, deltaZ);
    }

    public Location from() {
        return from.clone();
    }

    public Location to() {
        return to.clone();
    }

    public double deltaX() {
        return deltaX;
    }

    public double deltaY() {
        return deltaY;
    }

    public double deltaZ() {
        return deltaZ;
    }

    public double deltaXZ() {
        return deltaXZ;
    }

    public long timestampMs() {
        return timestampMs;
    }
}
