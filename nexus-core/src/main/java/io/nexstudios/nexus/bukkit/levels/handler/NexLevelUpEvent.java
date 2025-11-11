package io.nexstudios.nexus.bukkit.levels.handler;

import io.nexstudios.nexus.bukkit.levels.LevelKey;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// Level Up
public final class NexLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final LevelKey key;
    private final int oldLevel;
    private final int newLevel;

    public NexLevelUpEvent(UUID playerId, LevelKey key, int oldLevel, int newLevel) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.key = key;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public UUID getPlayerId() { return playerId; }
    public LevelKey getKey() { return key; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}