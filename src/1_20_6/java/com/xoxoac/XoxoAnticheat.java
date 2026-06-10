package com.xoxoac;

import com.xoxoac.core.CheckManager;
import com.xoxoac.core.PlayerDataManager;
import com.xoxoac.listeners.PlayerDataListener;
import com.xoxoac.modules.*;
import com.xoxoac.modules.movement.TimerA;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class XoxoAnticheat extends JavaPlugin {

    @Override
    public void onEnable() {
        ViolationManager violationManager = new ViolationManager(this);
        PlayerDataManager playerDataManager = new PlayerDataManager();
        CheckManager checkManager = new CheckManager(violationManager);
        checkManager.register(new TimerA());

        XoxoCommand xoxoCommand = new XoxoCommand(violationManager);
        Objects.requireNonNull(getCommand("xoxo")).setExecutor(xoxoCommand);
        Objects.requireNonNull(getCommand("xoxo")).setTabCompleter(xoxoCommand);

        // Register xoxo-AntiCheat Modules
        MovementChecks movementChecks = new MovementChecks(violationManager);
        CombatChecks combatChecks = new CombatChecks(violationManager);
        BlockChecks blockChecks = new BlockChecks(violationManager);

        getServer().getPluginManager().registerEvents(
                new PlayerDataListener(playerDataManager, checkManager, violationManager), this
        );
        getServer().getPluginManager().registerEvents(movementChecks, this);
        getServer().getPluginManager().registerEvents(combatChecks, this);
        getServer().getPluginManager().registerEvents(blockChecks, this);

        int totalChecks = checkManager.checks().size()
                + movementChecks.checkCount()
                + combatChecks.checkCount()
                + blockChecks.checkCount();

        getLogger().info("xoxo-AntiCheat modules successfully hooked! Checks loaded: " + totalChecks
                + " (packet/core: " + checkManager.checks().size()
                + ", movement: " + movementChecks.checkCount()
                + ", combat: " + combatChecks.checkCount()
                + ", block: " + blockChecks.checkCount() + ")");
    }

    @Override
    public void onDisable() {
        getLogger().info("xoxo-AntiCheat Plugin has been disabled!");
    }
}
