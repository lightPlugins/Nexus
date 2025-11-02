// nexus-nms/v1_21_R9/src/main/java/io/nexstudios/internal/nms/v1_21_9/packets/ItemPacketModifierImpl.java
package io.nexstudios.internal.nms.v1_21_10.packets;

import io.nexstudios.nexus.bukkit.items.ItemPacketModifier;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemPacketModifierImpl implements ItemPacketModifier {

    @Override
    public void sendFakeItemLore(Player player, int slot, ItemStack item, @Nullable List<Component> lore) {
        sendFakeItem(player, slot, item, null, lore);
    }

    @Override
    public void sendFakeItemName(Player player, int slot, ItemStack item, @Nullable Component displayName) {
        sendFakeItem(player, slot, item, displayName, null);
    }

    @Override
    public void sendFakeItem(Player bukkitPlayer, int slot, ItemStack item,
                             @Nullable Component displayName, @Nullable List<Component> lore) {
        // Validierung
        if (!validateInput(bukkitPlayer, slot, item)) {
            return;
        }

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();

            // Clone und modifiziere das Item
            ItemStack fakeItem = item.clone();

            // DisplayName setzen (wenn nicht null)
            if (displayName != null) {
                fakeItem.editMeta(meta -> meta.displayName(displayName));
            }

            // Lore setzen (wenn nicht null)
            if (lore != null) {
                fakeItem.lore(lore);
            }

            // Konvertiere zu NMS ItemStack
            net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(fakeItem);

            // Hole das aktuelle Container Menu
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            // WICHTIG: Inkrementiere die StateId für Synchronisation
            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            // Erstelle und sende das Packet
            ClientboundContainerSetSlotPacket packet = new ClientboundContainerSetSlotPacket(
                    containerId,
                    stateId,
                    slot,
                    nmsStack
            );

            nmsPlayer.connection.send(packet);

        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetItemView(Player bukkitPlayer, int slot) {
        if (!validatePlayer(bukkitPlayer) || !validateSlot(slot)) {
            return;
        }

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();

            // Hole das echte Item vom Server
            ItemStack realItem = bukkitPlayer.getInventory().getItem(slot);
            net.minecraft.world.item.ItemStack nmsStack = (realItem == null || realItem.getType().isAir()) ?
                    net.minecraft.world.item.ItemStack.EMPTY :
                    CraftItemStack.asNMSCopy(realItem);

            AbstractContainerMenu menu = nmsPlayer.containerMenu;
            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            ClientboundContainerSetSlotPacket packet = new ClientboundContainerSetSlotPacket(
                    containerId,
                    stateId,
                    slot,
                    nmsStack
            );

            nmsPlayer.connection.send(packet);

        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetAllItems(Player bukkitPlayer) {
        if (!validatePlayer(bukkitPlayer)) {
            return;
        }

        // Reset alle Inventory-Slots (0-40)
        for (int slot = 0; slot <= 40; slot++) {
            resetItemView(bukkitPlayer, slot);
        }
    }

    // ---- Validierungs-Methoden ----

    private boolean validateInput(Player player, int slot, ItemStack item) {
        if (!validatePlayer(player)) {
            return false;
        }

        if (!validateSlot(slot)) {
            return false;
        }

        return item != null && !item.getType().isAir();
    }

    private boolean validatePlayer(Player player) {
        return player != null && player.isOnline();
    }

    private boolean validateSlot(int slot) {
        // 0-8: Hotbar
        // 9-35: Main Inventory
        // 36-39: Armor
        // 40: Offhand
        return slot >= 0 && slot <= 40;
    }

    private void handleError(Player player, Exception e) {
        // Logging (optional)
        player.sendMessage("§cFehler beim Aktualisieren des Items!");
        e.printStackTrace();
    }
}