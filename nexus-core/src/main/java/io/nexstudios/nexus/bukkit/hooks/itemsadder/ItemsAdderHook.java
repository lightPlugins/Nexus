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
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public String getPlacedCustomBlockId(Block block) {
        if (block == null) return null;
        CustomBlock placed = CustomBlock.byAlreadyPlaced(block);
        if (placed == null) return null;
        return placed.getNamespacedID(); // e.g. "some_pack:some_block"
    }

    /**
     * Matches a config id like "itemsadder:some_pack:some_block" against a placed block.
     */
    public boolean matchesBlock(Block block, String configId) {
        if (block == null || configId == null) return false;

        String raw = configId.trim();
        if (raw.isEmpty()) return false;

        String prefix = "itemsadder:";
        if (!raw.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return false;
        }

        String expected = raw.substring(prefix.length()).trim(); // "some_pack:some_block"
        if (expected.isEmpty()) return false;

        String actual = getPlacedCustomBlockId(block);
        return actual != null && actual.equalsIgnoreCase(expected);
    }
}
