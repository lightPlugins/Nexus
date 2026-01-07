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
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof NexInventory)) {
            return; // kein Nexus-GUI
        }

        Player player = (Player) event.getWhoClicked();
        NexInventoryView view = NexInventoryManager.get().viewOf(player.getUniqueId());
        if (view == null) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;

        // Klick im Top-Inventory
        if (clicked == top) {
            event.setCancelled(true);

            // Sicherheit: nur Slots im Top-Bereich an handleClick geben
            if (rawSlot < top.getSize()) {
                view.handleClick(event, rawSlot);
            }
            return;
        }

        // (unterer Teil)
        if (clicked == event.getView().getBottomInventory()) {
            if (view.isPlayerInventoryLocked()) {
                event.setCancelled(true);
            }
        }

        // Sonstige Inventare (selten, z.B. offhand) – je nach Bedarf:
        // aktuell: nichts Besonderes, normal durchlaufen lassen.
    }
}

