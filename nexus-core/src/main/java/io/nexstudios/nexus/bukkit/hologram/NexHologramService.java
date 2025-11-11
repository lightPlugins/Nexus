// Java
package io.nexstudios.nexus.bukkit.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public interface NexHologramService {

    interface Handle extends NexHologram {
        UUID id();
        Location baseLocation();
        int refreshTicks();
        void setRefreshTicks(int ticks);
        void setAttachTo(Entity entity); // optional: (de-)aktivieren
        void setVisibilityPredicate(Predicate<Player> predicate);
        void destroy(); // deregistriert automatisch
    }

    // Spezifikation des Hologramms
    final class Spec {
        public Location baseLocation;
        public List<Component> staticLines; // optional
        public Function<Player, List<Component>> perPlayerLines; // optional
        public int refreshTicks = 40; // default 2s
        public Predicate<Player> visibility = p -> true; // default EVERYONE
        public Entity attachTo; // optional

        public Spec base(Location loc) { this.baseLocation = loc; return this; }
        public Spec staticLines(List<Component> lines) { this.staticLines = lines; return this; }
        public Spec perPlayer(Function<Player, List<Component>> fn) { this.perPlayerLines = fn; return this; }
        public Spec refreshTicks(int ticks) { this.refreshTicks = Math.max(1, ticks); return this; }
        public Spec visibility(Predicate<Player> vis) { this.visibility = vis; return this; }
        public Spec attachTo(Entity e) { this.attachTo = e; return this; }
    }

    // Registrierung
    Handle register(Spec spec);

    // Verwaltung
    void unregister(UUID id);
    Handle find(UUID id);
    Collection<Handle> all();

    // Lebenszyklus
    void shutdown(); // destroy() auf allen
}