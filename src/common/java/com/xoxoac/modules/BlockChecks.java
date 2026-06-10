package com.xoxoac.modules;

import com.xoxoac.modules.block.BlockBreakContext;
import com.xoxoac.modules.block.BlockPlaceContext;
import com.xoxoac.modules.block.BlockReachA;
import com.xoxoac.modules.block.BlockSightA;
import com.xoxoac.modules.block.FastBreakA;
import com.xoxoac.modules.block.InteractReachA;
import com.xoxoac.modules.block.MineSession;
import com.xoxoac.modules.block.ScaffoldA;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockChecks implements Listener {

    private final ViolationManager manager;
    private final Map<UUID, MineSession> activeMineSessions = new HashMap<>();

    private final FastBreakA fastBreak = new FastBreakA();
    private final BlockReachA blockReach = new BlockReachA();
    private final BlockSightA blockSight = new BlockSightA();
    private final InteractReachA interactReach = new InteractReachA();
    private final ScaffoldA scaffold = new ScaffoldA();

    public BlockChecks(ViolationManager manager) {
        this.manager = manager;
    }

    public int checkCount() {
        return 5;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        activeMineSessions.put(
                player.getUniqueId(),
                new MineSession(event.getBlock().getLocation(), System.currentTimeMillis())
        );
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        UUID uuid = player.getUniqueId();
        MineSession session = activeMineSessions.remove(uuid);
        BlockBreakContext context = BlockBreakContext.from(player, event.getBlock(), session, System.currentTimeMillis());

        if (flagAndCancel(player, event, fastBreak.name(), fastBreak.check(player, context))) return;
        if (flagAndCancel(player, event, blockReach.name(), blockReach.check(player, context))) return;
        flagAndCancel(player, event, blockSight.name(), blockSight.check(player, context));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        BlockPlaceContext context = BlockPlaceContext.from(player, event.getBlockPlaced());
        if (flagAndCancel(player, event, blockReach.name(), blockReach.check(player, context))) return;
        flagAndCancel(player, event, scaffold.name(), scaffold.check(player, context));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        String details = interactReach.check(player, event.getClickedBlock());
        if (details != null) {
            manager.flag(player, interactReach.name(), details);
            event.setCancelled(true);
        }
    }

    private boolean shouldSkip(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || manager.isExcepted(player.getUniqueId());
    }

    private boolean flagAndCancel(Player player, BlockBreakEvent event, String check, String details) {
        if (details == null) return false;
        manager.flag(player, check, details);
        event.setCancelled(true);
        return true;
    }

    private boolean flagAndCancel(Player player, BlockPlaceEvent event, String check, String details) {
        if (details == null) return false;
        manager.flag(player, check, details);
        event.setCancelled(true);
        return true;
    }
}
