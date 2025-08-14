package io.nexstudios.nexus.bukkit.effects.util;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Collection;

public final class EntityMatcher {
    private EntityMatcher(){}

    public static boolean whitelistMatches(Entity target, Collection<String> namespacedTypes) {
        if (namespacedTypes == null || namespacedTypes.isEmpty()) return false;
        for (String s : namespacedTypes) {
            if (matches(target, s)) return true;
        }
        return false;
    }

    public static boolean matches(Entity target, String namespacedType) {
        if (namespacedType == null) return false;
        String[] parts = namespacedType.split(":", 2);
        if (parts.length != 2) return false;

        String ns = parts[0].toLowerCase();
        String value = parts[1];

        return switch (ns) {
            case "minecraft" -> matchesMinecraft(target, value);
            case "mythicmobs" -> matchesMythicMobs(target, value);
            default -> false;
        };
    }

    private static boolean matchesMinecraft(Entity target, String id) {
        try {
            EntityType type = EntityType.valueOf(id.toUpperCase());
            return target.getType() == type;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean matchesMythicMobs(Entity target, String id) {
        if(NexusPlugin.getInstance().getMythicMobsHook() == null) {
            return false;
        }
        return NexusPlugin.getInstance().getMythicMobsHook().isMythicMob(target, id);
    }
}

