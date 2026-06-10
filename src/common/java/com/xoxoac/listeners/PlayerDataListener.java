package com.xoxoac.listeners;

import com.xoxoac.core.CheckManager;
import com.xoxoac.core.MoveSnapshot;
import com.xoxoac.core.PlayerData;
import com.xoxoac.core.PlayerDataManager;
import com.xoxoac.modules.ViolationManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerDataListener implements Listener {

    private final PlayerDataManager dataManager;
    private final CheckManager checkManager;
    private final ViolationManager violationManager;

    public PlayerDataListener(
            PlayerDataManager dataManager,
            CheckManager checkManager,
            ViolationManager violationManager
    ) {
        this.dataManager = dataManager;
        this.checkManager = checkManager;
        this.violationManager = violationManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (shouldIgnore(player)) return;

        Location to = event.getTo();
        if (to == null || event.getFrom().getWorld() != to.getWorld()) return;

        PlayerData data = dataManager.get(player);
        MoveSnapshot move = data.recordMove(event.getFrom(), to, System.currentTimeMillis());
        checkManager.handleMove(player, data, move);
    }

    @EventHandler
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        if (shouldIgnore(player)) return;

        PlayerData data = dataManager.get(player);
        data.recordArmSwing(System.currentTimeMillis());
        checkManager.handleArmSwing(player, data);
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (shouldIgnore(player)) return;

        PlayerData data = dataManager.get(player);
        checkManager.handleAttack(player, data, target);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataManager.remove(event.getPlayer().getUniqueId());
    }

    private boolean shouldIgnore(Player player) {
        return violationManager.isExcepted(player.getUniqueId())
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR;
    }
}
