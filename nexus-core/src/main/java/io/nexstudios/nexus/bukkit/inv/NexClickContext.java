package io.nexstudios.nexus.bukkit.inv;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;

public interface NexClickContext {
    Player player();
    String inventoryId();
    int pageIndex();
    int slot();          // Top-Inventory Slot (0-basiert)
    String namespace();  // z. B. "navigation:close", "required:<id>", "custom:<id>", "body"
    boolean isNavigation();
    boolean isRequired();
    boolean isCustom();
    boolean isBody();
    Integer bodyIndex(); // 0-basiert innerhalb der sichtbaren Body-Seite; nur wenn isBody() == true
    ConfigurationSection extraSettings();
}

