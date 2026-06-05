package com.xoxoac.modules;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatChecks implements Listener {

    private final ViolationManager manager;
    private final Map<UUID, Long> clickTimes = new HashMap<>();
    private final Map<UUID, Integer> clicks  = new HashMap<>();

    public CombatChecks(ViolationManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (manager.isExcepted(uuid)) return; // FIXED

        long now = System.currentTimeMillis();
        clickTimes.putIfAbsent(uuid, now);
        clicks.put(uuid, clicks.getOrDefault(uuid, 0) + 1);

        if (now - clickTimes.get(uuid) >= 1000) {
            int cps = clicks.get(uuid);

            if (cps > 20) {
                manager.flag(player, "AntiAutoClicker", "CPS: " + cps);
            }

            clicks.put(uuid, 0);
            clickTimes.put(uuid, now);
        }
    }
}