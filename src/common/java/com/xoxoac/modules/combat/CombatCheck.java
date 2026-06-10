package com.xoxoac.modules.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface CombatCheck {

    String name();

    String check(Player player, LivingEntity target, CombatContext context);

    default boolean cancelHit() {
        return false;
    }
}
