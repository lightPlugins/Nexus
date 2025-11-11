package io.nexstudios.nexus.bukkit.levels.handler;

import io.nexstudios.nexus.bukkit.levels.LevelKey;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// Gain XP
public final class NexLevelGainXPEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final LevelKey key;
    private final double deltaXp;
    private final double oldXp;
    private final double newXp;

    public NexLevelGainXPEvent(UUID playerId, LevelKey key, double deltaXp, double oldXp, double newXp) {
        super(!Bukkit.isPrimaryThread()); // falls außerhalb aufgerufen würde
        this.playerId = playerId;
        this.key = key;
        this.deltaXp = deltaXp;
        this.oldXp = oldXp;
        this.newXp = newXp;
    }

    public UUID getPlayerId() { return playerId; }
    public LevelKey getKey() { return key; }
    public double getDeltaXp() { return deltaXp; }
    public double getOldXp() { return oldXp; }
    public double getNewXp() { return newXp; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}