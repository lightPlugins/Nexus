package io.nexstudios.nexus.bukkit.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
// ... existing code ...
import org.bukkit.entity.Entity;

import java.util.List;

public interface HoloBuilder {
    HoloBuilder create();

    // Basis
    HoloBuilder location(Location loc);
    HoloBuilder lines(List<Component> lines);

    // Optionen (TextDisplay-orientiert)
    HoloBuilder lineSpacing(double spacing);
    HoloBuilder backgroundColor(Integer argb); // ARGB 0xAARRGGBB
    HoloBuilder billboard(String billboard);   // "fixed", "vertical", "horizontal", "center"
    HoloBuilder lineWidth(Integer px);
    HoloBuilder seeThrough(boolean v);
    HoloBuilder shadow(boolean v);
    HoloBuilder textOpacity(byte opacity);     // 0..255
    HoloBuilder scale(Vector scale);           // optional TextDisplay-Transformation
    HoloBuilder viewRange(Integer blocks);     // optional (Client-seitig, hier nicht hart erzwungen)

    // Sichtbarkeit
    HoloBuilder viewersEveryOne();
    HoloBuilder viewerOnly(Player p);

    // Fallback (ArmorStand) deaktivieren/aktivieren
    HoloBuilder useTextDisplay(boolean use);

    // Passenger-Kopplung (clientseitig): Hologramm an Entity „mounten“
    HoloBuilder attachToEntity(Entity entity);

    NexHologram build();
}