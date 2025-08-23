package io.nexstudios.nexus.bukkit.inv.event;

import io.nexstudios.nexus.bukkit.inv.NexInventory;
import io.nexstudios.nexus.bukkit.inv.NexInventoryManager;
import io.nexstudios.nexus.bukkit.inv.NexInventoryView;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class NexInventoryClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;
        if (!(clicked.getHolder(false) instanceof NexInventory)) return;
        if (clicked != event.getView().getTopInventory()) return;
        if (event.getRawSlot() < 0) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        NexInventoryView view = NexInventoryManager.get().viewOf(player.getUniqueId());
        if (view == null) return;

        view.handleClick(event, event.getRawSlot());
    }
}

