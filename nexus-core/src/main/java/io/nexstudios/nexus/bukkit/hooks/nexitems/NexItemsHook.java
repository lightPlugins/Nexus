package io.nexstudios.nexus.bukkit.hooks.nexitems;

import org.bukkit.inventory.ItemStack;

public interface NexItemsHook {


    String getItemId(ItemStack itemStack);
    boolean isNexusItem(ItemStack itemStack);
    ItemStack getItemById(String templateId);

}
