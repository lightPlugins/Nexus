package io.nexstudios.internal.nms.v1_21_9.packets;

import io.nexstudios.nexus.bukkit.items.ItemPacketModifier;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import io.netty.channel.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Packet-basierte Client-side Item-Manipulation (Lore/Name) inkl. Cursor/Carried-Item.
 *
 * Zusätzlich (NEW):
 * - Outgoing Packet Rewriter: modifiziert vanilla inventory packets "inline" (instant, kein 1-tick flicker).
 *
 * WICHTIG:
 * - Transformer wird im Netty-Thread ausgeführt. Muss thread-safe sein (read-only / cache).
 */
public final class ItemPacketModifierImpl implements ItemPacketModifier {

    // -------------------------------------------------------------------------
    // New API: Outgoing rewrite
    // -------------------------------------------------------------------------

    private static final String HANDLER_NAME = "nexus_item_visual_rewriter";

    private volatile @Nullable OutgoingItemTransformer outgoingTransformer;

    // cache for reflective packet access
    private final Map<Class<?>, PacketAccess> accessCache = new ConcurrentHashMap<>();

    @Override
    public void setOutgoingItemTransformer(@Nullable OutgoingItemTransformer transformer) {
        this.outgoingTransformer = transformer;
    }

    @Override
    public void installPacketRewriter(Player bukkitPlayer) {
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;
        if (!(bukkitPlayer instanceof CraftPlayer cp)) return;

        ServerPlayer nms = cp.getHandle();
        Channel ch = getPlayerChannel(nms);
        if (ch == null) return;

        ch.eventLoop().execute(() -> {
            ChannelPipeline pl = ch.pipeline();
            if (pl.get(HANDLER_NAME) != null) return;

            pl.addBefore("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    OutgoingItemTransformer t = outgoingTransformer;
                    if (t == null) {
                        super.write(ctx, msg, promise);
                        return;
                    }

                    try {
                        Object out = rewriteOutgoing(bukkitPlayer, msg, t);
                        super.write(ctx, out, promise);
                    } catch (Throwable ignored) {
                        // fail-open: never break networking
                        super.write(ctx, msg, promise);
                    }
                }
            });
        });
    }

    @Override
    public void uninstallPacketRewriter(Player bukkitPlayer) {
        if (bukkitPlayer == null) return;
        if (!(bukkitPlayer instanceof CraftPlayer cp)) return;

        ServerPlayer nms = cp.getHandle();
        Channel ch = getPlayerChannel(nms);
        if (ch == null) return;

        ch.eventLoop().execute(() -> {
            ChannelPipeline pl = ch.pipeline();
            if (pl.get(HANDLER_NAME) != null) {
                pl.remove(HANDLER_NAME);
            }
        });
    }

    private Object rewriteOutgoing(Player player, Object pkt, OutgoingItemTransformer transformer) {
        if (pkt == null) return null;

        // 1) Single slot update
        if (pkt instanceof ClientboundContainerSetSlotPacket) {
            return rewriteSetSlot(player, pkt, transformer);
        }

        // 2) Bulk content update (includes carried/cursor in many versions)
        String cn = pkt.getClass().getName();
        if (cn.endsWith("ClientboundContainerSetContentPacket")) {
            return rewriteSetContent(player, pkt, transformer);
        }

        // 3) Dedicated cursor packet (optional in some mappings)
        if (cn.endsWith("ClientboundSetCursorItemPacket")) {
            return rewriteCursorOnly(player, pkt, transformer);
        }

        return pkt;
    }

    private Object rewriteSetSlot(Player player, Object pkt, OutgoingItemTransformer transformer) {
        PacketAccess acc = accessCache.computeIfAbsent(pkt.getClass(), PacketAccess::forSetSlot);

        int containerId = acc.getInt(pkt, 0);
        int stateId     = acc.getInt(pkt, 1);
        int slot        = acc.getInt(pkt, 2);

        net.minecraft.world.item.ItemStack nmsStack =
                (net.minecraft.world.item.ItemStack) acc.getObj(pkt, 0);

        if (nmsStack == null) return pkt;

        ItemStack bukkit = CraftItemStack.asBukkitCopy(nmsStack);
        if (bukkit == null || bukkit.getType().isAir()) return pkt;

        ItemStack out = transformer.transform(player, OutgoingItemTransformer.Context.SLOT, slot, bukkit);
        if (out == null) return pkt;

        net.minecraft.world.item.ItemStack nmsOut = CraftItemStack.asNMSCopy(out);
        Object rebuilt = acc.newInstance(containerId, stateId, slot, nmsOut);
        return (rebuilt != null) ? rebuilt : pkt;
    }

    @SuppressWarnings("unchecked")
    private Object rewriteSetContent(Player player, Object pkt, OutgoingItemTransformer transformer) {
        PacketAccess acc = accessCache.computeIfAbsent(pkt.getClass(), PacketAccess::forSetContent);

        int containerId = acc.getInt(pkt, 0);
        int stateId     = acc.getInt(pkt, 1);

        Object oItems = acc.getObj(pkt, 0);
        Object oCarried = acc.getObj(pkt, 1);

        List<net.minecraft.world.item.ItemStack> items = null;
        if (oItems instanceof List<?> list) {
            items = (List<net.minecraft.world.item.ItemStack>) list;
        }

        net.minecraft.world.item.ItemStack carried =
                (oCarried instanceof net.minecraft.world.item.ItemStack s) ? s : null;

        boolean changed = false;

        List<net.minecraft.world.item.ItemStack> newItems = items;
        if (items != null && !items.isEmpty()) {
            newItems = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                net.minecraft.world.item.ItemStack nms = items.get(i);
                if (nms == null) {
                    newItems.add(null);
                    continue;
                }

                ItemStack bukkit = CraftItemStack.asBukkitCopy(nms);
                if (bukkit == null || bukkit.getType().isAir()) {
                    newItems.add(nms);
                    continue;
                }

                ItemStack out = transformer.transform(player, OutgoingItemTransformer.Context.CONTENT_SLOT, i, bukkit);
                if (out == null) {
                    newItems.add(nms);
                } else {
                    newItems.add(CraftItemStack.asNMSCopy(out));
                    changed = true;
                }
            }
        }

        net.minecraft.world.item.ItemStack newCarried = carried;
        if (carried != null) {
            ItemStack bukkitCarried = CraftItemStack.asBukkitCopy(carried);
            if (bukkitCarried != null && !bukkitCarried.getType().isAir()) {
                ItemStack out = transformer.transform(player, OutgoingItemTransformer.Context.CURSOR, -1, bukkitCarried);
                if (out != null) {
                    newCarried = CraftItemStack.asNMSCopy(out);
                    changed = true;
                }
            } else {
                // carried is air -> usually no need to transform
            }
        }

        if (!changed) return pkt;

        Object rebuilt = acc.newInstance(containerId, stateId, newItems, newCarried);
        return (rebuilt != null) ? rebuilt : pkt;
    }

    private Object rewriteCursorOnly(Player player, Object pkt, OutgoingItemTransformer transformer) {
        PacketAccess acc = accessCache.computeIfAbsent(pkt.getClass(), PacketAccess::forCursorOnly);

        net.minecraft.world.item.ItemStack carried =
                (net.minecraft.world.item.ItemStack) acc.getObj(pkt, 0);
        if (carried == null) return pkt;

        ItemStack bukkit = CraftItemStack.asBukkitCopy(carried);
        if (bukkit == null || bukkit.getType().isAir()) return pkt;

        ItemStack out = transformer.transform(player, OutgoingItemTransformer.Context.CURSOR, -1, bukkit);
        if (out == null) return pkt;

        net.minecraft.world.item.ItemStack nmsOut = CraftItemStack.asNMSCopy(out);
        Object rebuilt = acc.newInstance(nmsOut);
        return (rebuilt != null) ? rebuilt : pkt;
    }

    // -------------------------------------------------------------------------
    // Reflection helpers for packet fields/ctors (cached per class)
    // -------------------------------------------------------------------------

    private static final class PacketAccess {
        private final Field[] intFields;
        private final Field[] objFields;
        private final Constructor<?> ctor;

        private PacketAccess(Field[] intFields, Field[] objFields, Constructor<?> ctor) {
            this.intFields = intFields;
            this.objFields = objFields;
            this.ctor = ctor;
        }

        static PacketAccess forSetSlot(Class<?> cls) {
            // Expected layout: 3x int + 1x ItemStack
            Field[] ints = findFields(cls, int.class, 3);
            Field[] objs = findAssignableFields(cls, net.minecraft.world.item.ItemStack.class, 1);
            Constructor<?> ctor = findCtor(cls, int.class, int.class, int.class, net.minecraft.world.item.ItemStack.class);
            return new PacketAccess(ints, objs, ctor);
        }

        static PacketAccess forSetContent(Class<?> cls) {
            // Expected layout: 2x int + List + ItemStack (order can vary; we pick by types)
            Field[] ints = findFields(cls, int.class, 2);
            Field listField = findFirstField(cls, List.class);
            Field stackField = findFirstAssignableField(cls, net.minecraft.world.item.ItemStack.class);

            if (listField == null || stackField == null) {
                throw new IllegalStateException("SetContent packet fields not found: " + cls.getName());
            }
            listField.setAccessible(true);
            stackField.setAccessible(true);

            // ctor signature varies; we pick first that has (int,int, List, ItemStack) in some order
            Constructor<?> ctor = findCompatibleCtor(cls, int.class, int.class, List.class, net.minecraft.world.item.ItemStack.class);
            return new PacketAccess(ints, new Field[]{ listField, stackField }, ctor);
        }

        static PacketAccess forCursorOnly(Class<?> cls) {
            Field[] objs = findAssignableFields(cls, net.minecraft.world.item.ItemStack.class, 1);
            Constructor<?> ctor = findCtor(cls, net.minecraft.world.item.ItemStack.class);
            return new PacketAccess(new Field[0], objs, ctor);
        }

        int getInt(Object o, int index) {
            try {
                return (int) intFields[index].get(o);
            } catch (Throwable t) {
                return 0;
            }
        }

        Object getObj(Object o, int index) {
            try {
                return objFields[index].get(o);
            } catch (Throwable t) {
                return null;
            }
        }

        Object newInstance(Object... args) {
            try {
                return ctor.newInstance(args);
            } catch (Throwable t) {
                return null;
            }
        }

        private static Field[] findFields(Class<?> cls, Class<?> type, int count) {
            List<Field> out = new ArrayList<>();
            for (Field f : cls.getDeclaredFields()) {
                if (f.getType() == type) {
                    f.setAccessible(true);
                    out.add(f);
                }
            }
            if (out.size() < count) throw new IllegalStateException("Not enough " + type.getName() + " fields in " + cls.getName());
            return out.subList(0, count).toArray(Field[]::new);
        }

        private static Field[] findAssignableFields(Class<?> cls, Class<?> type, int count) {
            List<Field> out = new ArrayList<>();
            for (Field f : cls.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    out.add(f);
                }
            }
            if (out.size() < count) throw new IllegalStateException("Not enough " + type.getName() + " fields in " + cls.getName());
            return out.subList(0, count).toArray(Field[]::new);
        }

        private static Field findFirstField(Class<?> cls, Class<?> type) {
            for (Field f : cls.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) return f;
            }
            return null;
        }

        private static Field findFirstAssignableField(Class<?> cls, Class<?> type) {
            for (Field f : cls.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) return f;
            }
            return null;
        }

        private static Constructor<?> findCtor(Class<?> cls, Class<?>... params) {
            try {
                Constructor<?> c = cls.getDeclaredConstructor(params);
                c.setAccessible(true);
                return c;
            } catch (Exception e) {
                throw new IllegalStateException("Missing ctor in " + cls.getName() + " with " + Arrays.toString(params), e);
            }
        }

        private static Constructor<?> findCompatibleCtor(Class<?> cls, Class<?>... required) {
            outer:
            for (Constructor<?> c : cls.getDeclaredConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                if (pt.length != required.length) continue;

                // multiset compare by assignability
                List<Class<?>> need = new ArrayList<>(Arrays.asList(required));
                for (Class<?> p : pt) {
                    boolean matched = false;
                    for (int i = 0; i < need.size(); i++) {
                        Class<?> r = need.get(i);
                        if (r.isAssignableFrom(p) || (r == int.class && p == int.class)) {
                            need.remove(i);
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) continue outer;
                }

                if (!need.isEmpty()) continue;
                c.setAccessible(true);
                return c;
            }
            // fallback: first ctor
            Constructor<?>[] ctors = cls.getDeclaredConstructors();
            if (ctors.length == 0) throw new IllegalStateException("No ctors for " + cls.getName());
            Constructor<?> c = ctors[0];
            c.setAccessible(true);
            return c;
        }
    }

    // -------------------------------------------------------------------------
    // Channel lookup (NMS-only)
    // -------------------------------------------------------------------------

    private static Channel getPlayerChannel(ServerPlayer nmsPlayer) {
        try {
            Object listener = nmsPlayer.connection; // ServerGamePacketListenerImpl
            if (listener == null) return null;

            // 1) Try field "connection" (net.minecraft.network.Connection)
            Object conn = null;
            try {
                Field fConn = listener.getClass().getDeclaredField("connection");
                fConn.setAccessible(true);
                conn = fConn.get(listener);
            } catch (Throwable ignored) { }

            // 2) Fallback: find field by type name endsWith ".Connection"
            if (conn == null) {
                for (Field f : listener.getClass().getDeclaredFields()) {
                    if (f.getType().getName().endsWith(".Connection")) {
                        f.setAccessible(true);
                        conn = f.get(listener);
                        break;
                    }
                }
            }
            if (conn == null) return null;

            // 3) Try field "channel"
            try {
                Field fCh = conn.getClass().getDeclaredField("channel");
                fCh.setAccessible(true);
                return (Channel) fCh.get(conn);
            } catch (Throwable ignored) { }

            // 4) Fallback: find first Channel-typed field
            for (Field f : conn.getClass().getDeclaredFields()) {
                if (Channel.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (Channel) f.get(conn);
                }
            }

            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Existing implementation (legacy direct-send)
    // -------------------------------------------------------------------------

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

        if (!validatePlayer(bukkitPlayer)) return;
        if (item == null || item.getType().isAir()) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            if (slot < 0 || slot >= menu.slots.size()) return;

            ItemStack fakeItem = item.clone();
            if (displayName != null) fakeItem.editMeta(meta -> meta.displayName(displayName));
            if (lore != null) fakeItem.lore(lore);

            net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(fakeItem);

            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, stateId, slot, nmsStack));
        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetItemView(Player bukkitPlayer, int slot) {
        if (!validatePlayer(bukkitPlayer)) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            if (slot < 0 || slot >= menu.slots.size()) return;

            net.minecraft.world.item.ItemStack real = menu.getSlot(slot).getItem();

            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, stateId, slot, real.copy()));
        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void sendFakeCursorItem(Player bukkitPlayer,
                                   ItemStack item,
                                   @Nullable Component displayName,
                                   @Nullable List<Component> lore) {

        if (!validatePlayer(bukkitPlayer)) return;
        if (item == null || item.getType().isAir()) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            ItemStack fakeItem = item.clone();
            if (displayName != null) fakeItem.editMeta(meta -> meta.displayName(displayName));
            if (lore != null) fakeItem.lore(lore);

            net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(fakeItem);

            // Try dedicated cursor packet first
            if (trySendCursorPacket(nmsPlayer, nmsStack)) return;

            // Last resort
            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;
            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, stateId, -1, nmsStack));
        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetCursorItem(Player bukkitPlayer) {
        if (!validatePlayer(bukkitPlayer)) return;

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            net.minecraft.world.item.ItemStack realCarried = menu.getCarried();

            if (trySendCursorPacket(nmsPlayer, realCarried.copy())) return;

            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;
            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, stateId, -1, realCarried.copy()));
        } catch (Exception e) {
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetAllItems(Player bukkitPlayer) {
        if (!validatePlayer(bukkitPlayer)) return;

        for (int slot = 0; slot <= 40; slot++) {
            resetItemView(bukkitPlayer, slot);
        }
    }

    private static boolean trySendCursorPacket(ServerPlayer nmsPlayer, net.minecraft.world.item.ItemStack stack) {
        if (nmsPlayer == null) return false;

        String[] candidates = new String[] {
                "net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket",
                "net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket"
        };

        for (String cn : candidates) {
            try {
                Class<?> pktClass = Class.forName(cn);
                Constructor<?> c = pktClass.getDeclaredConstructor(net.minecraft.world.item.ItemStack.class);
                c.setAccessible(true);
                Object pkt = c.newInstance(stack);
                nmsPlayer.connection.send((Packet<?>) pkt);
                return true;
            } catch (Throwable ignored) { }
        }

        return false;
    }

    private boolean validatePlayer(Player player) {
        return player != null && player.isOnline();
    }

    private void handleError(Player player, Exception e) {
        e.printStackTrace();
    }
}
