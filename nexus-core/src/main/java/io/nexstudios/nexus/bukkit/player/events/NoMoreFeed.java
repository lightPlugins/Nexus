package io.nexstudios.nexus.bukkit.player.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class NoMoreFeed implements Listener {

    @EventHandler
    public void alwaysFullHunger(FoodLevelChangeEvent event) {
        event.getEntity().setFoodLevel(20);
        event.getEntity().setSaturation(20);
        event.setCancelled(true);
    }
}
