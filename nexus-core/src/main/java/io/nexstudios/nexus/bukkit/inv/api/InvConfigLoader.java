package io.nexstudios.nexus.bukkit.inv.api;// Java

import io.nexstudios.nexus.bukkit.inv.config.NexInventoryConfig;
import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public final class InvConfigLoader {

    // Mapped eine Bukkit-Config in unsere NexInventoryConfig-Struktur
    public static NexInventoryConfig load(String inventoryId, FileConfiguration cfg) {
        String title = cfg.getString("title", "Inventory");
        int rows = Math.max(1, Math.min(6, cfg.getInt("size", 6)));
        int interval = Math.max(1, cfg.getInt("update-intervall", 2)) * 20; // Sekunden -> Ticks

        boolean decoEnabled = cfg.getBoolean("content.decoration.enable", false);
        String decoItem = cfg.getString("content.decoration.item", "minecraft:black_stained_glass_pane");

        // Navigation (Pflicht-IDs)
        Map<String, NexItemConfig> navigation = new HashMap<>();
        // Variante A: Liste unter content.navigation
        if (cfg.isList("content.navigation")) {
            List<Map<?, ?>> navList = cfg.getMapList("content.navigation");
            for (Map<?, ?> obj : navList) {
                NexItemConfig item = parseItemConfigFromMap("navigation", obj);
                if (item != null && item.id != null && !item.id.isBlank()) {
                    navigation.put(item.id, item);
                }
            }
        } else {
            // Variante B: Schlüsselstruktur content.navigation.close / previous-page / next-page
            for (String navId : List.of("close", "previous-page", "next-page", "back")) {
                String base = "content.navigation." + navId;
                if (cfg.contains(base)) {
                    NexItemConfig item = parseItemConfig(navId, "navigation", cfg, base);
                    navigation.put(navId, item);
                }
            }
        }


        // Required
        List<NexItemConfig> required = new ArrayList<>();
        if (cfg.contains("content.required")) {
            for (Map<?, ?> obj : cfg.getMapList("content.required")) {
                NexItemConfig item = parseItemConfigFromMap("required", obj);
                if (item != null) required.add(item);
            }
        }

        // Custom
        List<NexItemConfig> custom = new ArrayList<>();
        if (cfg.contains("content.custom")) {
            for (Map<?, ?> obj : cfg.getMapList("content.custom")) {
                NexItemConfig item = parseItemConfigFromMap("custom", obj);
                if (item != null) custom.add(item);
            }
        }

        // Body-Zone (optional erweitert; Default: 3x7 CENTER)
        InvAlignment align = InvAlignment.valueOf(cfg.getString("content.body.alignment", "CENTER").toUpperCase(Locale.ROOT));
        int bodyRows = cfg.getInt("content.body.rows", 3);
        int bodyCols = cfg.getInt("content.body.cols", 7);
        List<Integer> bodySlots1b = cfg.getIntegerList("content.body.slots");

        ConfigurationSection extraSection = cfg.getConfigurationSection("content.extra-settings");

        return new NexInventoryConfig(
                inventoryId,
                title,
                rows,
                interval,
                decoEnabled,
                decoItem,
                navigation,
                required,
                custom,
                align,
                bodyRows,
                bodyCols,
                bodySlots1b,
                extraSection
        );
    }

    private static NexItemConfig parseItemConfig(String id, String type, FileConfiguration cfg, String base) {
        String itemSpec = cfg.getString(base + ".item", "minecraft:stone");
        int page = cfg.getInt(base + ".page", 1);
        List<Integer> slots1b = cfg.getIntegerList(base + ".slots");
        boolean usePage = cfg.getBoolean(base + ".use-page-number-as-amount", false);
        Integer modelData = cfg.contains(base + ".model-data") ? cfg.getInt(base + ".model-data") : null;
        Integer amount = cfg.contains(base + ".amount") ? cfg.getInt(base + ".amount") : null;
        String tooltip = cfg.getString(base + ".tooltip-style", null);
        String name = cfg.getString(base + ".name", null);
        Object lore = cfg.contains(base + ".lore") ? cfg.get(base + ".lore") : null;
        List<String> hide = cfg.getStringList(base + ".hide-flags");
        List<Map<String, Object>> ench = castListOfMap(cfg.getList(base + ".enchantments"));
        List<Map<String, Object>> attrs = castListOfMap(cfg.getList(base + ".attributes"));
        Map<String, Object> extras = cfg.contains(base + ".extras")
                ? cfg.getConfigurationSection(base + ".extras").getValues(true)
                : Map.of();
        return new NexItemConfig(
                id,
                type,
                itemSpec,
                page,
                slots1b,
                usePage,
                amount,
                modelData,
                tooltip,
                name,
                lore,
                hide,
                ench,
                attrs,
                extras);
    }

    @SuppressWarnings("unchecked")
    private static NexItemConfig parseItemConfigFromMap(String type, Map<?, ?> raw) {
        try {
            // id prüfen
            Object rawId = raw.get("id");
            if (rawId == null) return null;
            String id = String.valueOf(rawId).trim();
            if (id.isEmpty() || "null".equalsIgnoreCase(id)) return null;

            // item
            Object rawItem = raw.get("item");
            String item = (rawItem != null) ? String.valueOf(rawItem) : "minecraft:stone";

            // page
            int page = 1;
            Object rawPage = raw.get("page");
            if (rawPage instanceof Number n) page = n.intValue();
            else if (rawPage instanceof String s && !s.isBlank()) {
                try { page = Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
            }

            // slots 1-basiert -> Liste Integer
            List<Integer> slots1b = new ArrayList<>();
            Object rawSlots = raw.get("slots");
            if (rawSlots instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Number n) slots1b.add(n.intValue());
                    else if (o instanceof String s) {
                        try { slots1b.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // use-page-number-as-amount
            boolean usePage = false;
            Object rawUsePage = raw.get("use-page-number-as-amount");
            if (rawUsePage instanceof Boolean b) usePage = b;
            else if (rawUsePage instanceof String s) usePage = Boolean.parseBoolean(s);

            // amount
            Integer amount = null;
            Object rawAmount = raw.get("amount");
            if (rawAmount instanceof Number n) amount = n.intValue();
            else if (rawAmount instanceof String s && !s.isBlank()) {
                try { amount = Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
            }

            // model-data
            Integer modelData = null;
            Object rawModel = raw.get("model-data");
            if (rawModel instanceof Number n) modelData = n.intValue();
            else if (rawModel instanceof String s && !s.isBlank()) {
                try { modelData = Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
            }

            // tooltip-style
            String tooltip = null;
            Object rawTooltip = raw.get("tooltip-style");
            if (rawTooltip != null) tooltip = String.valueOf(rawTooltip);

            // name
            String name = null;
            Object rawName = raw.get("name");
            if (rawName != null) name = String.valueOf(rawName);

            // lore (kann String ODER List<?> sein) -> als Object durchreichen
            Object lore = raw.get("lore");

            // hide-flags
            List<String> hide = new ArrayList<>();
            Object rawHide = raw.get("hide-flags");
            if (rawHide instanceof List<?> l) {
                for (Object o : l) {
                    if (o != null) hide.add(String.valueOf(o));
                }
            }

            // enchantments
            List<Map<String, Object>> ench = new ArrayList<>();
            Object rawEnch = raw.get("enchantments");
            if (rawEnch instanceof List<?> l) {
                for (Object o : l) {
                    if (o instanceof Map<?, ?> m) ench.add((Map<String, Object>) m);
                }
            }

            // attributes
            List<Map<String, Object>> attrs = new ArrayList<>();
            Object rawAttrs = raw.get("attributes");
            if (rawAttrs instanceof List<?> l) {
                for (Object o : l) {
                    if (o instanceof Map<?, ?> m) attrs.add((Map<String, Object>) m);
                }
            }

            // extras: kann Map ODER Liste ODER Skalar sein -> in Map<String,Object> wrappen
            Map<String, Object> extras;
            Object rawExtras = raw.get("extras");
            if (rawExtras instanceof Map<?, ?> m) {
                extras = (Map<String, Object>) m;
            } else if (rawExtras instanceof List<?> l) {
                extras = Map.of("list", l);
            } else if (rawExtras != null) {
                extras = Map.of("value", rawExtras);
            } else {
                extras = Map.of();
            }

            return new NexItemConfig(id, type, item, page, slots1b, usePage, amount, modelData, tooltip, name, lore, hide, ench, attrs, extras);
        } catch (Exception e) {
            // bewusst nicht schlucken ohne Spur – gib null zurück, aber vermeide Exceptions im Log-Spam
            return null;
        }
    }


    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castListOfMap(Object o) {
        if (o instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object e : list) {
                if (e instanceof Map<?, ?> m) out.add((Map<String, Object>) m);
            }
            return out;
        }
        return List.of();
    }
}