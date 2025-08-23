package io.nexstudios.nexus.bukkit.inv.renderer;

import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import org.bukkit.inventory.ItemStack;

public interface NexItemRenderer {
    ItemStack renderStatic(NexItemConfig cfg, String inventoryId);
}

