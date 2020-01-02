package org.battleplugins.survivalarena;

import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.arenas.ArenaFactory;

public class SurvivalArenaFactory implements ArenaFactory {

    private SurvivalArenaPlugin plugin;

    SurvivalArenaFactory(SurvivalArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Arena newArena() {
        return new SurvivalArena(plugin);
    }
}
