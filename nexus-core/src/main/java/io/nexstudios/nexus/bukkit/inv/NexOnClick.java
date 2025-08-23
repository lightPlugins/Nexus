package io.nexstudios.nexus.bukkit.inv;

import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface NexOnClick {
    void onClick(InventoryClickEvent event, NexClickContext ctx);
}

