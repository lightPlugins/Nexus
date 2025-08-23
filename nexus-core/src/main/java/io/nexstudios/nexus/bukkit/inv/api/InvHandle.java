// Java
package io.nexstudios.nexus.bukkit.inv.api;

import io.nexstudios.nexus.bukkit.inv.NexInventory;
import io.nexstudios.nexus.bukkit.inv.NexInventoryView;
import io.nexstudios.nexus.bukkit.inv.NexOnClick;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public interface InvHandle {
    InvKey key();

    // Öffnet das Inventar für einen Spieler
    NexInventoryView open(Player player);

    // Optional: Body-Items setzen (z. B. vom Plugin dynamisch geliefert)
    InvHandle setBodyItems(List<?> models, NexOnClick clickHandler);

    // Optional: Zugriff auf das NexInventory (z. B. für erweiterte Einstellungen)
    NexInventory inventory();

    // Optional: Augmentierungen nach Config-Load
    InvHandle onPostLoad(Consumer<NexInventory> augment);
}