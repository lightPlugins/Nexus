package io.nexstudios.internal.nms.v1_21_8.packets;

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

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Packet-basierte Client-side Item-Manipulation (Lore/Name) inkl. Cursor/Carried-Item.
 *
 * Hinweis:
 * - Slot: wird als "Menu Slot" gesendet. Für Player-Inventory typischerweise 0-40.
 * - Cursor/Carried Item: wird per ContainerSetSlot mit slot=-1 gesendet.
 */
public final class ItemPacketModifierImpl implements ItemPacketModifier {

    @Override
    public void sendFakeItemLore(Player player, int slot, ItemStack item, @Nullable List<Component> lore) {
        sendFakeItem(player, slot, item, null, lore);
    }

    @Override
    public void sendFakeItemName(Player player, int slot, ItemStack item, @Nullable Component displayName) {
        sendFakeItem(player, slot, item, displayName, null);
    }

    @Override
    public void sendFakeItem(Player bukkitPlayer,
                             int slot,
                             ItemStack item,
                             @Nullable Component displayName,
                             @Nullable List<Component> lore) {

        if (!validateInput(bukkitPlayer, slot, item)) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            // sichere Slot-Grenze (passt auch, wenn ein Container offen ist)
            if (slot < 0 || slot >= menu.slots.size()) return;

            ItemStack fakeItem = item.clone();

            if (displayName != null) {
                fakeItem.editMeta(meta -> meta.displayName(displayName));
            }
            if (lore != null) {
                fakeItem.lore(lore);
            }

            net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(fakeItem);

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
    public void resetItemView(Player bukkitPlayer, int slot) {
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            if (slot < 0 || slot >= menu.slots.size()) return;

            // "echtes" Item aus dem aktuellen Menu-Slot (funktioniert für PlayerInv + offene Container)
            net.minecraft.world.item.ItemStack real = menu.getSlot(slot).getItem();

            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    containerId,
                    stateId,
                    slot,
                    real.copy()
            ));

        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    // -------------------------------------------------------------------------
    // Cursor / Carried Item (Item am Mauszeiger)
    // -------------------------------------------------------------------------

    @Override
    public void sendFakeCursorItem(Player bukkitPlayer,
                                   ItemStack item,
                                   @Nullable Component displayName,
                                   @Nullable List<Component> lore) {

        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;
        if (item == null || item.getType().isAir()) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            ItemStack fakeItem = item.clone();
            if (displayName != null) fakeItem.editMeta(meta -> meta.displayName(displayName));
            if (lore != null) fakeItem.lore(lore);

            net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(fakeItem);

            // 1) Preferred: dedicated cursor packet (prevents protocol kick)
            if (trySendCursorPacket(nmsPlayer, nmsStack)) {
                return;
            }

            // 2) Fallback: some protocol variants accept slot=-1 (but in your case it kicked before)
            // Keep it as last resort only.
            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    containerId,
                    stateId,
                    -1,
                    nmsStack
            ));

        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetCursorItem(Player bukkitPlayer) {
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            net.minecraft.world.item.ItemStack realCarried = menu.getCarried();

            // 1) Preferred: dedicated cursor packet
            if (trySendCursorPacket(nmsPlayer, realCarried.copy())) {
                return;
            }

            // 2) Fallback: slot=-1 (last resort)
            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    containerId,
                    stateId,
                    -1,
                    realCarried.copy()
            ));

        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetAllItems(Player bukkitPlayer) {
        if (!validatePlayer(bukkitPlayer)) return;

        // Reset alle Player-Inventory-Slots (0-40)
        for (int slot = 0; slot <= 40; slot++) {
            resetItemView(bukkitPlayer, slot);
        }
    }

    // ---- Validierungs-Methoden ----

    private static boolean trySendCursorPacket(ServerPlayer nmsPlayer, net.minecraft.world.item.ItemStack stack) {
        if (nmsPlayer == null) return false;

        // try a small set of known/likely names across mappings
        String[] candidates = new String[] {
                "net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket",
                "net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket"
        };

        for (String cn : candidates) {
            try {
                Class<?> pktClass = Class.forName(cn);

                // Common case: constructor(ItemStack)
                Constructor<?> c = pktClass.getDeclaredConstructor(net.minecraft.world.item.ItemStack.class);
                c.setAccessible(true);
                Object pkt = c.newInstance(stack);
                nmsPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) pkt);
                return true;
            } catch (NoSuchMethodException ignored) {
                // try next candidate
            } catch (Throwable ignored) {
                // try next candidate
            }
        }

        return false;
    }

    private boolean validateInput(Player player, int slot, ItemStack item) {
        if (!validatePlayer(player)) return false;
        // Slot-Range hier bewusst 0..40 (dein API-Contract). Die NMS-Checks prüfen zusätzlich menu.slots.size().
        if (!validateSlot(slot)) return false;
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
        // bewusst kein player.sendMessage hier -> Library/Service sollte nicht chat-spammen
        e.printStackTrace();
    }
}