package io.nexstudios.nexus.bukkit.conditions;

import io.nexstudios.nexus.bukkit.actions.NexParams;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * Kontext für die Auswertung einer Condition.
 * Unterstützt Offline- und Online-Player.
 */
public record NexusConditionContext(
        OfflinePlayer offlinePlayer,
        @Nullable Location location,
        ConditionData data,
        NexParams params
) {

    public NexusConditionContext {
        if (offlinePlayer == null) {
            throw new IllegalArgumentException("offlinePlayer must not be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (params == null) {
            params = NexParams.empty();
        }
    }

    /**
     * Liefert den Online-Player, falls online, sonst null.
     */
    @Nullable
    public Player player() {
        return offlinePlayer.getPlayer();
    }

    /**
     * Convenience: Online-Player erzwingen.
     * @throws IllegalStateException wenn der Spieler offline ist
     */
    public Player requireOnlinePlayer() {
        Player p = player();
        if (p == null) {
            throw new IllegalStateException(
                    "This condition requires the offlinePlayer to be online, but the player is offline: " + offlinePlayer.getName()
            );
        }
        return p;
    }

    /**
     * Erzeugt eine Kopie mit anderen Params.
     */
    public NexusConditionContext withParams(NexParams newParams) {
        return new NexusConditionContext(offlinePlayer, location, data, newParams == null ? NexParams.empty() : newParams);
    }

    /**
     * Erzeugt eine Kopie mit zusätzlichem/überschriebenem Param.
     */
    public NexusConditionContext withParam(String key, String value) {
        return new NexusConditionContext(offlinePlayer, location, data, params.with(key, value));
    }
}