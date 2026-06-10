package com.xoxoac.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.xoxoac.modules.combat.AutoClickerA;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PacketClickListener extends PacketListenerAbstract {

    // KEIN "new AutoClickerA()", sondern wir holen uns das echte Modul!
    private final AutoClickerA autoClickerModule;
    private final Map<UUID, Long> lastMiningTime = new HashMap<>();
    private final Plugin plugin;

    // Wir übergeben das Modul jetzt im Konstruktor
    public PacketClickListener(Plugin plugin, AutoClickerA autoClickerModule) {
        this.plugin = plugin;
        this.autoClickerModule = autoClickerModule;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = (Player) event.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 1. Block-Abbau im Survival abfangen
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = digging.getAction();

            if (action == DiggingAction.START_DESTROY_BLOCK) {
                lastMiningTime.put(uuid, now);
                autoClickerModule.clear(player); // Leert die Klicks im echten Modul!
            }
            return;
        }

        // 2. Den eigentlichen Linksklick (Armschwung) prüfen
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            WrapperPlayClientAnimation animation = new WrapperPlayClientAnimation(event);

            if (animation.getHand() == InteractionHand.MAIN_HAND) {

                // Wenn der Spieler vor weniger als 250ms einen Block abgebaut hat, ignorieren wir das Gedrückthalten!
                if (lastMiningTime.containsKey(uuid) && (now - lastMiningTime.get(uuid) < 250L)) {
                    return;
                }

                // Klick im echten Modul auswerten
                String alert = autoClickerModule.handleSwing(player, now);

                if (alert != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.kickPlayer("[XoxoAC] Unnatürliches Klickverhalten (AutoClickerA).");
                    });
                }
            }
        }
    }

    public void handleQuit(UUID uuid) {
        lastMiningTime.remove(uuid);
    }
}
