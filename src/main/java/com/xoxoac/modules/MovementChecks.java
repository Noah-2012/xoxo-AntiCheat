package com.xoxoac.modules;

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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MovementChecks implements Listener {

    private final ViolationManager manager;
    private final Map<UUID, Integer> airTicks         = new HashMap<>();
    private final Map<UUID, Integer> violations       = new HashMap<>();
    private final Map<UUID, Integer> elytraGraceTicks = new HashMap<>();
    private final Map<UUID, Integer> knockbackGrace   = new HashMap<>();
    private final Map<UUID, Boolean> levitating       = new HashMap<>();

    private static final int ELYTRA_GRACE    = 10;
    private static final int KNOCKBACK_GRACE = 40;

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

    public MovementChecks(ViolationManager manager) {
        this.manager = manager;
    }

    // NEW core helper: returns the max Y velocity a player can have from purely
    // natural sources (jump + Jump Boost). Anything above this must be external.
    // Vanilla jump: 0.42. Jump Boost adds 0.1 per level.
    private double naturalJumpVelocity(Player player) {
        double vel = 0.42;
        if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            vel += 0.1 * (player.getPotionEffect(PotionEffectType.JUMP_BOOST).getAmplifier() + 1);
        }
        return vel;
    }

    // PlayerVelocityEvent fires for EVERY server-applied velocity change.
    // We use it as a secondary safety net (for cases where the event fires
    // before onPlayerMove), but the primary check is now the live velocity
    // read in onPlayerMove itself — removing all timing dependencies.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (manager.isExcepted(uuid)) return;

        double velY = event.getVelocity().getY();
        double naturalMax = naturalJumpVelocity(player);

        // Only grant grace for velocity clearly above what a natural jump produces.
        // 0.25 buffer above natural jump comfortably separates wind charges (1.0+)
        // from Jump Boost jumps while ignoring normal jumps entirely.
        if (velY > naturalMax + 0.25) {
            knockbackGrace.put(uuid, KNOCKBACK_GRACE);
            airTicks.put(uuid, 0);
        }
        // Horizontal-only knockback (melee)
        else if (velY < 0.1 && event.getVelocity().length() > 0.5) {
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
            default -> {}
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        switch (event.getAction()) {
            case ADDED, CHANGED -> {
                if (event.getNewEffect() != null &&
                    event.getNewEffect().getType().equals(PotionEffectType.LEVITATION)) {
                    levitating.put(uuid, true);
                }
            }
            case REMOVED, CLEARED -> {
                if (event.getOldEffect() != null &&
                    event.getOldEffect().getType().equals(PotionEffectType.LEVITATION)) {
                    levitating.put(uuid, false);
                }
            }
            default -> {}
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

        if (manager.isExcepted(uuid)) return;

        if (player.getGameMode() == GameMode.CREATIVE ||
            player.getGameMode() == GameMode.SPECTATOR ||
            player.isSwimming()) return;

        if (levitating.getOrDefault(uuid, false) ||
            player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            reduceViolation(uuid);
            return;
        }

        if (player.isGliding()) {
            airTicks.put(uuid, 0);
            reduceViolation(uuid);
            return;
        }

        int eGrace = elytraGraceTicks.getOrDefault(uuid, 0);
        if (eGrace > 0) {
            elytraGraceTicks.put(uuid, eGrace - 1);
            airTicks.put(uuid, 0);
            reduceViolation(uuid);
            return;
        }

        // ---------------------------------------------------------------
        // PRIMARY EXTERNAL LAUNCH DETECTION — live velocity read.
        // This has NO timing dependency on PlayerVelocityEvent ordering.
        // If the player's current Y velocity exceeds what a natural jump
        // can produce, they were externally launched this tick or recently.
        // We reset airTicks and skip all checks for this tick.
        // ---------------------------------------------------------------
        double currentVelY = player.getVelocity().getY();
        double naturalMax  = naturalJumpVelocity(player);

        // 0.15 buffer: catches the full wind charge arc as velocity decays,
        // while staying safely above natural jump velocity (max ~0.72 with JB II).
        // E.g. wind charge decaying: 1.5 → 1.39 → ... → 0.58 → all caught.
        // Normal jump with JB II: 0.62 → below naturalMax(0.62) + 0.15 = 0.77? 
        // Wait - JB II natural max = 0.62, threshold = 0.62 + 0.15 = 0.77.
        // Wind charge = 1.0+ → caught. JB II jump = 0.62 → NOT caught. Correct.
        if (currentVelY > naturalMax + 0.15) {
            airTicks.put(uuid, 0);
            reduceViolation(uuid);
            return;
        }

        // SECONDARY: event-based grace (catches horizontal knockback and acts
        // as fallback for any edge cases the live check misses)
        int kbGrace = knockbackGrace.getOrDefault(uuid, 0);
        if (kbGrace > 0) {
            knockbackGrace.put(uuid, kbGrace - 1);
            if (kbGrace == 1) airTicks.put(uuid, 0);
            reduceViolation(uuid);
            return;
        }

        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;

        double deltaY  = to.getY() - from.getY();
        double deltaXZ = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());

        boolean serverGround = isActuallyOnGround(player.getLocation());
        boolean clientGround = player.isOnGround();
        boolean onClimbable  = isOnClimbable(player.getLocation());

        boolean inLiquid = player.getLocation().getBlock().isLiquid();
        boolean onLiquid = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().isLiquid();

        if (inLiquid) {
            reduceViolation(uuid);
            return;
        }

        if (onClimbable) {
            airTicks.put(uuid, 0);
            reduceViolation(uuid);
            return;
        }

        // Anti-Jesus
        if (onLiquid && !serverGround) {
            if (Math.abs(deltaY) < 0.005 && deltaXZ > 0.15) {
                handleViolation(player, event, "Jesus", "Walking on water surface");
                return;
            }
        }

        // AirTicks
        if (serverGround) {
            airTicks.put(uuid, 0);
            reduceViolation(uuid);
        } else {
            airTicks.put(uuid, airTicks.getOrDefault(uuid, 0) + 1);
        }

        int ticks = airTicks.getOrDefault(uuid, 0);

        // AntiFly
        if (ticks > 8) {
            if (deltaY > 0) {
                handleViolation(player, event, "AntiFly", "Ascending over air vacuum");
                return;
            }
            if (deltaY == 0.0 && !clientGround) {
                handleViolation(player, event, "AntiFly", "Hovering mid-air");
                return;
            }
        }

        if (!serverGround && clientGround && deltaY > 0.2) {
            handleViolation(player, event, "AntiFly", "Spoofed Ground (Air Jump)");
            return;
        }

        // AntiJump — only meaningful when player just left the ground
        // AND server confirms ground contact (not mid-air wind charge)
        double maxJump = naturalMax + 0.2; // leniency buffer on top of natural max

        if (deltaY > maxJump && ticks <= 2 && serverGround) {
            handleViolation(player, event, "AntiJump", "High Jump: " + String.format("%.2f", deltaY));
            return;
        }

        // AntiSpeed
        double maxSpeed = 0.66;
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            maxSpeed += (player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1) * 0.12;
        }

        if (deltaXZ > maxSpeed) {
            handleViolation(player, event, "AntiSpeed", "Speed limit exceeded: " + String.format("%.2f", deltaXZ));
        }
    }

    @EventHandler
    public void onBoatMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!(boat.getPassenger() instanceof Player player)) return;
        if (manager.isExcepted(player.getUniqueId())) return;

        double deltaY    = event.getTo().getY() - event.getFrom().getY();
        Block blockBelow = boat.getLocation().subtract(0, 0.5, 0).getBlock();

        if (deltaY > 0.5 && !boat.isInWater() && !blockBelow.getType().isSolid()) {
            manager.flag(player, "AntiBoatFly", "Boat ascending in air");
            boat.setVelocity(new org.bukkit.util.Vector(0, -0.5, 0));
        }
    }

    private void handleViolation(Player player, PlayerMoveEvent event, String check, String info) {
        UUID uuid = player.getUniqueId();
        int vl = violations.getOrDefault(uuid, 0) + 1;
        violations.put(uuid, vl);

        if (vl > 3) {
            manager.flag(player, check, info + " (VL: " + vl + ")");
            event.setTo(event.getFrom());
        }
    }

    private void reduceViolation(UUID uuid) {
        int vl = violations.getOrDefault(uuid, 0);
        if (vl > 0) violations.put(uuid, vl - 1);
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
        Block feet  = loc.getBlock();
        Block head  = loc.clone().add(0, 1, 0).getBlock();
        Block below = loc.clone().subtract(0, 0.1, 0).getBlock();
        return CLIMBABLE.contains(feet.getType())
            || CLIMBABLE.contains(head.getType())
            || CLIMBABLE.contains(below.getType());
    }
}