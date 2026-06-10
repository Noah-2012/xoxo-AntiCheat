package com.xoxoac.modules.movement;

import com.xoxoac.core.Check;
import com.xoxoac.core.CheckCategory;
import com.xoxoac.core.CheckResult;
import com.xoxoac.core.MoveSnapshot;
import com.xoxoac.core.PlayerData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TimerA extends Check {

    private static final int MAX_MOVES_PER_SECOND = 24;
    private static final int BUFFER_TO_FLAG = 3;

    private final Map<UUID, Integer> buffer = new HashMap<>();

    public TimerA() {
        super("TimerA", CheckCategory.MOVEMENT);
    }

    @Override
    public CheckResult handleMove(Player player, PlayerData data, MoveSnapshot move) {
        if (player.isInsideVehicle()) {
            decay(player.getUniqueId());
            return pass();
        }

        int packets = data.movesInLastSecond();
        if (packets > MAX_MOVES_PER_SECOND) {
            int vl = buffer.merge(player.getUniqueId(), 1, Integer::sum);
            if (vl >= BUFFER_TO_FLAG) {
                return fail("Packet rate " + packets + "/s (max " + MAX_MOVES_PER_SECOND + ", buffer " + vl + ")");
            }
        } else {
            decay(player.getUniqueId());
        }

        return pass();
    }

    private void decay(UUID uuid) {
        buffer.computeIfPresent(uuid, (key, value) -> value <= 1 ? null : value - 1);
    }
}
