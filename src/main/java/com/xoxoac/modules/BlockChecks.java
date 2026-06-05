package com.xoxoac.modules;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class BlockChecks implements Listener {

    private final ViolationManager manager;

    private static class MineContext {
        final Location blockLocation;
        final long startTime;

        MineContext(Location blockLocation, long startTime) {
            this.blockLocation = blockLocation;
            this.startTime = startTime;
        }
    }

    private final Map<UUID, MineContext> activeMineSessions = new HashMap<>();
    
    private static final Set<Material> PLACEABLE_ON = Set.of(
        Material.COBWEB, Material.BAMBOO, Material.BAMBOO_SAPLING,
        Material.SUGAR_CANE, Material.VINE, Material.TWISTING_VINES,
        Material.WEEPING_VINES, Material.CAVE_VINES, Material.CAVE_VINES_PLANT,
        Material.SNOW, Material.LILY_PAD, Material.MOSS_CARPET,
        Material.PINK_PETALS, Material.GLOW_LICHEN, Material.SCULK_VEIN
    );

    // Dynamic Safe Lookup for cross-version compatibility (Handles Haste/Mining Fatigue mappings)
    private final PotionEffectType hasteEffect = getPotionEffectFallback("HASTE", "FAST_DIGGING");
    private final PotionEffectType fatigueEffect = getPotionEffectFallback("MINING_FATIGUE", "SLOW_DIGGING");

    public BlockChecks(ViolationManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (manager.isExcepted(player.getUniqueId())) return;

        activeMineSessions.put(
            player.getUniqueId(),
            new MineContext(event.getBlock().getLocation(), System.currentTimeMillis())
        );
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (manager.isExcepted(player.getUniqueId())) return;
    
        Block block = event.getBlock();
        UUID uuid = player.getUniqueId();
        Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
    
        if (activeMineSessions.containsKey(uuid)) {
            MineContext session = activeMineSessions.get(uuid);
    
            if (session.blockLocation.equals(block.getLocation())) {
                long timeTaken = System.currentTimeMillis() - session.startTime;
                float hardness = block.getType().getHardness();
                ItemStack item = player.getInventory().getItemInMainHand();

                // Calculate the true vanilla expected breaking speeds
                float speedMultiplier = 1.0f;
                int efficiencyLevel = 0;

                if (item != null && item.getType() != Material.AIR) {
                    // Check if tool matches block types natively
                    if (block.getBlockData().isPreferredTool(item)) {
                        speedMultiplier = getToolBaseMultiplier(item.getType());
                    }
                    if (item.containsEnchantment(Enchantment.EFFICIENCY)) {
                        efficiencyLevel = item.getEnchantmentLevel(Enchantment.EFFICIENCY);
                    }
                }

                // Append efficiency calculations to mining speeds
                if (efficiencyLevel > 0 && speedMultiplier > 1.0f) {
                    speedMultiplier += (float) (efficiencyLevel * efficiencyLevel) + 1.0f;
                }

                // Account for active Haste levels
                if (hasteEffect != null && player.hasPotionEffect(hasteEffect)) {
                    PotionEffect effect = player.getPotionEffect(hasteEffect);
                    if (effect != null) {
                        speedMultiplier *= 1.0f + ((effect.getAmplifier() + 1) * 0.2f);
                    }
                }

                // Account for active Mining Fatigue delays
                if (fatigueEffect != null && player.hasPotionEffect(fatigueEffect)) {
                    PotionEffect effect = player.getPotionEffect(fatigueEffect);
                    if (effect != null) {
                        speedMultiplier *= Math.pow(0.3, Math.min(effect.getAmplifier() + 1, 4));
                    }
                }

                // Check environment penalties (Airborne/Submerged states slow down mining)
                if (!player.isOnGround()) speedMultiplier /= 5.0f;
                if (player.getEyeLocation().getBlock().isLiquid()) speedMultiplier /= 5.0f;

                // Calculate ticks to finish breaking block state
                float damagePerTick = speedMultiplier / (hardness * 30.0f);
                boolean isInstantMine = damagePerTick >= 1.0f;

                // Subtract 1 tick (50ms) to create a protective latency buffer zone
                long minBreakTime = isInstantMine ? 0 : (long) ((Math.ceil(1.0f / damagePerTick) - 1) * 50);

                // Run validation rules against simulated expectations
                if (hardness > 0 && !isInstantMine && timeTaken < minBreakTime) {
                    manager.flag(player, "AntiSpeedMine", "Broke block in " + timeTaken + "ms (Min expected: " + minBreakTime + "ms)");
                    event.setCancelled(true);
                    activeMineSessions.remove(uuid);
                    return;
                }
            }
        }
        activeMineSessions.remove(uuid);
    
        // Reach check
        double distance = player.getEyeLocation().distance(blockCenter);
        if (distance > 5.4) {
            manager.flag(player, "AntiImpossible", "Reach: " + String.format("%.2f", distance) + " blocks");
            event.setCancelled(true);
            return;
        }
    
        // Line of sight check
        Vector lookDirection  = player.getLocation().getDirection().normalize();
        Vector blockDirection = blockCenter.toVector().subtract(player.getEyeLocation().toVector()).normalize();
        double dotProduct = lookDirection.dot(blockDirection);
    
        if (dotProduct < 0.65 && block.getType().isSolid()) {
            manager.flag(player, "AntiImpossible", "Broke block out of vision profile");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (manager.isExcepted(player.getUniqueId())) return;
    
        Block block = event.getBlockPlaced();
        Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
    
        double distance = player.getEyeLocation().distance(blockCenter);
        if (distance > 5.2) {
            manager.flag(player, "AntiReach", "Placed block out of reach: " + String.format("%.2f", distance));
            event.setCancelled(true);
            return;
        }
    
        BlockFace[] structuralFaces = {BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        boolean hasValidAnchor = false;
    
        for (BlockFace face : structuralFaces) {
            Block neighbour = block.getRelative(face);
            if (neighbour.getType().isSolid() || PLACEABLE_ON.contains(neighbour.getType())) {
                hasValidAnchor = true;
                break;
            }
        }
    
        if (!hasValidAnchor) {
            manager.flag(player, "AntiScaffold", "Suspicious floating layout placement");
            event.setCancelled(true);
        }
    }    

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (manager.isExcepted(player.getUniqueId())) return;

        Location center = event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5);
        double distance = player.getEyeLocation().distance(center);

        if (distance > 5.4) {
            manager.flag(player, "AntiImpossible", "Interact Reach: " + String.format("%.2f", distance));
            event.setCancelled(true);
        }
    }

    private float getToolBaseMultiplier(Material tool) {
        String name = tool.name();
        if (name.startsWith("GOLDEN_")) return 12.0f;
        if (name.startsWith("NETHERITE_")) return 9.0f;
        if (name.startsWith("DIAMOND_")) return 8.0f;
        if (name.startsWith("IRON_")) return 6.0f;
        if (name.startsWith("STONE_")) return 4.0f;
        if (name.startsWith("WOODEN_")) return 2.0f;
        if (tool == Material.SHEARS) return 1.5f;
        return 1.0f;
    }

    private PotionEffectType getPotionEffectFallback(String modernName, String legacyName) {
        try {
            PotionEffectType type = PotionEffectType.getByName(modernName);
            return type != null ? type : PotionEffectType.getByName(legacyName);
        } catch (Exception e) {
            return PotionEffectType.getByName(legacyName);
        }
    }
}