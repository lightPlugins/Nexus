package io.nexstudios.internal.nms.v1_21_11.packets;

import io.netty.channel.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.items.ItemPacketModifier;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Packet-basierte Client-side Item-Manipulation (Lore/Name) inkl. Cursor/Carried-Item.
 *
 * Zusätzlich:
 * - Outgoing Packet Rewriter: modifiziert vanilla inventory packets "inline".
 *
 * DEBUG:
 * - Sehr ausführliche Logs per NexusPlugin.nexusLogger.info(...)
 */
public final class ItemPacketModifierImpl implements ItemPacketModifier {

    // -------------------------------------------------------------------------
    // DEBUG
    // -------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    private static void log(String msg) {
        if (!DEBUG) return;
        try {
            NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] " + msg);
        } catch (Throwable ignored) {
            // avoid crashing if logger not ready
        }
    }

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
        log("setOutgoingItemTransformer: " + (transformer == null ? "null" : transformer.getClass().getName()));
    }

    @Override
    public void installPacketRewriter(Player bukkitPlayer) {
        if (bukkitPlayer == null) {
            log("installPacketRewriter: player=null");
            return;
        }
        if (!bukkitPlayer.isOnline()) {
            log("installPacketRewriter: player=" + bukkitPlayer.getName() + " not online");
            return;
        }
        if (!(bukkitPlayer instanceof CraftPlayer cp)) {
            log("installPacketRewriter: player=" + bukkitPlayer.getName() + " not CraftPlayer (" + bukkitPlayer.getClass().getName() + ")");
            return;
        }

        ServerPlayer nms = cp.getHandle();
        Channel ch = getPlayerChannel(nms);
        if (ch == null) {
            log("installPacketRewriter: player=" + bukkitPlayer.getName() + " channel=null (getPlayerChannel failed)");
            return;
        }

        log("installPacketRewriter: player=" + bukkitPlayer.getName() + " channel=" + ch);

        ch.eventLoop().execute(() -> {
            try {
                ChannelPipeline pl = ch.pipeline();

                if (pl.get(HANDLER_NAME) != null) {
                    log("installPacketRewriter: handler already present for " + bukkitPlayer.getName());
                    return;
                }

                // Dump pipeline names for debugging
                try {
                    log("installPacketRewriter: pipeline for " + bukkitPlayer.getName() + " = " + pl.names());
                } catch (Throwable t) {
                    log("installPacketRewriter: failed to dump pipeline names: " + t);
                }

                // Add handler before packet_handler if possible, else fallback to addLast
                if (pl.get("packet_handler") != null) {
                    pl.addBefore("packet_handler", HANDLER_NAME, new OutgoingRewriteHandler(bukkitPlayer));
                    log("installPacketRewriter: added handler BEFORE packet_handler for " + bukkitPlayer.getName());
                } else {
                    pl.addLast(HANDLER_NAME, new OutgoingRewriteHandler(bukkitPlayer));
                    log("installPacketRewriter: packet_handler not found, added handler LAST for " + bukkitPlayer.getName());
                }

                try {
                    log("installPacketRewriter: pipeline(after) for " + bukkitPlayer.getName() + " = " + pl.names());
                } catch (Throwable t) {
                    log("installPacketRewriter: failed to dump pipeline names(after): " + t);
                }

            } catch (Throwable t) {
                log("installPacketRewriter: exception on eventLoop for " + bukkitPlayer.getName() + ": " + t);
            }
        });
    }

    @Override
    public void uninstallPacketRewriter(Player bukkitPlayer) {
        if (bukkitPlayer == null) {
            log("uninstallPacketRewriter: player=null");
            return;
        }
        if (!(bukkitPlayer instanceof CraftPlayer cp)) {
            log("uninstallPacketRewriter: player=" + bukkitPlayer.getName() + " not CraftPlayer");
            return;
        }

        ServerPlayer nms = cp.getHandle();
        Channel ch = getPlayerChannel(nms);
        if (ch == null) {
            log("uninstallPacketRewriter: player=" + bukkitPlayer.getName() + " channel=null");
            return;
        }

        ch.eventLoop().execute(() -> {
            try {
                ChannelPipeline pl = ch.pipeline();
                if (pl.get(HANDLER_NAME) != null) {
                    pl.remove(HANDLER_NAME);
                    log("uninstallPacketRewriter: removed handler for " + bukkitPlayer.getName());
                } else {
                    log("uninstallPacketRewriter: handler not present for " + bukkitPlayer.getName());
                }
            } catch (Throwable t) {
                log("uninstallPacketRewriter: exception for " + bukkitPlayer.getName() + ": " + t);
            }
        });
    }

    private final class OutgoingRewriteHandler extends ChannelDuplexHandler {

        private final Player bukkitPlayer;

        private OutgoingRewriteHandler(Player bukkitPlayer) {
            this.bukkitPlayer = bukkitPlayer;
            log("OutgoingRewriteHandler: created for " + safeName(bukkitPlayer));
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            OutgoingItemTransformer t = outgoingTransformer;

            if (DEBUG) {
                String pktName = (msg == null) ? "null" : msg.getClass().getName();
                log("write(): player=" + safeName(bukkitPlayer)
                        + " transformer=" + (t == null ? "null" : "set")
                        + " packet=" + pktName);
            }

            if (t == null) {
                super.write(ctx, msg, promise);
                return;
            }

            try {
                Object out = rewriteOutgoing(bukkitPlayer, msg, t);
                if (DEBUG && out != msg) {
                    log("write(): packet rewritten for " + safeName(bukkitPlayer) + " from=" + className(msg) + " to=" + className(out));
                }
                super.write(ctx, out, promise);
            } catch (Throwable ex) {
                log("write(): EXCEPTION for " + safeName(bukkitPlayer) + " packet=" + className(msg) + " ex=" + ex);
                super.write(ctx, msg, promise);
            }
        }
    }

    private static String safeName(Player p) {
        return (p == null) ? "null" : (p.getName() + "/" + p.getUniqueId());
    }

    private static String className(Object o) {
        return (o == null) ? "null" : o.getClass().getName();
    }

    private Object rewriteOutgoing(Player player, Object pkt, OutgoingItemTransformer transformer) {
        if (pkt == null) return null;

        // 1) Single slot update
        if (pkt instanceof ClientboundContainerSetSlotPacket) {
            if (DEBUG) log("rewriteOutgoing: matched SetSlot for " + safeName(player));
            return rewriteSetSlot(player, pkt, transformer);
        }

        String cn = pkt.getClass().getName();

        // 2) Bulk content update
        if (cn.endsWith("ClientboundContainerSetContentPacket")) {
            if (DEBUG) log("rewriteOutgoing: matched SetContent for " + safeName(player) + " class=" + cn);
            return rewriteSetContent(player, pkt, transformer);
        }

        // 3) Dedicated cursor packet
        if (cn.endsWith("ClientboundSetCursorItemPacket")) {
            if (DEBUG) log("rewriteOutgoing: matched CursorOnly for " + safeName(player) + " class=" + cn);
            return rewriteCursorOnly(player, pkt, transformer);
        }

        return pkt;
    }

    private Object rewriteSetSlot(Player player, Object pkt, OutgoingItemTransformer transformer) {
        PacketAccess acc;
        try {
            acc = accessCache.computeIfAbsent(pkt.getClass(), PacketAccess::forSetSlot);
        } catch (Throwable t) {
            log("rewriteSetSlot: cannot build PacketAccess for " + className(pkt) + " ex=" + t);
            return pkt;
        }

        int containerId = acc.getInt(pkt, 0);
        int stateId = acc.getInt(pkt, 1);
        int slot = acc.getInt(pkt, 2);

        net.minecraft.world.item.ItemStack nmsStack = (net.minecraft.world.item.ItemStack) acc.getObj(pkt, 0);

        if (DEBUG) {
            log("rewriteSetSlot: player=" + safeName(player) + " containerId=" + containerId + " stateId=" + stateId + " slot=" + slot
                    + " nmsStack=" + (nmsStack == null ? "null" : nmsStack.getItem().toString()));
        }

        if (nmsStack == null) return pkt;

        ItemStack bukkit = CraftItemStack.asBukkitCopy(nmsStack);
        if (bukkit == null || bukkit.getType().isAir()) return pkt;

        final OutgoingItemTransformer.Context ctx =
                (slot < 0) ? OutgoingItemTransformer.Context.CURSOR : OutgoingItemTransformer.Context.SLOT;

        ItemStack out = transformer.transform(player, ctx, slot, bukkit);


        if (out == null) {
            if (DEBUG) log("rewriteSetSlot: transformer returned null (no change) for " + safeName(player) + " slot=" + slot);
            return pkt;
        }

        net.minecraft.world.item.ItemStack nmsOut = CraftItemStack.asNMSCopy(out);
        Object rebuilt = acc.newInstance(containerId, stateId, slot, nmsOut);

        if (rebuilt == null) {
            log("rewriteSetSlot: rebuilt==null for " + safeName(player) + " slot=" + slot + " (ctor mismatch?)");
            return pkt;
        }

        return rebuilt;
    }

    @SuppressWarnings("unchecked")
    private Object rewriteSetContent(Player player, Object pkt, OutgoingItemTransformer transformer) {
        PacketAccess acc;
        try {
            acc = accessCache.computeIfAbsent(pkt.getClass(), PacketAccess::forSetContent);
        } catch (Throwable t) {
            log("rewriteSetContent: cannot build PacketAccess for " + className(pkt) + " ex=" + t);
            return pkt;
        }

        int containerId = acc.getInt(pkt, 0);
        int stateId = acc.getInt(pkt, 1);

        Object oItems = acc.getObj(pkt, 0);
        Object oCarried = acc.getObj(pkt, 1);

        List<net.minecraft.world.item.ItemStack> items = null;
        if (oItems instanceof List<?> list) {
            items = (List<net.minecraft.world.item.ItemStack>) list;
        }

        net.minecraft.world.item.ItemStack carried =
                (oCarried instanceof net.minecraft.world.item.ItemStack s) ? s : null;

        if (DEBUG) {
            log("rewriteSetContent: player=" + safeName(player)
                    + " containerId=" + containerId + " stateId=" + stateId
                    + " items=" + (items == null ? "null" : items.size())
                    + " carried=" + (carried == null ? "null" : "present"));
        }

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
                    if (DEBUG) log("rewriteSetContent: transformed slotIndex=" + i + " for " + safeName(player));
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
                    if (DEBUG) log("rewriteSetContent: transformed CARRIED for " + safeName(player));
                }
            }
        }

        if (!changed) {
            if (DEBUG) log("rewriteSetContent: nothing changed for " + safeName(player));
            return pkt;
        }

        Object rebuilt = acc.newInstanceSetContent(containerId, stateId, newItems, newCarried);
        if (rebuilt == null) {
            log("rewriteSetContent: rebuilt==null for " + safeName(player) + " (ctor mismatch?)");
            return pkt;
        }

        return rebuilt;
    }

    private Object rewriteCursorOnly(Player player, Object pkt, OutgoingItemTransformer transformer) {
        PacketAccess acc;
        try {
            acc = accessCache.computeIfAbsent(pkt.getClass(), PacketAccess::forCursorOnly);
        } catch (Throwable t) {
            log("rewriteCursorOnly: cannot build PacketAccess for " + className(pkt) + " ex=" + t);
            return pkt;
        }

        net.minecraft.world.item.ItemStack carried = (net.minecraft.world.item.ItemStack) acc.getObj(pkt, 0);
        if (carried == null) return pkt;

        ItemStack bukkit = CraftItemStack.asBukkitCopy(carried);
        if (bukkit == null || bukkit.getType().isAir()) return pkt;

        ItemStack out = transformer.transform(player, OutgoingItemTransformer.Context.CURSOR, -1, bukkit);
        if (out == null) {
            if (DEBUG) log("rewriteCursorOnly: transformer returned null for " + safeName(player));
            return pkt;
        }

        net.minecraft.world.item.ItemStack nmsOut = CraftItemStack.asNMSCopy(out);
        Object rebuilt = acc.newInstance(nmsOut);

        if (rebuilt == null) {
            log("rewriteCursorOnly: rebuilt==null for " + safeName(player) + " (ctor mismatch?)");
            return pkt;
        }

        return rebuilt;
    }

    // -------------------------------------------------------------------------
    // Reflection helpers for packet fields/ctors (cached per class)
    // -------------------------------------------------------------------------

    private static final class PacketAccess {
        private final Field[] intFields;
        private final Field[] objFields;
        private final Constructor<?> ctor;

        // mapping for ctor argument order (mainly for SetContent)
        private final int ctorContainerIdIndex;
        private final int ctorStateIdIndex;
        private final int ctorItemsIndex;
        private final int ctorCarriedIndex;

        private PacketAccess(Field[] intFields,
                             Field[] objFields,
                             Constructor<?> ctor,
                             int ctorContainerIdIndex,
                             int ctorStateIdIndex,
                             int ctorItemsIndex,
                             int ctorCarriedIndex) {
            this.intFields = intFields;
            this.objFields = objFields;
            this.ctor = ctor;
            this.ctorContainerIdIndex = ctorContainerIdIndex;
            this.ctorStateIdIndex = ctorStateIdIndex;
            this.ctorItemsIndex = ctorItemsIndex;
            this.ctorCarriedIndex = ctorCarriedIndex;

            log("PacketAccess: created for " + ctor.getDeclaringClass().getName()
                    + " ctor=" + ctor
                    + " map(containerId=" + ctorContainerIdIndex
                    + ", stateId=" + ctorStateIdIndex
                    + ", items=" + ctorItemsIndex
                    + ", carried=" + ctorCarriedIndex + ")");
        }

        static PacketAccess forSetSlot(Class<?> cls) {
            log("PacketAccess.forSetSlot: " + cls.getName());
            Field[] ints = findFields(cls, int.class, 3);
            Field[] objs = findAssignableFields(cls, net.minecraft.world.item.ItemStack.class, 1);
            Constructor<?> ctor = findCtor(cls, int.class, int.class, int.class, net.minecraft.world.item.ItemStack.class);
            return new PacketAccess(ints, objs, ctor, 0, 1, -1, -1);
        }

        static PacketAccess forSetContent(Class<?> cls) {
            log("PacketAccess.forSetContent: " + cls.getName());
            Field[] ints = findFields(cls, int.class, 2);

            Field listField = findFirstField(cls, List.class);
            Field stackField = findFirstAssignableField(cls, net.minecraft.world.item.ItemStack.class);

            if (listField == null || stackField == null) {
                throw new IllegalStateException("SetContent packet fields not found: " + cls.getName());
            }
            listField.setAccessible(true);
            stackField.setAccessible(true);

            Constructor<?> ctor = findCompatibleCtor(cls, int.class, int.class, List.class, net.minecraft.world.item.ItemStack.class);

            int[] mapping = mapSetContentCtor(ctor);
            return new PacketAccess(ints, new Field[]{listField, stackField}, ctor,
                    mapping[0], mapping[1], mapping[2], mapping[3]);
        }

        static PacketAccess forCursorOnly(Class<?> cls) {
            log("PacketAccess.forCursorOnly: " + cls.getName());
            Field[] objs = findAssignableFields(cls, net.minecraft.world.item.ItemStack.class, 1);
            Constructor<?> ctor = findCtor(cls, net.minecraft.world.item.ItemStack.class);
            return new PacketAccess(new Field[0], objs, ctor, -1, -1, -1, 0);
        }

        int getInt(Object o, int index) {
            try {
                return (int) intFields[index].get(o);
            } catch (Throwable t) {
                log("PacketAccess.getInt: failed index=" + index + " ex=" + t);
                return 0;
            }
        }

        Object getObj(Object o, int index) {
            try {
                return objFields[index].get(o);
            } catch (Throwable t) {
                log("PacketAccess.getObj: failed index=" + index + " ex=" + t);
                return null;
            }
        }

        Object newInstance(Object... args) {
            try {
                return ctor.newInstance(args);
            } catch (Throwable t) {
                log("PacketAccess.newInstance: failed ctor=" + ctor + " args=" + Arrays.toString(args) + " ex=" + t);
                return null;
            }
        }

        Object newInstanceSetContent(int containerId,
                                     int stateId,
                                     List<?> items,
                                     net.minecraft.world.item.ItemStack carried) {
            try {
                Object[] ctorArgs = new Object[ctor.getParameterCount()];
                ctorArgs[ctorContainerIdIndex] = containerId;
                ctorArgs[ctorStateIdIndex] = stateId;
                ctorArgs[ctorItemsIndex] = items;
                ctorArgs[ctorCarriedIndex] = carried;
                return ctor.newInstance(ctorArgs);
            } catch (Throwable t) {
                log("PacketAccess.newInstanceSetContent: failed ctor=" + ctor
                        + " map=(" + ctorContainerIdIndex + "," + ctorStateIdIndex + "," + ctorItemsIndex + "," + ctorCarriedIndex + ")"
                        + " ex=" + t);
                return null;
            }
        }

        private static int[] mapSetContentCtor(Constructor<?> ctor) {
            Class<?>[] pt = ctor.getParameterTypes();

            int containerIdIdx = -1;
            int stateIdIdx = -1;
            int itemsIdx = -1;
            int carriedIdx = -1;

            for (int i = 0; i < pt.length; i++) {
                Class<?> p = pt[i];
                if (p == int.class) {
                    if (containerIdIdx == -1) containerIdIdx = i;
                    else if (stateIdIdx == -1) stateIdIdx = i;
                } else if (List.class.isAssignableFrom(p)) {
                    itemsIdx = i;
                } else if (net.minecraft.world.item.ItemStack.class.isAssignableFrom(p)) {
                    carriedIdx = i;
                }
            }

            if (containerIdIdx == -1 || stateIdIdx == -1 || itemsIdx == -1 || carriedIdx == -1) {
                throw new IllegalStateException("Cannot map SetContent ctor params: " + ctor);
            }

            log("mapSetContentCtor: ctor=" + ctor
                    + " containerIdIdx=" + containerIdIdx
                    + " stateIdIdx=" + stateIdIdx
                    + " itemsIdx=" + itemsIdx
                    + " carriedIdx=" + carriedIdx);

            return new int[]{containerIdIdx, stateIdIdx, itemsIdx, carriedIdx};
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
                log("findCompatibleCtor: selected ctor for " + cls.getName() + " -> " + c);
                return c;
            }

            Constructor<?>[] ctors = cls.getDeclaredConstructors();
            if (ctors.length == 0) throw new IllegalStateException("No ctors for " + cls.getName());
            Constructor<?> c = ctors[0];
            c.setAccessible(true);
            log("findCompatibleCtor: FALLBACK first ctor for " + cls.getName() + " -> " + c);
            return c;
        }
    }

    // -------------------------------------------------------------------------
    // Channel lookup (NMS-only)
    // -------------------------------------------------------------------------

    private static Channel getPlayerChannel(ServerPlayer nmsPlayer) {
        try {
            if (nmsPlayer == null) {
                NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: nmsPlayer=null");
                return null;
            }

            Object listener = nmsPlayer.connection; // ServerGamePacketListenerImpl (oder Superklasse)
            if (listener == null) {
                NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: nmsPlayer.connection=null");
                return null;
            }

            NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: listenerClass=" + listener.getClass().getName());

            // 1) Find net.minecraft.network.Connection in class hierarchy
            Connection conn = null;

            Field connField = findFieldByTypeHierarchy(listener.getClass(), Connection.class);
            if (connField != null) {
                connField.setAccessible(true);
                Object val = connField.get(listener);
                if (val instanceof Connection c) {
                    conn = c;
                    NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: found Connection field="
                            + connField.getDeclaringClass().getName() + "#" + connField.getName());
                } else {
                    NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: Connection field resolved but value is "
                            + (val == null ? "null" : val.getClass().getName()));
                }
            } else {
                // helpful debug: dump fields across hierarchy
                NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: could not find Connection field by type. Dumping fields:");
                dumpFieldsHierarchy(listener.getClass());
                return null;
            }

            if (conn == null) {
                NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: conn=null after field read");
                return null;
            }

            // 2) Find io.netty.channel.Channel in Connection hierarchy
            Field chField = findFieldByTypeHierarchy(conn.getClass(), Channel.class);
            if (chField != null) {
                chField.setAccessible(true);
                Object val = chField.get(conn);
                if (val instanceof Channel ch) {
                    NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: found Channel field="
                            + chField.getDeclaringClass().getName() + "#" + chField.getName()
                            + " channel=" + ch);
                    return ch;
                } else {
                    NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: Channel field resolved but value is "
                            + (val == null ? "null" : val.getClass().getName()));
                    return null;
                }
            } else {
                NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: could not find Channel field by type. Dumping Connection fields:");
                dumpFieldsHierarchy(conn.getClass());
                return null;
            }

        } catch (Throwable t) {
            NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] getPlayerChannel: EXCEPTION " + t);
            return null;
        }
    }

    private static Field findFieldByTypeHierarchy(Class<?> start, Class<?> targetType) {
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (targetType.isAssignableFrom(f.getType())) {
                    return f;
                }
            }
        }
        return null;
    }

    private static void dumpFieldsHierarchy(Class<?> start) {
        List<String> lines = new ArrayList<>();
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            lines.add("  Class: " + c.getName());
            for (Field f : c.getDeclaredFields()) {
                lines.add("    - " + f.getName() + " : " + f.getType().getName());
            }
        }
        for (String s : lines) {
            NexusPlugin.nexusLogger.info("[ItemPacketModifierImpl] " + s);
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

        if (!validatePlayer(bukkitPlayer)) {
            log("sendFakeItem: validatePlayer failed player=" + safeName(bukkitPlayer));
            return;
        }
        if (item == null || item.getType().isAir()) {
            log("sendFakeItem: item null/air player=" + safeName(bukkitPlayer));
            return;
        }

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            if (slot < 0 || slot >= menu.slots.size()) {
                log("sendFakeItem: slot out of range slot=" + slot + " menuSlots=" + menu.slots.size() + " player=" + safeName(bukkitPlayer));
                return;
            }

            ItemStack fakeItem = item.clone();
            if (displayName != null) fakeItem.editMeta(meta -> meta.displayName(displayName));
            if (lore != null) fakeItem.lore(lore);

            net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(fakeItem);

            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            log("sendFakeItem: sending SetSlot packet player=" + safeName(bukkitPlayer) + " containerId=" + containerId + " stateId=" + stateId + " slot=" + slot);

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, stateId, slot, nmsStack));
        } catch (Exception e) {
            log("sendFakeItem: EXCEPTION player=" + safeName(bukkitPlayer) + " ex=" + e);
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetItemView(Player bukkitPlayer, int slot) {
        if (!validatePlayer(bukkitPlayer)) {
            log("resetItemView: validatePlayer failed player=" + safeName(bukkitPlayer));
            return;
        }

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            if (slot < 0 || slot >= menu.slots.size()) {
                log("resetItemView: slot out of range slot=" + slot + " menuSlots=" + menu.slots.size() + " player=" + safeName(bukkitPlayer));
                return;
            }

            net.minecraft.world.item.ItemStack real = menu.getSlot(slot).getItem();

            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            log("resetItemView: sending real SetSlot player=" + safeName(bukkitPlayer) + " containerId=" + containerId + " stateId=" + stateId + " slot=" + slot);

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, stateId, slot, real.copy()));
        } catch (Exception e) {
            log("resetItemView: EXCEPTION player=" + safeName(bukkitPlayer) + " ex=" + e);
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void sendFakeCursorItem(Player bukkitPlayer,
                                   ItemStack item,
                                   @Nullable Component displayName,
                                   @Nullable List<Component> lore) {

        if (!validatePlayer(bukkitPlayer)) {
            log("sendFakeCursorItem: validatePlayer failed player=" + safeName(bukkitPlayer));
            return;
        }
        if (item == null || item.getType().isAir()) {
            log("sendFakeCursorItem: item null/air player=" + safeName(bukkitPlayer));
            return;
        }

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            ItemStack fakeItem = item.clone();
            if (displayName != null) fakeItem.editMeta(meta -> meta.displayName(displayName));
            if (lore != null) fakeItem.lore(lore);

            net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(fakeItem);

            log("sendFakeCursorItem: trySendCursorPacket player=" + safeName(bukkitPlayer));
            if (trySendCursorPacket(nmsPlayer, nmsStack)) return;

            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            log("sendFakeCursorItem: fallback slot=-1 player=" + safeName(bukkitPlayer) + " containerId=" + containerId + " stateId=" + stateId);

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, stateId, -1, nmsStack));
        } catch (Exception e) {
            log("sendFakeCursorItem: EXCEPTION player=" + safeName(bukkitPlayer) + " ex=" + e);
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetCursorItem(Player bukkitPlayer) {
        if (!validatePlayer(bukkitPlayer)) {
            log("resetCursorItem: validatePlayer failed player=" + safeName(bukkitPlayer));
            return;
        }

        try {
            ServerPlayer nmsPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
            AbstractContainerMenu menu = nmsPlayer.containerMenu;

            net.minecraft.world.item.ItemStack realCarried = menu.getCarried();

            log("resetCursorItem: trySendCursorPacket player=" + safeName(bukkitPlayer));
            if (trySendCursorPacket(nmsPlayer, realCarried.copy())) return;

            int stateId = menu.incrementStateId();
            int containerId = menu.containerId;

            log("resetCursorItem: fallback slot=-1 player=" + safeName(bukkitPlayer) + " containerId=" + containerId + " stateId=" + stateId);

            nmsPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, stateId, -1, realCarried.copy()));
        } catch (Exception e) {
            log("resetCursorItem: EXCEPTION player=" + safeName(bukkitPlayer) + " ex=" + e);
            handleError(bukkitPlayer, e);
        }
    }

    @Override
    public void resetAllItems(Player bukkitPlayer) {
        if (!validatePlayer(bukkitPlayer)) {
            log("resetAllItems: validatePlayer failed player=" + safeName(bukkitPlayer));
            return;
        }

        log("resetAllItems: player=" + safeName(bukkitPlayer) + " resetting slots 0..40");
        for (int slot = 0; slot <= 40; slot++) {
            resetItemView(bukkitPlayer, slot);
        }
    }

    private static boolean trySendCursorPacket(ServerPlayer nmsPlayer, net.minecraft.world.item.ItemStack stack) {
        if (nmsPlayer == null) {
            log("trySendCursorPacket: nmsPlayer=null");
            return false;
        }

        String[] candidates = new String[]{
                "net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket",
                "net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket"
        };

        for (String cn : candidates) {
            try {
                log("trySendCursorPacket: trying " + cn);
                Class<?> pktClass = Class.forName(cn);
                Constructor<?> c = pktClass.getDeclaredConstructor(net.minecraft.world.item.ItemStack.class);
                c.setAccessible(true);
                Object pkt = c.newInstance(stack);
                nmsPlayer.connection.send((Packet<?>) pkt);
                log("trySendCursorPacket: SUCCESS " + cn);
                return true;
            } catch (Throwable t) {
                log("trySendCursorPacket: FAILED " + cn + " ex=" + t);
            }
        }

        log("trySendCursorPacket: no candidate worked");
        return false;
    }

    private boolean validatePlayer(Player player) {
        return player != null && player.isOnline();
    }

    private void handleError(Player player, Exception e) {
        e.printStackTrace();
    }
}
