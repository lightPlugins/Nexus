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


    @FunctionalInterface
    interface OutgoingItemTransformer {

        enum Context {
            SLOT,          // single slot update
            CONTENT_SLOT,  // bulk content list entry
            CURSOR         // carried/cursor item
        }

        /**
         * IMPORTANT: may be called off-main-thread (network thread). Must be thread-safe.
         *
         * @param player   recipient
         * @param context  where this item is used (slot/content/cursor)
         * @param rawSlot  for SLOT/CONTENT_SLOT: packet index; for CURSOR: -1
         * @param original original item (should not be mutated)
         * @return null => keep original; otherwise return a new/clone ItemStack with modified visuals
         */
        @Nullable ItemStack transform(Player player, Context context, int rawSlot, ItemStack original);
    }

    void setOutgoingItemTransformer(@Nullable OutgoingItemTransformer transformer);

    void installPacketRewriter(Player player);
    void uninstallPacketRewriter(Player player);
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

    // -------------------------------------------------------------------------
    // Cursor / Carried Item (Item am Mauszeiger)
    // -------------------------------------------------------------------------

    /**
     * Sends a fake item (cursor / carried item) to the player's client.
     * This affects the item "on the mouse cursor" in the currently open container/menu.
     *
     * Default implementation: not supported (so other NMS modules don't break).
     */
    default void sendFakeCursorItem(Player player,
                                    ItemStack item,
                                    @Nullable Component displayName,
                                    @Nullable List<Component> lore) {
        throw new UnsupportedOperationException("Cursor item packets are not supported in this NMS version.");
    }

    /**
     * Convenience: fake only cursor lore.
     */
    default void sendFakeCursorLore(Player player, ItemStack item, @Nullable List<Component> lore) {
        sendFakeCursorItem(player, item, null, lore);
    }

    /**
     * Convenience: fake only cursor name.
     */
    default void sendFakeCursorName(Player player, ItemStack item, @Nullable Component displayName) {
        sendFakeCursorItem(player, item, displayName, null);
    }

    /**
     * Resets the cursor item to show the server-side carried stack.
     *
     * Default implementation: not supported.
     */
    default void resetCursorItem(Player player) {
        throw new UnsupportedOperationException("Cursor item packets are not supported in this NMS version.");
    }
}