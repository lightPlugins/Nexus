package io.nexstudios.internal.nms.v1_21_8.packets;

import io.nexstudios.nexus.bukkit.hologram.HoloBuilder;
import io.nexstudios.nexus.bukkit.hologram.HoloBuilderFactory;
import io.nexstudios.nexus.bukkit.hologram.NexHologram;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PaperHoloBuilder implements HoloBuilder, HoloBuilderFactory {

    private enum ViewerMode { ONLY_PLAYER, EVERYONE }

    private Location baseLoc;
    private List<Component> lines = new ArrayList<>();
    private Integer backgroundArgb = 0x00000000;
    private Integer lineWidth = 200;
    private String billboard = "fixed";
    private boolean seeThrough = false;
    private boolean shadow = false;

    private ViewerMode viewerMode = ViewerMode.EVERYONE;
    private Player onlyViewer;
    private Entity attachTo;

    @Override
    public HoloBuilder create() {
        return new PaperHoloBuilder();
    }

    @Override
    public HoloBuilder location(Location loc) {
        this.baseLoc = loc == null ? null : loc.clone();
        return this;
    }

    @Override
    public HoloBuilder lines(List<Component> lines) {
        this.lines.clear();
        if (lines != null) this.lines.addAll(lines);
        return this;
    }

    @Override public HoloBuilder lineSpacing(double spacing) { return this; }
    @Override public HoloBuilder backgroundColor(Integer argb) { this.backgroundArgb = argb; return this; }
    @Override public HoloBuilder billboard(String billboard) { if (billboard != null) this.billboard = billboard.toLowerCase(Locale.ROOT); return this; }
    @Override public HoloBuilder lineWidth(Integer px) { this.lineWidth = px; return this; }
    @Override public HoloBuilder seeThrough(boolean v) { this.seeThrough = v; return this; }
    @Override public HoloBuilder shadow(boolean v) { this.shadow = v; return this; }
    @Override public HoloBuilder textOpacity(byte opacity) { return this; }
    @Override public HoloBuilder scale(Vector s) { return this; }
    @Override public HoloBuilder viewRange(Integer blocks) { return this; }

    @Override
    public HoloBuilder viewersEveryOne() {
        this.viewerMode = ViewerMode.EVERYONE;
        this.onlyViewer = null;
        return this;
    }

    @Override
    public HoloBuilder viewerOnly(Player p) {
        this.viewerMode = ViewerMode.ONLY_PLAYER;
        this.onlyViewer = p;
        return this;
    }

    @Override public HoloBuilder useTextDisplay(boolean use) { return this; }

    @Override
    public HoloBuilder attachToEntity(Entity entity) {
        this.attachTo = entity;
        return this;
    }

    @Override
    public NexHologram build() {
        Objects.requireNonNull(baseLoc, "Hologram location must not be null");
        if (lines.isEmpty()) lines = List.of(Component.empty());

        int id = IdGen.next();
        UUID uuid = UUID.randomUUID();

        Level nmsLevel = ((CraftWorld) Objects.requireNonNull(baseLoc.getWorld())).getHandle();

        PaperHologram holo = new PaperHologram(
                nmsLevel,
                baseLoc.clone(),
                new ArrayList<>(lines),
                id,
                uuid,
                backgroundArgb == null ? 0x00000000 : backgroundArgb,
                lineWidth == null ? 200 : lineWidth,
                billboard,
                attachTo
        );

        if (viewerMode == ViewerMode.ONLY_PLAYER && onlyViewer != null && onlyViewer.isOnline()) {
            holo.showTo(onlyViewer);
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) holo.showTo(p);
        }

        return holo;
    }

    // ----------------------------------------------------

    private static final class PaperHologram implements NexHologram {

        private final Level nmsLevel;

        private Location baseLoc;
        private List<Component> lines;
        private final int entityId;
        private final UUID uuid;
        private final int backgroundArgb;
        private final int lineWidth;
        private final String billboard;
        private final Entity attachTo;

        private final Set<UUID> visibleFor = new HashSet<>();
        private final Accessors accessors;

        private PaperHologram(Level nmsLevel,
                              Location baseLoc,
                              List<Component> lines,
                              int entityId,
                              UUID uuid,
                              int backgroundArgb,
                              int lineWidth,
                              String billboard,
                              Entity attachTo) {
            this.nmsLevel = nmsLevel;
            this.baseLoc = baseLoc;
            this.lines = lines;
            this.entityId = entityId;
            this.uuid = uuid;
            this.backgroundArgb = backgroundArgb;
            this.lineWidth = lineWidth;
            this.billboard = billboard;
            this.attachTo = attachTo;
            this.accessors = Accessors.resolve(nmsLevel);
        }

        @Override
        public void showTo(Player player) {
            if (player == null || !player.isOnline()) return;
            var sp = ((CraftPlayer) player).getHandle();

            sp.connection.send(spawnTextDisplay(entityId, uuid, baseLoc));
            sp.connection.send(metaTextDisplay(entityId, joinLines(lines)));

            if (attachTo != null) {
                ClientboundSetPassengersPacket pkt = createPassengerPacketForBukkitEntity(attachTo, new int[]{entityId});
                if (pkt != null) {
                    sp.connection.send(pkt);
                }
            }

            visibleFor.add(player.getUniqueId());
        }

        @Override
        public void hideFrom(Player player) {
            if (player == null || !player.isOnline()) return;
            ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(entityId));
            visibleFor.remove(player.getUniqueId());
        }

        @Override
        public void showToAllOnline() {
            for (Player p : Bukkit.getOnlinePlayers()) showTo(p);
        }

        @Override
        public void hideFromAll() {
            for (UUID uid : new ArrayList<>(visibleFor)) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) hideFrom(p);
            }
        }

        @Override
        public void updateLines(List<Component> newLines) {
            if (newLines == null || newLines.isEmpty()) return;
            this.lines = new ArrayList<>(newLines);

            broadcast(metaTextDisplay(entityId, joinLines(newLines)));

            if (attachTo != null) {
                ClientboundSetPassengersPacket pkt = createPassengerPacketForBukkitEntity(attachTo, new int[]{entityId});
                if (pkt != null) {
                    broadcast(pkt);
                }
            }
        }

        @Override
        public void teleport(Location newBase) {
            if (newBase == null) return;
            this.baseLoc = newBase.clone();
        }

        @Override
        public void destroy() {
            hideFromAll();
        }

        private void broadcast(Packet<?> pkt) {
            for (UUID uid : visibleFor) {
                Player p = Bukkit.getPlayer(uid);
                if (p == null || !p.isOnline()) continue;
                ((CraftPlayer) p).getHandle().connection.send(pkt);
            }
        }

        private static Component joinLines(List<Component> lines) {
            if (lines.size() == 1) return lines.get(0);
            Component result = Component.empty();
            for (int i = 0; i < lines.size(); i++) {
                result = result.append(lines.get(i));
                if (i + 1 < lines.size()) {
                    result = result.append(Component.text("\n"));
                }
            }
            return result;
        }

        private Packet<?> spawnTextDisplay(int id, UUID uuid, Location loc) {
            return new ClientboundAddEntityPacket(
                    id, uuid,
                    loc.getX(), loc.getY(), loc.getZ(),
                    0f, 0f,
                    EntityType.TEXT_DISPLAY,
                    0,
                    Vec3.ZERO,
                    0.0
            );
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Packet<?> metaTextDisplay(int id, Component text) {
            List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();

            // billboard (jetzt als BYTE!)
            if (accessors.displayBillboard != null) {
                byte code = switch (billboard) {
                    case "vertical" -> (byte) 1;
                    case "horizontal" -> (byte) 2;
                    case "center" -> (byte) 3;
                    default -> (byte) 0;
                };
                values.add(SynchedEntityData.DataValue.create((EntityDataAccessor) accessors.displayBillboard, Byte.valueOf(code)));
            }

            // text
            if (accessors.textText != null) {
                var nms = toNms(text == null ? Component.text(" ") : text);
                values.add(SynchedEntityData.DataValue.create((EntityDataAccessor) accessors.textText, nms));
            }

            // width
            if (accessors.textLineWidth != null) {
                values.add(SynchedEntityData.DataValue.create((EntityDataAccessor) accessors.textLineWidth, Math.max(1, lineWidth)));
            }

            // background
            if (accessors.textBackgroundColor != null) {
                values.add(SynchedEntityData.DataValue.create((EntityDataAccessor) accessors.textBackgroundColor, backgroundArgb));
            }

            return new ClientboundSetEntityDataPacket(id, values);
        }

        private static net.minecraft.network.chat.Component toNms(Component adv) {
            return io.papermc.paper.adventure.PaperAdventure.asVanilla(
                    adv == null ? Component.empty() : adv
            );
        }

        private static ClientboundSetPassengersPacket createPassengerPacketForBukkitEntity(Entity bukkitVehicle,
                                                                                           int[] passengers) {
            try {
                net.minecraft.world.entity.Entity nmsVehicle = ((CraftEntity) bukkitVehicle).getHandle();
                ClientboundSetPassengersPacket pkt = new ClientboundSetPassengersPacket(nmsVehicle);

                Field f = ClientboundSetPassengersPacket.class.getDeclaredField("passengers");
                f.setAccessible(true);
                f.set(pkt, passengers);

                return pkt;
            } catch (Throwable t) {
                return null;
            }
        }
    }

    // ---------- helper classes ----------

    private static final class IdGen {
        private static final AtomicInteger NEXT = new AtomicInteger(1_700_000_000);
        static int next() { return NEXT.getAndIncrement(); }
    }

    private static final class Accessors {
        final EntityDataAccessor<?> displayBillboard;
        final EntityDataAccessor<?> textText;
        final EntityDataAccessor<?> textLineWidth;
        final EntityDataAccessor<?> textBackgroundColor;

        private Accessors(EntityDataAccessor<?> displayBillboard,
                          EntityDataAccessor<?> textText,
                          EntityDataAccessor<?> textLineWidth,
                          EntityDataAccessor<?> textBackgroundColor) {
            this.displayBillboard = displayBillboard;
            this.textText = textText;
            this.textLineWidth = textLineWidth;
            this.textBackgroundColor = textBackgroundColor;
        }

        static Accessors resolve(Level level) {
            Display.TextDisplay sample = newTextDisplay(level);

            EntityDataAccessor<?> bill = null;
            EntityDataAccessor<?> txt = null;
            EntityDataAccessor<?> lw = null;
            EntityDataAccessor<?> bg = null;

            if (sample != null) {
                Class<?> current = sample.getClass();
                while (current != null && current != Object.class) {
                    for (Field f : current.getDeclaredFields()) {
                        if (!EntityDataAccessor.class.isAssignableFrom(f.getType())) continue;
                        try {
                            f.setAccessible(true);
                            Object v = f.get(sample);
                            if (v == null) continue;
                            String name = f.getName().toLowerCase(Locale.ROOT);

                            if (bill == null && name.contains("bill")) {
                                bill = (EntityDataAccessor<?>) v;
                                continue;
                            }
                            if (txt == null && name.contains("text")) {
                                txt = (EntityDataAccessor<?>) v;
                                continue;
                            }
                            if (lw == null && (name.contains("line") || name.contains("width"))) {
                                lw = (EntityDataAccessor<?>) v;
                                continue;
                            }
                            if (bg == null && name.contains("background")) {
                                bg = (EntityDataAccessor<?>) v;
                            }
                        } catch (Throwable ignored) {}
                    }
                    current = current.getSuperclass();
                }

                // Fallbacks
                if (bill == null) bill = firstAccessor(sample);
                if (txt == null) txt = firstAccessorOf(sample, "text");
            }

            return new Accessors(bill, txt, lw, bg);
        }

        private static Display.TextDisplay newTextDisplay(Level level) {
            try {
                Constructor<TextDisplay> c = TextDisplay.class.getDeclaredConstructor(EntityType.class, Level.class);
                c.setAccessible(true);
                return c.newInstance(EntityType.TEXT_DISPLAY, level);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static EntityDataAccessor<?> firstAccessor(Object o) {
            Class<?> c = o.getClass();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (!EntityDataAccessor.class.isAssignableFrom(f.getType())) continue;
                    try {
                        f.setAccessible(true);
                        Object v = f.get(o);
                        if (v != null) return (EntityDataAccessor<?>) v;
                    } catch (Throwable ignored) {}
                }
                c = c.getSuperclass();
            }
            return null;
        }

        private static EntityDataAccessor<?> firstAccessorOf(Object o, String contains) {
            Class<?> c = o.getClass();
            String needle = contains.toLowerCase(Locale.ROOT);
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (!EntityDataAccessor.class.isAssignableFrom(f.getType())) continue;
                    try {
                        f.setAccessible(true);
                        Object v = f.get(o);
                        if (v != null && f.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                            return (EntityDataAccessor<?>) v;
                        }
                    } catch (Throwable ignored) {}
                }
                c = c.getSuperclass();
            }
            return null;
        }
    }
}
