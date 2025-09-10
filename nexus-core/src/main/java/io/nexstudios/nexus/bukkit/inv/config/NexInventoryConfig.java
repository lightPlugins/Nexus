package io.nexstudios.nexus.bukkit.inv.config;

import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public class NexInventoryConfig {
    public final String inventoryId;
    public final String title;                 // z. B. "#language:title#"
    public final int rows;                     // 1–6
    public final int updateIntervalTicks;      // aus "update-intervall"
    public final boolean decorationEnabled;
    public final String decorationItemSpec;

    // Navigation sind Pflicht-IDs: close, previous-page, next-page
    public final Map<String, NexItemConfig> navigation; // key = id (close/previous-page/next-page)

    public final List<NexItemConfig> required;
    public final List<NexItemConfig> custom;

    // Body-Zone/Fill-Settings
    public final InvAlignment bodyAlignment;
    public final int bodyRows;                 // Anzahl Zeilen der Body-Zone
    public final int bodyCols;                 // Anzahl Spalten der Body-Zone
    public final List<Integer> bodySlots1b;    // explizite Slots 1-basiert (optional; hat Vorrang vor rows/cols)

    public final ConfigurationSection extraSettings; // frei

    public NexInventoryConfig(String inventoryId,
                              String title,
                              int rows,
                              int updateIntervalTicks,
                              boolean decorationEnabled,
                              String decorationItemSpec,
                              Map<String, NexItemConfig> navigation,
                              List<NexItemConfig> required,
                              List<NexItemConfig> custom,
                              InvAlignment bodyAlignment,
                              int bodyRows,
                              int bodyCols,
                              List<Integer> bodySlots1b,
                              ConfigurationSection extraSettings) {
        this.inventoryId = inventoryId;
        this.title = title;
        this.rows = rows;
        this.updateIntervalTicks = updateIntervalTicks;
        this.decorationEnabled = decorationEnabled;
        this.decorationItemSpec = decorationItemSpec;
        this.navigation = navigation;
        this.required = required;
        this.custom = custom;
        this.bodyAlignment = bodyAlignment;
        this.bodyRows = bodyRows;
        this.bodyCols = bodyCols;
        this.bodySlots1b = bodySlots1b;
        this.extraSettings = extraSettings;
    }
}

