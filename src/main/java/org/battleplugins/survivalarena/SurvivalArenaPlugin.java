package org.battleplugins.survivalarena;

import mc.alk.arena.BattleArena;
import mc.alk.arena.util.Log;

import org.bukkit.plugin.java.JavaPlugin;

public class SurvivalArenaPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        BattleArena.registerCompetition(this, "SurvivalArena", "survivalarena", new SurvivalArenaFactory(this), new SurvivalArenaExecutor(this));

        getDataFolder().mkdir();
        saveDefaultConfig();

        Log.info("[" + getName() + "] v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        Log.info("[" + getName() + "] v" + getDescription().getVersion() + " disabled");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        getDataFolder().mkdir();
        saveDefaultConfig();
    }
}
