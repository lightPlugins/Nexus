package io.nexstudios.nexus.bukkit.effects.filters.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.filters.NexusFilter;
import io.nexstudios.nexus.bukkit.effects.filters.PlayerContext;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class InventoryContainsFilter implements NexusFilter<PlayerContext> {

    private static final class Requirement {
        final ItemStack pattern; // Muster-Item (von StringUtils.parseItem)
        final Material material; // Nur genutzt, wenn matchMeta=false
        final int amount;

        Requirement(ItemStack pattern, int amount, boolean matchMeta) {
            this.pattern = pattern;
            this.amount = Math.max(1, amount);
            this.material = matchMeta ? null : pattern.getType();
        }
    }

    private final List<Requirement> requirements;
    private final boolean requireAll;
    private final boolean matchMeta;

    private InventoryContainsFilter(List<Requirement> requirements, boolean requireAll, boolean matchMeta) {
        this.requirements = List.copyOf(requirements);
        this.requireAll = requireAll;
        this.matchMeta = matchMeta;
    }

    @Override
    public boolean test(PlayerContext ctx) {
        Player p = ctx.player();
        if (p == null || requirements.isEmpty()) return false;

        ItemStack[] contents = p.getInventory().getContents();
        if (contents == null || contents.length == 0) return false;

        if (requireAll) {
            // AND: alle Anforderungen müssen erfüllt sein
            for (Requirement r : requirements) {
                if (countMatches(contents, r) < r.amount) return false;
            }
            return true;
        } else {
            // OR: mindestens eine Anforderung muss erfüllt sein
            for (Requirement r : requirements) {
                if (countMatches(contents, r) >= r.amount) return true;
            }
            return false;
        }
    }

    private int countMatches(ItemStack[] contents, Requirement r) {
        int total = 0;
        if (matchMeta) {
            for (ItemStack it : contents) {
                if (it == null || it.getType().isAir()) continue;
                if (it.isSimilar(r.pattern)) {
                    total += it.getAmount();
                    if (total >= r.amount) return total;
                }
            }
        } else {
            Material mat = r.material;
            if (mat == null || mat.isAir()) return 0;
            for (ItemStack it : contents) {
                if (it == null || it.getType().isAir()) continue;
                if (it.getType() == mat) {
                    total += it.getAmount();
                    if (total >= r.amount) return total;
                }
            }
        }
        return total;
    }

    public static InventoryContainsFilter fromConfig(Map<String, Object> cfg) {
        if (cfg == null) return null;

        boolean requireAll = getBool(cfg, "require-all", false);
        boolean matchMeta = getBool(cfg, "match-meta", true);

        List<Requirement> reqs = new ArrayList<>();

        Object raw = cfg.get("items");
        if (raw instanceof List<?> list) {
            for (Object entry : list) {
                parseEntry(entry, matchMeta).ifPresent(reqs::add);
            }
        } else if (raw != null) {
            parseEntry(raw, matchMeta).ifPresent(reqs::add);
        }

        if (reqs.isEmpty()) {
            NexusPlugin.nexusLogger.warning("inventory-contains: Keine gültigen Einträge in 'items' gefunden. Filter wird ignoriert.");
            return null;
        }

        return new InventoryContainsFilter(reqs, requireAll, matchMeta);
    }

    private static Optional<Requirement> parseEntry(Object entry, boolean matchMeta) {
        switch (entry) {
            case null -> {
                return Optional.empty();
            }
            case String s -> {
                String id = s.trim();
                if (id.isEmpty()) return Optional.empty();
                ItemStack pattern = StringUtils.parseItem(id);
                // parseItem liefert bei Fehler DEEPSLATE als Fallback -> als ungültig werten:
                if (pattern == null || pattern.getType().isAir() || pattern.getType() == Material.DEEPSLATE)
                    return Optional.empty();
                return Optional.of(new Requirement(pattern, 1, matchMeta));
            }
            case Map<?, ?> map -> {
                Map<String, Object> m = new HashMap<>();
                map.forEach((k, v) -> m.put(String.valueOf(k), v));

                String id = str(m.get("id"));
                if (id == null || id.isEmpty()) return Optional.empty();

                int amount = getInt(m);
                ItemStack pattern = StringUtils.parseItem(id);
                if (pattern == null || pattern.getType().isAir() || pattern.getType() == Material.DEEPSLATE)
                    return Optional.empty();

                return Optional.of(new Requirement(pattern, amount, matchMeta));
            }
            default -> {
            }
        }

        return Optional.empty();
    }

    private static boolean getBool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        if (v instanceof Boolean b) return b;
        if (v != null) {
            String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
            if ("true".equals(s)) return true;
            if ("false".equals(s)) return false;
        }
        return def;
    }

    private static int getInt(Map<String, Object> m) {
        Object v = m.get("amount");
        if (v instanceof Number n) return n.intValue();
        try {
            return v != null ? Integer.parseInt(String.valueOf(v)) : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
