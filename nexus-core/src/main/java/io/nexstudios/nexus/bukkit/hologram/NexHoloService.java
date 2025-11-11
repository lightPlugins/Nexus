// Java
package io.nexstudios.nexus.bukkit.hologram;

import io.nexstudios.nexus.bukkit.platform.NexServices;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public final class NexHoloService implements NexHologramService, Listener {

    private final Plugin plugin;
    private final Map<UUID, Entry> registry = new ConcurrentHashMap<>();
    private int schedulerTaskId = -1;
    private long tickNow = 0L;

    public NexHoloService(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Globaler 1-Tick Loop, pro Hologramm eigener refreshTicks => nextDue
        this.schedulerTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 1L, 1L).getTaskId();
    }

    @Override
    public Handle register(Spec spec) {
        Objects.requireNonNull(spec.baseLocation, "Spec.baseLocation");
        if (spec.staticLines == null && spec.perPlayerLines == null) {
            throw new IllegalArgumentException("Provide either staticLines or perPlayerLines");
        }
        UUID id = UUID.randomUUID();
        Entry e = new Entry(id, spec);
        registry.put(id, e);

        // initial anzeigen für alle passenden Spieler
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (e.visibleFor(p)) e.ensureShown(p);
        }
        return e;
    }

    @Override
    public void unregister(UUID id) {
        Entry e = registry.remove(id);
        if (e != null) e.destroy();
    }

    @Override
    public Handle find(UUID id) {
        return registry.get(id);
    }

    @Override
    public Collection<Handle> all() {
        return Collections.unmodifiableCollection(registry.values());
    }

    @Override
    public void shutdown() {
        for (Entry e : new ArrayList<>(registry.values())) e.destroy();
        registry.clear();
        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
            schedulerTaskId = -1;
        }
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        PlayerChangedWorldEvent.getHandlerList().unregister(this);
    }

    // Tick-Loop
    private void onTick() {
        tickNow++;
        // Wichtig: Kopie der Werte, damit wir während des Loops sicher deregistrieren können
        for (Entry e : new java.util.ArrayList<>(registry.values())) {

            // 1) Hard-Lifecycle: Wenn attachTo nicht mehr lebt/valide ist -> Hologram sofort zerstören
            if (e.attachTo != null) {
                boolean invalid = !e.attachTo.isValid();
                boolean dead = (e.attachTo instanceof org.bukkit.entity.LivingEntity le) && le.isDead();
                if (invalid || dead) {
                    e.destroy();            // hideFromAll + Cleanup im Handle
                    registry.remove(e.id()); // aus Registry entfernen
                    continue;               // nächstes Entry
                }
            }

            // 2) normaler Refresh-Takt: nur ausführen, wenn fällig
            if (tickNow < e.nextDue) continue;
            e.nextDue = tickNow + e.refreshTicks;

            // Sichtbarkeit anwenden
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                boolean wanted = e.visibleFor(p);
                boolean currently = e.visiblePlayers.contains(p.getUniqueId());
                if (wanted && !currently) e.ensureShown(p);
                else if (!wanted && currently) e.ensureHidden(p);
            }

            // Refresh (nur noch einmal hier, kein extra Modulo mehr)
            e.refreshVisible();
        }
    }

    // Events
    @EventHandler public void onJoin(PlayerJoinEvent ev) {
        Player p = ev.getPlayer();
        for (Entry e : registry.values()) {
            if (e.visibleFor(p)) e.ensureShown(p);
        }
    }
    @EventHandler public void onQuit(PlayerQuitEvent ev) {
        Player p = ev.getPlayer();
        for (Entry e : registry.values()) e.ensureHidden(p);
    }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent ev) {
        Player p = ev.getPlayer();
        for (Entry e : registry.values()) {
            boolean wanted = e.visibleFor(p);
            boolean currently = e.visiblePlayers.contains(p.getUniqueId());
            if (wanted && !currently) e.ensureShown(p);
            else if (!wanted && currently) e.ensureHidden(p);
        }
    }

    // -------- interne Entry-Implementierung (Handle) --------

    private final class Entry implements Handle {
        final UUID id;
        final Location base;
        final Predicate<Player> visibility;
        final int refreshTicks;
        int currentRefreshTicks;

        final Function<Player, List<Component>> perPlayer;
        final List<Component> staticLines;

        Entity attachTo;

        final Set<UUID> visiblePlayers = new HashSet<>();
        final Map<UUID, List<Component>> lastRendered = new HashMap<>();

        // die echte Version-Hologram-Instanz pro Spieler (optional)
        final Map<UUID, NexHologram> perViewerHolo = new HashMap<>();

        long nextDue = 0L;

        Entry(UUID id, Spec spec) {
            this.id = id;
            this.base = spec.baseLocation.clone();
            this.visibility = spec.visibility == null ? (p -> true) : spec.visibility;
            this.refreshTicks = Math.max(1, spec.refreshTicks);
            this.currentRefreshTicks = this.refreshTicks;
            this.perPlayer = spec.perPlayerLines;
            this.staticLines = spec.staticLines;
            this.attachTo = spec.attachTo;
            this.nextDue = tickNow; // sofort tickbar
        }

        boolean visibleFor(Player p) {
            return p != null && p.isOnline() && visibility.test(p);
        }

        void ensureShown(Player p) {
            if (p == null || !p.isOnline()) return;
            if (visiblePlayers.add(p.getUniqueId())) {
                // Hologram für diesen Viewer erzeugen
                NexHologram holo = NexServices.newHoloBuilder()
                        .location(base)
                        .lines(resolveLinesFor(p))
                        .viewerOnly(p)
                        .billboard("center") // oder aus Spec anreichern
                        .lineWidth(200)
                        .backgroundColor(0x00000000)
                        .attachToEntity(attachTo)
                        .build();
                perViewerHolo.put(p.getUniqueId(), holo);
                lastRendered.put(p.getUniqueId(), resolveLinesFor(p));
            }
        }

        void ensureHidden(Player p) {
            UUID uid = p.getUniqueId();
            NexHologram holo = perViewerHolo.remove(uid);
            if (holo != null) {
                holo.hideFrom(p);
                holo.destroy();
            }
            visiblePlayers.remove(uid);
            lastRendered.remove(uid);
        }

        @Override public UUID id() { return id; }
        @Override public Location baseLocation() { return base.clone(); }
        @Override public int refreshTicks() { return currentRefreshTicks; }
        @Override public void setRefreshTicks(int ticks) { currentRefreshTicks = Math.max(1, ticks); }
        @Override public void setAttachTo(Entity entity) { this.attachTo = entity; }
        @Override public void setVisibilityPredicate(Predicate<Player> predicate) { /* optional live-change */ }

        // NexHologram delegationen (global)
        @Override public void showTo(Player player) { ensureShown(player); }
        @Override public void hideFrom(Player player) { ensureHidden(player); }
        @Override public void showToAllOnline() { for (Player p : Bukkit.getOnlinePlayers()) ensureShown(p); }
        @Override public void hideFromAll() { for (UUID uid : new ArrayList<>(visiblePlayers)) { Player p = Bukkit.getPlayer(uid); if (p != null) ensureHidden(p); } }
        @Override public void teleport(Location newBase) {
            if (newBase == null) return;
            base.setX(newBase.getX()); base.setY(newBase.getY()); base.setZ(newBase.getZ());
            // Neuaufbau (einfach): verstecken + zeigen
            for (UUID uid : new ArrayList<>(visiblePlayers)) {
                Player p = Bukkit.getPlayer(uid);
                if (p == null) continue;
                ensureHidden(p);
                ensureShown(p);
            }
        }
        @Override public void updateLines(List<Component> newLines) {
            // statisch überschreiben
            if (newLines != null) {
                for (UUID uid : visiblePlayers) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p == null) continue;
                    NexHologram holo = perViewerHolo.get(uid);
                    if (holo == null) continue;
                    holo.updateLines(newLines);
                    lastRendered.put(uid, newLines);
                }
            }
        }
        @Override public void destroy() { hideFromAll(); registry.remove(id); }

        void refreshVisible() {
            // KEIN tickNow % currentRefreshTicks mehr – das macht already onTick per nextDue
            for (UUID uid : new ArrayList<>(visiblePlayers)) {
                Player p = Bukkit.getPlayer(uid);
                if(p == null) continue;
                if (!p.isOnline()) { ensureHidden(p); continue; }

                List<Component> fresh = resolveLinesFor(p);
                List<Component> last  = lastRendered.get(uid);

                if (!equalsLines(last, fresh)) {
                    NexHologram holo = perViewerHolo.get(uid);
                    if (holo != null) {
                        holo.updateLines(fresh);
                        lastRendered.put(uid, fresh);
                    }
                }
            }
        }



        List<Component> resolveLinesFor(Player p) {
            if (perPlayer != null) {
                try { return Optional.ofNullable(perPlayer.apply(p)).orElseGet(List::of); }
                catch (Exception ignored) { return List.of(); }
            }
            return staticLines == null ? List.of() : staticLines;
        }

        boolean equalsLines(List<Component> a, List<Component> b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if (a.size() != b.size()) return false;
            for (int i = 0; i < a.size(); i++) {
                if (!Objects.equals(a.get(i), b.get(i))) return false;
            }
            return true;
        }
    }
}