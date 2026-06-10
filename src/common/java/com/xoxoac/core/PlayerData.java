package com.xoxoac.core;

import org.bukkit.Location;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private final Deque<Long> movePacketWindow = new ArrayDeque<>();

    private Location lastKnownLocation;
    private long lastMoveAtMs;
    private long lastArmSwingAtMs;
    private int transactionPingMs;

    PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public MoveSnapshot recordMove(Location from, Location to, long nowMs) {
        MoveSnapshot snapshot = new MoveSnapshot(from, to, nowMs);
        lastKnownLocation = to.clone();
        lastMoveAtMs = nowMs;

        movePacketWindow.addLast(nowMs);
        while (!movePacketWindow.isEmpty() && nowMs - movePacketWindow.peekFirst() > 1000L) {
            movePacketWindow.removeFirst();
        }

        return snapshot;
    }

    public int movesInLastSecond() {
        return movePacketWindow.size();
    }

    public Location lastKnownLocation() {
        return lastKnownLocation == null ? null : lastKnownLocation.clone();
    }

    public long lastMoveAtMs() {
        return lastMoveAtMs;
    }

    public void recordArmSwing(long nowMs) {
        lastArmSwingAtMs = nowMs;
    }

    public long lastArmSwingAtMs() {
        return lastArmSwingAtMs;
    }

    public int transactionPingMs() {
        return transactionPingMs;
    }

    public void transactionPingMs(int transactionPingMs) {
        this.transactionPingMs = Math.max(0, transactionPingMs);
    }
}
