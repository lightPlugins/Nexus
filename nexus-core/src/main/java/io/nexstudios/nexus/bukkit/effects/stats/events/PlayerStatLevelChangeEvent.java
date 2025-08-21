package io.nexstudios.nexus.bukkit.effects.stats.events;

import io.nexstudios.nexus.bukkit.effects.stats.NexusStat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerStatLevelChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final NexusStat stat;
    private final int oldLevel;
    private final int newLevel;

    public PlayerStatLevelChangeEvent(UUID playerId, NexusStat stat, int oldLevel, int newLevel) {
        super(false); // siehe Hinweis im anderen Event
        this.playerId = playerId;
        this.stat = stat;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public NexusStat getStat() {
        return stat;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

