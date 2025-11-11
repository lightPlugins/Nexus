// Java
package io.nexstudios.nexus.bukkit.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public interface NexHologram {
    void showTo(Player player);
    void hideFrom(Player player);

    void showToAllOnline();
    void hideFromAll();

    void updateLines(List<Component> newLines);
    void teleport(Location newBase);
    void destroy();
}