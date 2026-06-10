package com.xoxoac.modules.block;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class FastBreakA {

    private final PotionEffectType hasteEffect = getPotionEffectFallback("HASTE", "FAST_DIGGING");
    private final PotionEffectType fatigueEffect = getPotionEffectFallback("MINING_FATIGUE", "SLOW_DIGGING");

    public String name() {
        return "FastBreakA";
    }

    public String check(Player player, BlockBreakContext context) {
        MineSession session = context.mineSession();
        if (session == null || !session.blockLocation().equals(context.block().getLocation())) {
            return null;
        }

        long timeTaken = context.nowMs() - session.startTimeMs();
        float hardness = context.block().getType().getHardness();
        if (hardness <= 0) {
            return null;
        }

        float speedMultiplier = speedMultiplier(player, context);
        float damagePerTick = speedMultiplier / (hardness * 30.0f);
        boolean instantMine = damagePerTick >= 1.0f;
        long minBreakTime = instantMine ? 0 : (long) ((Math.ceil(1.0f / damagePerTick) - 1) * 50);

        if (!instantMine && timeTaken < minBreakTime) {
            return "Broke block in " + timeTaken + "ms (min " + minBreakTime + "ms)";
        }

        return null;
    }

    private float speedMultiplier(Player player, BlockBreakContext context) {
        ItemStack item = player.getInventory().getItemInMainHand();
        float speedMultiplier = 1.0f;
        int efficiencyLevel = 0;

        if (item != null && item.getType() != Material.AIR) {
            if (context.block().getBlockData().isPreferredTool(item)) {
                speedMultiplier = getToolBaseMultiplier(item.getType());
            }
            if (item.containsEnchantment(Enchantment.EFFICIENCY)) {
                efficiencyLevel = item.getEnchantmentLevel(Enchantment.EFFICIENCY);
            }
        }

        if (efficiencyLevel > 0 && speedMultiplier > 1.0f) {
            speedMultiplier += (float) (efficiencyLevel * efficiencyLevel) + 1.0f;
        }

        if (hasteEffect != null && player.hasPotionEffect(hasteEffect)) {
            PotionEffect effect = player.getPotionEffect(hasteEffect);
            if (effect != null) {
                speedMultiplier *= 1.0f + ((effect.getAmplifier() + 1) * 0.2f);
            }
        }

        if (fatigueEffect != null && player.hasPotionEffect(fatigueEffect)) {
            PotionEffect effect = player.getPotionEffect(fatigueEffect);
            if (effect != null) {
                speedMultiplier *= Math.pow(0.3, Math.min(effect.getAmplifier() + 1, 4));
            }
        }

        if (!player.isOnGround()) speedMultiplier /= 5.0f;
        if (player.getEyeLocation().getBlock().isLiquid()) speedMultiplier /= 5.0f;

        return speedMultiplier;
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
