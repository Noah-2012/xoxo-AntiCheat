package com.xoxoac.core;

import com.xoxoac.modules.ViolationManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CheckManager {

    private final ViolationManager violationManager;
    private final List<Check> checks = new ArrayList<>();

    public CheckManager(ViolationManager violationManager) {
        this.violationManager = violationManager;
    }

    public void register(Check check) {
        checks.add(check);
    }

    public List<Check> checks() {
        return Collections.unmodifiableList(checks);
    }

    public void flag(Player player, CheckResult result) {
        if (result.failed()) {
            violationManager.flag(player, result.checkName(), result.details());
        }
    }

    public void handleMove(Player player, PlayerData data, MoveSnapshot move) {
        for (Check check : checks) {
            flag(player, check.handleMove(player, data, move));
        }
    }

    public void handleArmSwing(Player player, PlayerData data) {
        for (Check check : checks) {
            flag(player, check.handleArmSwing(player, data));
        }
    }

    public void handleAttack(Player player, PlayerData data, Player target) {
        for (Check check : checks) {
            flag(player, check.handleAttack(player, data, target));
        }
    }
}
