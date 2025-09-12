package io.nexstudios.nexus.bukkit.inv.api;

import io.nexstudios.nexus.bukkit.inv.NexOnClick;
import org.bukkit.inventory.ItemStack;

/**
 * Couples a filler ItemStack with a dedicated click handler.
 * This allows per-item behavior without relying on a global bodyClickHandler.
 */
public record NexFillerEntry(ItemStack item, NexOnClick onClick) {

    public static NexFillerEntry of(ItemStack item, NexOnClick handler) {
        return new NexFillerEntry(item, handler);
    }
}
