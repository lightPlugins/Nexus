package io.nexstudios.nexus.bukkit.effects.filters.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.filters.NexusFilter;
import io.nexstudios.nexus.bukkit.effects.filters.PlayerContext;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MatchItemFilter implements NexusFilter<PlayerContext> {

    private final List<ItemStack> targets; // Liste von Ziel-Items; OR-Logik

    private MatchItemFilter(List<ItemStack> targets) {
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack is : targets) {
            if (is != null && !is.getType().isAir()) {
                valid.add(is);
            }
        }
        this.targets = List.copyOf(valid);
    }

    @Override
    public boolean test(PlayerContext ctx) {
        if (targets.isEmpty()) return false;
        Player p = ctx.player();
        if (p == null) return false;

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) return false;

        for (ItemStack target : targets) {
            if (hand.isSimilar(target)) {
                return true;
            }
        }
        return false;
    }

    public static MatchItemFilter fromConfig(Map<String, Object> cfg) {
        if (cfg == null) return null;

        List<ItemStack> parsedTargets = new ArrayList<>();

        Object rawItems = cfg.get("items");
        if (rawItems instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                String raw = String.valueOf(o).trim();
                if (raw.isEmpty()) continue;

                ItemStack parsed = StringUtils.parseItem(raw);
                if (parsed != null && !parsed.getType().isAir()) {
                    parsedTargets.add(parsed);
                } else {
                    NexusPlugin.nexusLogger.warning("match-item: Ungültiger Eintrag in items: " + raw);
                }
            }
        }

        if (parsedTargets.isEmpty() && cfg.containsKey("item")) {
            Object single = cfg.get("item");
            if (single != null) {
                String raw = String.valueOf(single).trim();
                if (!raw.isEmpty()) {
                    ItemStack parsed = StringUtils.parseItem(raw);
                    if (parsed != null && !parsed.getType().isAir()) {
                        parsedTargets.add(parsed);
                    } else {
                        NexusPlugin.nexusLogger.warning("match-item: Konnte Ziel-Item nicht erzeugen: " + raw);
                    }
                }
            }
        }

        if (parsedTargets.isEmpty()) return null;
        return new MatchItemFilter(parsedTargets);
    }
}



