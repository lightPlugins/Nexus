package io.nexstudios.nexus.bukkit.effects.filters;

import org.bukkit.entity.Player;

public interface PlayerContext extends TriggerContext {
    Player player();
}

