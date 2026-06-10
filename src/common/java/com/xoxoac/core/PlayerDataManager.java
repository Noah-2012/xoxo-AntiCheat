package com.xoxoac.core;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDataManager {

    private final Map<UUID, PlayerData> players = new HashMap<>();

    public PlayerData get(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), PlayerData::new);
    }

    public void remove(UUID uuid) {
        players.remove(uuid);
    }

    public Collection<PlayerData> all() {
        return players.values();
    }
}
