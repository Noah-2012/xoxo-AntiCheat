package com.xoxoac.modules.movement;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public final class NoSlowA implements MovementCheck {

    @Override
    public String name() {
        return "NoSlowA";
    }

    @Override
    public String check(Player player, MovementContext context) {
        // Bypass Riptide completely. Riptide movement speeds should be checked in Fly/Speed modules.
        if (player.isRiptiding()) {
            return null;
        }

        ItemStack activeItem = player.getActiveItem();
        if (activeItem == null || activeItem.getType() == Material.AIR) {
            return null;
        }

        // Base maximum speed allowed horizontally while actively using an item (e.g., eating, blocking, drawing bow)
        double maxItemSpeed = 0.13;

        // Account for sprinting while using items (e.g., modern versions allow sprinting while using certain items or via specific mechanics)
        if (player.isSprinting()) {
            maxItemSpeed = 0.25;
        }

        // Properly scale speed potion effects based on vanilla percentage modifiers
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            maxItemSpeed += maxItemSpeed * (amplifier * 0.20);
        }

        // Flag if the player's horizontal movement exceeds the item-use speed limit
        if (context.deltaXZ() > maxItemSpeed) {
            return String.format("%.2f bpt while using %s (max %.2f)",
                    context.deltaXZ(),
                    activeItem.getType().name().toLowerCase(),
                    maxItemSpeed);
        }

        return null;
    }
}
