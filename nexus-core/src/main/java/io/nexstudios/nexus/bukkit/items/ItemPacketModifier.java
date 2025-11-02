package io.nexstudios.nexus.bukkit.items;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Modifies item display properties client-side via packets.
 * Changes are only visible to the target player and don't affect the server-side item.
 */
public interface ItemPacketModifier {

    /**
     * Sends a fake item with custom lore to the player's client
     *
     * @param player Target player
     * @param slot Slot number (0-40 for player inventory)
     * @param item Original item
     * @param lore New lore (null to keep original)
     */
    void sendFakeItemLore(Player player, int slot, ItemStack item, @Nullable List<Component> lore);

    /**
     * Sends a fake item with custom display name to the player's client
     *
     * @param player Target player
     * @param slot Slot number (0-40 for player inventory)
     * @param item Original item
     * @param displayName New display name (null to keep original)
     */
    void sendFakeItemName(Player player, int slot, ItemStack item, @Nullable Component displayName);

    /**
     * Sends a fake item with both custom lore and display name
     *
     * @param player Target player
     * @param slot Slot number (0-40 for player inventory)
     * @param item Original item
     * @param displayName New display name (null to keep original)
     * @param lore New lore (null to keep original)
     */
    void sendFakeItem(Player player, int slot, ItemStack item,
                      @Nullable Component displayName, @Nullable List<Component> lore);

    /**
     * Resets the item to show server-side data
     *
     * @param player Target player
     * @param slot Slot number (0-40 for player inventory)
     */
    void resetItemView(Player player, int slot);

    /**
     * Resets all items in the player's inventory
     *
     * @param player Target player
     */
    void resetAllItems(Player player);
}