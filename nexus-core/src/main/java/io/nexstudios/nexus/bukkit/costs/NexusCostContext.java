package io.nexstudios.nexus.bukkit.costs;

import io.nexstudios.nexus.bukkit.actions.NexParams;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * Context for resolving a single cost entry.
 * Supports offline and online players.
 */
public record NexusCostContext(
        OfflinePlayer offlinePlayer,
        @Nullable Location location,
        CostData data,
        NexParams params
) {

    public NexusCostContext {
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
     * Returns the online player, or null if the player is offline.
     */
    @Nullable
    public Player player() {
        return offlinePlayer.getPlayer();
    }

    /**
     * Requires an online player, throws if the player is offline.
     */
    public Player requireOnlinePlayer() {
        Player p = player();
        if (p == null) {
            throw new IllegalStateException(
                    "This cost requires the offlinePlayer to be online, but the player is offline: " + offlinePlayer.getName()
            );
        }
        return p;
    }

    /**
     * Returns a copy with different parameters.
     */
    public NexusCostContext withParams(NexParams newParams) {
        return new NexusCostContext(offlinePlayer, location, data, newParams == null ? NexParams.empty() : newParams);
    }

    /**
     * Returns a copy with one additional/overwritten parameter.
     */
    public NexusCostContext withParam(String key, String value) {
        return new NexusCostContext(offlinePlayer, location, data, params.with(key, value));
    }
}