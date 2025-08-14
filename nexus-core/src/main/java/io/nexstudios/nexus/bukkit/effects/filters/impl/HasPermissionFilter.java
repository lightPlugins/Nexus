package io.nexstudios.nexus.bukkit.effects.filters.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.filters.NexusFilter;
import io.nexstudios.nexus.bukkit.effects.filters.PlayerContext;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HasPermissionFilter implements NexusFilter<PlayerContext> {

    private final List<String> permissions; // Liste der zu prüfenden Permissions
    private final boolean requireAll;       // true = AND-Logik, false = OR-Logik

    private HasPermissionFilter(List<String> permissions, boolean requireAll) {
        List<String> cleaned = new ArrayList<>();
        for (String p : permissions) {
            if (p == null) continue;
            String s = p.trim();
            if (!s.isEmpty()) cleaned.add(s);
        }
        this.permissions = List.copyOf(cleaned);
        this.requireAll = requireAll;
    }

    @Override
    public boolean test(PlayerContext ctx) {
        Player p = ctx.player();
            if (p == null) return false;
        if (permissions.isEmpty()) return false;

        if (requireAll) {
            for (String node : permissions) {
                if (!p.hasPermission(node)) return false;
            }
            return true;
        } else {
            for (String node : permissions) {
                if (p.hasPermission(node)) return true;
            }
            return false;
        }
    }

    public static HasPermissionFilter fromConfig(Map<String, Object> cfg) {
        if (cfg == null) return null;

        List<String> perms = new ArrayList<>();

            Object raw = cfg.get("permissions");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                String s = String.valueOf(o).trim();
                if (!s.isEmpty()) perms.add(s);
            }
        }

        if (perms.isEmpty() && cfg.containsKey("permission")) {
            Object one = cfg.get("permission");
            if (one != null) {
                String s = String.valueOf(one).trim();
                if (!s.isEmpty()) perms.add(s);
            }
        }

        boolean requireAll = false;
        if (cfg.containsKey("require-all")) {
            Object ra = cfg.get("require-all");
            if (ra instanceof Boolean b) requireAll = b;
            else if (ra != null) requireAll = Boolean.parseBoolean(String.valueOf(ra).toLowerCase(Locale.ROOT));
        }

        if (perms.isEmpty()) {
            NexusPlugin.nexusLogger.warning("has-permission: Keine gültigen Permissions gefunden. Filter wird ignoriert.");
            return null;
        }

        return new HasPermissionFilter(perms, requireAll);
    }
}


