package com.xoxoac.modules;

import com.xoxoac.modules.movement.FlyA;
import com.xoxoac.modules.movement.FlyB;
import com.xoxoac.modules.movement.JesusA;
import com.xoxoac.modules.movement.JumpA;
import com.xoxoac.modules.movement.MovementCheck;
import com.xoxoac.modules.movement.MovementContext;
import com.xoxoac.modules.movement.NoSlowA;
import com.xoxoac.modules.movement.SpeedA;
import com.xoxoac.modules.movement.StepA;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MovementChecks implements Listener {

    private static final int ELYTRA_GRACE = 10;
    private static final int KNOCKBACK_GRACE = 40;
    private static final int WATER_EXIT_GRACE = 8;

    private static final Set<Material> CLIMBABLE = Set.of(
            Material.LADDER,
            Material.VINE,
            Material.TWISTING_VINES,
            Material.WEEPING_VINES,
            Material.TWISTING_VINES_PLANT,
            Material.WEEPING_VINES_PLANT,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT,
            Material.SCAFFOLDING
    );

    private final ViolationManager manager;
    private final List<MovementCheck> checks = List.of(
            new JesusA(),
            new StepA(),
            new FlyA(),
            new FlyB(),
            new JumpA(),
            new SpeedA(),
            new NoSlowA()
    );

    private final Map<UUID, Integer> airTicks = new HashMap<>();
    private final Map<UUID, Integer> elytraGraceTicks = new HashMap<>();
    private final Map<UUID, Integer> knockbackGrace = new HashMap<>();
    private final Map<UUID, Integer> waterExitGraceTicks = new HashMap<>();
    private final Map<UUID, Boolean> wasInLiquid = new HashMap<>();
    private final Map<UUID, Boolean> levitating = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> buffers = new HashMap<>();

    public MovementChecks(ViolationManager manager) {
        this.manager = manager;
    }

    public int checkCount() {
        return checks.size();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (manager.isExcepted(uuid)) return;

        double velY = event.getVelocity().getY();
        double naturalMax = MovementContext.naturalJumpVelocity(player);

        if (velY > naturalMax + 0.25) {
            knockbackGrace.put(uuid, KNOCKBACK_GRACE);
            airTicks.put(uuid, 0);
        } else if (velY < 0.1 && event.getVelocity().length() > 0.5) {
            knockbackGrace.put(uuid, KNOCKBACK_GRACE / 2);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (manager.isExcepted(player.getUniqueId())) return;

        switch (event.getCause()) {
            case PROJECTILE,
                 ENTITY_ATTACK,
                 ENTITY_EXPLOSION,
                 BLOCK_EXPLOSION,
                 FLY_INTO_WALL -> knockbackGrace.put(player.getUniqueId(), KNOCKBACK_GRACE / 2);
            default -> {
            }
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        switch (event.getAction()) {
            case ADDED, CHANGED -> {
                if (event.getNewEffect() != null
                        && event.getNewEffect().getType().equals(PotionEffectType.LEVITATION)) {
                    levitating.put(uuid, true);
                }
            }
            case REMOVED, CLEARED -> {
                if (event.getOldEffect() != null
                        && event.getOldEffect().getType().equals(PotionEffectType.LEVITATION)) {
                    levitating.put(uuid, false);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onGlideToggle(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (manager.isExcepted(player.getUniqueId())) return;

        UUID uuid = player.getUniqueId();
        if (!event.isGliding()) {
            elytraGraceTicks.put(uuid, ELYTRA_GRACE);
        }
        airTicks.put(uuid, 0);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (shouldSkip(player)) return;

        if (levitating.getOrDefault(uuid, false) || player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            reduceAll(uuid);
            return;
        }

        if (player.isGliding()) {
            airTicks.put(uuid, 0);
            reduceAll(uuid);
            return;
        }

        int elytraGrace = elytraGraceTicks.getOrDefault(uuid, 0);
        if (elytraGrace > 0) {
            elytraGraceTicks.put(uuid, elytraGrace - 1);
            airTicks.put(uuid, 0);
            reduceAll(uuid);
            return;
        }

        double currentVelY = player.getVelocity().getY();
        double naturalMax = MovementContext.naturalJumpVelocity(player);
        if (currentVelY > naturalMax + 0.15) {
            airTicks.put(uuid, 0);
            reduceAll(uuid);
            return;
        }

        int kbGrace = knockbackGrace.getOrDefault(uuid, 0);
        if (kbGrace > 0) {
            knockbackGrace.put(uuid, kbGrace - 1);
            if (kbGrace == 1) airTicks.put(uuid, 0);
            reduceAll(uuid);
            return;
        }

        int waterExitGrace = waterExitGraceTicks.getOrDefault(uuid, 0);
        if (waterExitGrace > 0) {
            waterExitGraceTicks.put(uuid, waterExitGrace - 1);
            airTicks.put(uuid, 0);
            reduceAll(uuid);
            return;
        }

        Location to = event.getTo();
        if (to == null || event.getFrom().getWorld() != to.getWorld()) return;

        boolean serverGround = isActuallyOnGround(player.getLocation());
        boolean onClimbable = isOnClimbable(player.getLocation());
        boolean currentlyInLiquid = player.getLocation().getBlock().isLiquid();

        // Track water exit for grace period
        boolean previouslyInLiquid = wasInLiquid.getOrDefault(uuid, false);
        if (previouslyInLiquid && !currentlyInLiquid) {
            waterExitGraceTicks.put(uuid, WATER_EXIT_GRACE);
        }
        wasInLiquid.put(uuid, currentlyInLiquid);

        if (currentlyInLiquid) {
            reduceAll(uuid);
            return;
        }

        if (onClimbable) {
            airTicks.put(uuid, 0);
            reduceAll(uuid);
            return;
        }

        if (serverGround) {
            airTicks.put(uuid, 0);
        } else {
            airTicks.put(uuid, airTicks.getOrDefault(uuid, 0) + 1);
        }

        MovementContext context = MovementContext.from(
                player,
                event.getFrom(),
                to,
                serverGround,
                player.isOnGround(),
                player.getLocation().clone().subtract(0, 0.1, 0).getBlock().isLiquid(),
                airTicks.getOrDefault(uuid, 0),
                currentVelY,
                naturalMax
        );

        boolean anyFailed = false;
        for (MovementCheck check : checks) {
            String details = check.check(player, context);
            if (details != null) {
                anyFailed = true;
                handleViolation(player, event, check.name(), details);
            } else {
                reduce(uuid, check.name());
            }
        }

        if (!anyFailed && serverGround) {
            reduceAll(uuid);
        }
    }

    @EventHandler
    public void onBoatMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!(boat.getPassenger() instanceof Player player)) return;
        if (manager.isExcepted(player.getUniqueId())) return;

        double deltaY = event.getTo().getY() - event.getFrom().getY();
        Block blockBelow = boat.getLocation().subtract(0, 0.5, 0).getBlock();

        if (deltaY > 0.5 && !boat.isInWater() && !blockBelow.getType().isSolid()) {
            manager.flag(player, "BoatFlyA", "Boat ascending in air");
            boat.setVelocity(new org.bukkit.util.Vector(0, -0.5, 0));
        }
    }

    private boolean shouldSkip(Player player) {
        return manager.isExcepted(player.getUniqueId())
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || player.isSwimming();
    }

    private void handleViolation(Player player, PlayerMoveEvent event, String check, String info) {
        UUID uuid = player.getUniqueId();
        int vl = buffers.computeIfAbsent(uuid, key -> new HashMap<>())
                .merge(check, 1, Integer::sum);

        if (vl > 3) {
            manager.flag(player, check, info + " (buffer:" + vl + ")");
            event.setTo(event.getFrom());
        }
    }

    private void reduce(UUID uuid, String check) {
        Map<String, Integer> playerBuffers = buffers.get(uuid);
        if (playerBuffers == null) return;
        playerBuffers.computeIfPresent(check, (key, value) -> value <= 1 ? null : value - 1);
    }

    private void reduceAll(UUID uuid) {
        Map<String, Integer> playerBuffers = buffers.get(uuid);
        if (playerBuffers == null) return;
        playerBuffers.replaceAll((check, value) -> Math.max(0, value - 1));
    }

    private boolean isActuallyOnGround(Location loc) {
        double[] offsets = {-0.3, 0.0, 0.3};
        for (double x : offsets) {
            for (double z : offsets) {
                Block b = loc.clone().add(x, -0.1, z).getBlock();
                if (b.getType().isSolid()) return true;
            }
        }
        return false;
    }

    private boolean isOnClimbable(Location loc) {
        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block below = loc.clone().subtract(0, 0.1, 0).getBlock();
        return CLIMBABLE.contains(feet.getType())
                || CLIMBABLE.contains(head.getType())
                || CLIMBABLE.contains(below.getType());
    }
}
