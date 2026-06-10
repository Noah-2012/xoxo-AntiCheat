package com.xoxoac.modules;

import com.xoxoac.modules.combat.AimAngleA;
import com.xoxoac.modules.combat.AutoClickerA;
import com.xoxoac.modules.combat.CombatCheck;
import com.xoxoac.modules.combat.CombatContext;
import com.xoxoac.modules.combat.MultiAuraA;
import com.xoxoac.modules.combat.NoSwingA;
import com.xoxoac.modules.combat.ReachA;
import com.xoxoac.modules.combat.RotationLockA;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

import java.util.List;

public class CombatChecks implements Listener {

    private final ViolationManager manager;
    private final AutoClickerA autoClicker = new AutoClickerA();
    private final NoSwingA noSwing = new NoSwingA();
    private final List<CombatCheck> hitChecks = List.of(
            new ReachA(),
            new AimAngleA(),
            noSwing,
            new MultiAuraA(),
            new RotationLockA()
    );

    public CombatChecks(ViolationManager manager) {
        this.manager = manager;
    }

    public int checkCount() {
        return 1 + hitChecks.size();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        long now = System.currentTimeMillis();
        noSwing.recordSwing(player.getUniqueId(), now);
        String details = autoClicker.handleSwing(player, now);
        if (details != null) {
            manager.flag(player, autoClicker.name(), details);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (shouldSkip(player)) return;

        CombatContext context = CombatContext.from(player, target, System.currentTimeMillis());

        for (CombatCheck check : hitChecks) {
            String details = check.check(player, target, context);
            if (details != null) {
                manager.flag(player, check.name(), details);
                if (check.cancelHit()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private boolean shouldSkip(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || manager.isExcepted(player.getUniqueId());
    }
}
