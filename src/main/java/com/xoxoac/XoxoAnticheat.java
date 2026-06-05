package com.xoxoac;

import com.xoxoac.modules.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class XoxoAnticheat extends JavaPlugin {

    @Override
    public void onEnable() {
        ViolationManager violationManager = new ViolationManager(this);
        XoxoCommand xoxoCommand = new XoxoCommand(violationManager);
        Objects.requireNonNull(getCommand("xoxo")).setExecutor(xoxoCommand);
        Objects.requireNonNull(getCommand("xoxo")).setTabCompleter(xoxoCommand);

        // Register xoxo-AntiCheat Modules
        getServer().getPluginManager().registerEvents(new MovementChecks(violationManager), this);
        getServer().getPluginManager().registerEvents(new CombatChecks(violationManager), this);
        getServer().getPluginManager().registerEvents(new BlockChecks(violationManager), this);

        getLogger().info("xoxo-AntiCheat modules successfully hooked!");
    }

    @Override
    public void onDisable() {
        getLogger().info("VirtualSMP Plugin has been disabled!");
    }
}