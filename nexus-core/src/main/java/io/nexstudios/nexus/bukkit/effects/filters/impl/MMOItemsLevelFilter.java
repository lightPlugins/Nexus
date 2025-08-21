package io.nexstudios.nexus.bukkit.effects.filters.impl;

import io.nexstudios.nexus.bukkit.effects.filters.NexusFilter;
import io.nexstudios.nexus.bukkit.effects.filters.PlayerContext;

public class MMOItemsLevelFilter implements NexusFilter<PlayerContext> {


    @Override
    public boolean test(PlayerContext ctx) {
        return false;
    }
}
