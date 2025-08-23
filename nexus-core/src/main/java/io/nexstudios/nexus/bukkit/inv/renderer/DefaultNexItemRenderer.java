package io.nexstudios.nexus.bukkit.inv.renderer;

import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class DefaultNexItemRenderer implements NexItemRenderer {

    @Override
    public ItemStack renderStatic(NexItemConfig cfg, String inventoryId) {
        if (cfg == null) return null;

        int amount = cfg.amount != null ? Math.max(1, cfg.amount) : 1;
        String spec = cfg.itemSpec == null ? "" : cfg.itemSpec.trim().toLowerCase(Locale.ROOT);
        boolean isVanilla = spec.startsWith("minecraft:") || spec.startsWith("vanilla:") || !spec.contains(":");

        if (isVanilla) {
            // Basis-Item für Vanilla: nur Material + Amount (keine Language/Anreicherung hier)
            Material mat = resolveMaterial(spec);
            if (mat == null) mat = Material.STONE;
            return NexServices.newItemBuilder()
                    .material(mat)
                    .amount(amount)
                    .build();
        }

        // Externe Items: nur parseItem + Amount (unverändert lassen)
        ItemStack stack = StringUtils.parseItem(spec);
        if (stack == null) return null;
        stack.setAmount(amount);
        return stack;
    }

    private static Material resolveMaterial(String spec) {
        if (spec == null || spec.isBlank()) return Material.STONE;
        String normalized = normalizeMinecraftSpec(spec);
        Material m = Material.matchMaterial(normalized, false);
        if (m != null) return m;
        String enumName = normalized.substring(normalized.indexOf(':') + 1).toUpperCase(Locale.ROOT);
        try { return Material.valueOf(enumName); } catch (IllegalArgumentException ignored) { return null; }
    }

    private static String normalizeMinecraftSpec(String spec) {
        if (spec == null || spec.isBlank()) return "minecraft:stone";
        if (!spec.contains(":")) return "minecraft:" + spec.toLowerCase(Locale.ROOT);
        if (spec.startsWith("vanilla:")) return "minecraft:" + spec.substring("vanilla:".length());
        return spec.toLowerCase(Locale.ROOT);
    }
}