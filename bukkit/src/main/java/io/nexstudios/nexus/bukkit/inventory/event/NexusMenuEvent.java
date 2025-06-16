package io.nexstudios.nexus.bukkit.inventory.event;

import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.bukkit.inventory.NexusMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class NexusMenuEvent implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof NexusMenu) {
            if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                // -999 is if the player clicked outside the inventory
                // -1 is if the player clicked on the inventory itself (not a slot)
                if(event.getSlot() == -999 || event.getSlot() == -1) {
                    return;
                }
                event.setCancelled(true);
                Nexus.nexusLogger.debug("Clicked slot id: " + event.getSlot(), 3);
                ((NexusMenu) event.getInventory().getHolder()).handleMenuClick(event);
            }
        }
    }
}
