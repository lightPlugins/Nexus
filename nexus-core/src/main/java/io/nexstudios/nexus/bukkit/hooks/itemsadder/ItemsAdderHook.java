package io.nexstudios.nexus.bukkit.hooks.itemsadder;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemsAdderHook implements Listener {

    public List<IItemsAdderLoadDataEvent> eventList = new ArrayList<>();

    public ItemsAdderHook(NexusPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void registerEvents(List<IItemsAdderLoadDataEvent> events) {
        eventList.addAll(events);
    }

    @EventHandler
    public void onItemsAdderReload(ItemsAdderLoadDataEvent event) {
        eventList.forEach(single -> single.execute(event));
    }

    public ItemStack getItem(String namespace) {
        CustomStack customStack = CustomStack.getInstance(namespace.toLowerCase(Locale.ROOT));
        if(customStack != null) {
            return customStack.getItemStack();
        }
        return null;
    }

    public boolean exists(String namespace) {
        return CustomStack.isInRegistry(namespace.toLowerCase(Locale.ROOT));
    }

    public boolean isCustomBlock(Block block) {
        return CustomBlock.byAlreadyPlaced(block) != null;
    }

    public boolean placeCustomBlock(String namespace, Location location) {
        CustomBlock customBlock = CustomBlock.getInstance(namespace.toLowerCase(Locale.ROOT));
        if(customBlock != null) {
            customBlock.place(location);
            return true;
        }
        return false;
    }
}
