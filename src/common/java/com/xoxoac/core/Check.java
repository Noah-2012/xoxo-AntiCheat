package com.xoxoac.core;

import org.bukkit.entity.Player;

public abstract class Check {

    private final String name;
    private final CheckCategory category;

    protected Check(String name, CheckCategory category) {
        this.name = name;
        this.category = category;
    }

    public String name() {
        return name;
    }

    public CheckCategory category() {
        return category;
    }

    public CheckResult handleMove(Player player, PlayerData data, MoveSnapshot move) {
        return pass();
    }

    public CheckResult handleArmSwing(Player player, PlayerData data) {
        return pass();
    }

    public CheckResult handleAttack(Player player, PlayerData data, Player target) {
        return pass();
    }

    protected CheckResult pass() {
        return CheckResult.pass();
    }

    protected CheckResult fail(String details) {
        return CheckResult.fail(name, details);
    }
}
