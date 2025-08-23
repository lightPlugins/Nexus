package io.nexstudios.nexus.bukkit.inv.renderer;

import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import org.bukkit.inventory.ItemStack;

public class DefaultNexItemRenderer implements NexItemRenderer {
    @Override
    public ItemStack renderStatic(NexItemConfig cfg, String inventoryId) {
        if (cfg == null) return null;
        String spec = cfg.itemSpec;
        if (spec == null || spec.isBlank()) return null;

        // Falls kein Namespace angegeben ist, als minecraft:<name> interpretieren
        String normalized = spec.contains(":") ? spec : "minecraft:" + spec.trim().toLowerCase();
        return StringUtils.parseItem(normalized);
    }

}

