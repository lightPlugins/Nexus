package io.nexstudios.nexus.bukkit.effects.stats.events;

import io.nexstudios.nexus.bukkit.effects.stats.NexusStat;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class NexusStatRegisteredEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final NexusStat stat;

    public NexusStatRegisteredEvent(NexusStat stat) {
        super(false); // Async=false? -> setze auf true wenn du sicher immer Main-Thread bist? Hier false wäre sicherer. Passe bei Bedarf an.
        this.stat = stat;
    }

    public NexusStat getStat() {
        return stat;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

