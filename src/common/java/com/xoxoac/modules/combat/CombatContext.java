package com.xoxoac.modules.combat;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class CombatContext {

    private final long timestampMs;
    private final Location eye;
    private final Location targetCenter;
    private final double distanceToCenter;
    private final double lookDot;

    private CombatContext(long timestampMs, Location eye, Location targetCenter) {
        this.timestampMs = timestampMs;
        this.eye = eye.clone();
        this.targetCenter = targetCenter.clone();
        this.distanceToCenter = eye.distance(targetCenter);

        Vector look = eye.getDirection().normalize();
        Vector toTarget = targetCenter.toVector().subtract(eye.toVector()).normalize();
        this.lookDot = look.dot(toTarget);
    }

    public static CombatContext from(Player player, LivingEntity target, long nowMs) {
        Location eye = player.getEyeLocation();
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        return new CombatContext(nowMs, eye, center);
    }

    public long timestampMs() {
        return timestampMs;
    }

    public Location eye() {
        return eye.clone();
    }

    public Location targetCenter() {
        return targetCenter.clone();
    }

    public double distanceToCenter() {
        return distanceToCenter;
    }

    public double lookDot() {
        return lookDot;
    }
}
