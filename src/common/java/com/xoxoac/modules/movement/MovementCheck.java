package com.xoxoac.modules.movement;

import org.bukkit.entity.Player;

public interface MovementCheck {

    String name();

    String check(Player player, MovementContext context);
}
