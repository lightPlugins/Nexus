package io.nexstudios.nexus.bukkit.inv.config;

import java.util.List;
import java.util.Map;

public class NexItemConfig {
    public final String id;             // namespace id (z. B. "heal" oder "my-custom-item")
    public final String namespaceType;  // "navigation", "required", "custom"
    public final String itemSpec;       // Item-String aus der Config
    public final int page;              // 1-basiert in der Config
    public final List<Integer> slots1b; // 1-basiert aus Config
    public final boolean usePageAsAmount;
    public final Integer amount;
    public final Integer modelData;
    public final String tooltipStyle;
    public final String name;
    public final Object lore;           // String oder List<String>
    public final List<String> hideFlags;
    public final List<Map<String, Object>> enchantments;
    public final List<Map<String, Object>> attributes;
    public final Map<String, Object> extras; // frei

    public NexItemConfig(
            String id,
            String namespaceType,
            String itemSpec,
            int page,
            List<Integer> slots1b,
            boolean usePageAsAmount,
            Integer amount,
            Integer modelData,
            String tooltipStyle,
            String name,
            Object lore,
            List<String> hideFlags,
            List<Map<String, Object>> enchantments,
            List<Map<String, Object>> attributes,
            Map<String, Object> extras
    ) {
        this.id = id;
        this.namespaceType = namespaceType;
        this.itemSpec = itemSpec;
        this.page = page;
        this.slots1b = slots1b;
        this.usePageAsAmount = usePageAsAmount;
        this.amount = amount;
        this.modelData = modelData;
        this.tooltipStyle = tooltipStyle;
        this.name = name;
        this.lore = lore;
        this.hideFlags = hideFlags;
        this.enchantments = enchantments;
        this.attributes = attributes;
        this.extras = extras;
    }
}

