package io.nexstudios.nexus.bukkit.hooks.superiorskyblock;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.handlers.GridManager;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;

import java.util.UUID;

public class SuperiorSkyblockHook {

    /**
     * This object is used as a wrapper to the known player objects of the Bukkit's API. It contains data about the
     * player, states of modes (fly mode etc) and more.
     * @param uuid from the player
     * @return the superior skyblock player Object
     */
    public SuperiorPlayer getSuperiorPlayer(UUID uuid) {
        return SuperiorSkyblockAPI.getPlayer(uuid);
    }

    /**
     * This object is used to cache data about the islands on your server. Members, banned list, multipliers,
     * upgrades and all the other data is stored in this object.
     * @param uuid from the player
     * @return the players Island
     */
    public Island getIslandFromPlayer(UUID uuid) {
        return getSuperiorPlayer(uuid).getIsland();
    }

    /**
     * The grid manager object handles all the islands on your server. If you want to interact with islands, get them
     * from the top list or anything related to that, you should use this object.
     * @return the GridManager of the Core
     */
    public GridManager getGridManager() {
        return SuperiorSkyblockAPI.getGrid();
    }
}
