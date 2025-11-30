package io.nexstudios.nexus.bukkit.actions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Kontext für die Ausführung einer Action.
 */
public record NexusActionContext(
        LivingEntity actor,
        @Nullable Location location,
        ActionData data,
        NexParams params
) {

    public NexusActionContext {
        if (actor == null) {
            throw new IllegalArgumentException("actor must not be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (params == null) {
            params = NexParams.empty();
        }
    }

    /**
     * Convenience: Actor als Player erzwingen.
     * @throws IllegalStateException wenn actor kein Player ist
     */
    public Player requirePlayer() {
        if (actor instanceof Player p) {
            return p;
        }
        throw new IllegalStateException(
                "This action requires a Player actor, but got: " + actor.getType()
        );
    }

    /**
     * Erzeugt eine Kopie mit anderen Params.
     */
    public NexusActionContext withParams(NexParams newParams) {
        return new NexusActionContext(actor, location, data, newParams == null ? NexParams.empty() : newParams);
    }

    /**
     * Erzeugt eine Kopie mit zusätzlichem/überschriebenem Param.
     */
    public NexusActionContext withParam(String key, String value) {
        return new NexusActionContext(actor, location, data, params.with(key, value));
    }
}