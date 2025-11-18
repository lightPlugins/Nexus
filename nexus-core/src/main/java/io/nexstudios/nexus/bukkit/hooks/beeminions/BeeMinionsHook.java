package io.nexstudios.nexus.bukkit.hooks.beeminions;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import me.leo_s.beeminions.BeeMinionsRework;
import me.leo_s.beeminions.api.BeeAPI;
import me.leo_s.beeminions.api.events.MinionItemsProduceEvent;
import me.leo_s.beeminions.api.events.MinionItemsRemoveEvent;
import me.leo_s.beeminions.api.events.MinionSellItemsEvent;
import me.leo_s.beeminions.core.minion.Minion;
import me.leo_s.beeminions.core.minion.MinionMeta;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BeeMinionsHook implements Listener {

    public static List<IBeeMinionsEvent> beeMinionsEvents = new ArrayList<>();

    public BeeMinionsHook(PluginManager pluginManager) {
        pluginManager.registerEvents(this, NexusPlugin.getInstance());
    }

    public BeeAPI getAPI() {
        return BeeMinionsRework.getPlugin().getApi();
    }

    public boolean createMinion(String skinId, UUID playerId, Location spawnLocation, String minionId) {
        MinionMeta meta = new MinionMeta(minionId, 1);
        Minion minion = getAPI().createMinion(skinId, playerId, spawnLocation, meta);
        return minion != null;
    }

    public boolean createMinion(UUID playerId, Location spawnLocation, String minionId) {
        MinionMeta meta = new MinionMeta(minionId, 1);
        Minion minion = getAPI().createMinion("default_skin", playerId, spawnLocation, meta);
        return minion != null;
    }

    public void deleteMinion(UUID playerId, UUID minionId) {
        getAPI().removeMinion(playerId, minionId);
    }

    @EventHandler
    public void onItemCollect(MinionItemsRemoveEvent event) {
        beeMinionsEvents.forEach(single -> single.execute(event));
    }

    @EventHandler
    public void onItemProduce(MinionItemsProduceEvent event) {
        beeMinionsEvents.forEach(single -> single.execute(event));
    }

    @EventHandler
    public void onItemSell(MinionSellItemsEvent event) {
        beeMinionsEvents.forEach(single -> single.execute(event));
    }

}
