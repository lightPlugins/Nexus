package io.nexstudios.nexus.bukkit.items;

import java.util.Locale;

/**
 * Flags, die in der versionsspezifischen Implementierung auf Data Components gemappt werden.
 * z. B.:
 * - HIDE_ENCHANTS    -> Enchantments im Tooltip nicht anzeigen
 * - HIDE_ATTRIBUTES  -> Attribute im Tooltip nicht anzeigen
 * - HIDE_ADDITIONAL  -> Zusätzlichen Tooltip verbergen (falls verfügbar)
 * - HIDE_UNBREAKABLE -> Unbreakable nicht anzeigen
 */
public enum ItemHideFlag {
    HIDE_ENCHANTS,
    HIDE_ATTRIBUTES,
    HIDE_ADDITIONAL,
    HIDE_UNBREAKABLE;

    public static ItemHideFlag fromString(String s) {
        if (s == null) return null;
        String k = s.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (k) {
            case "hide_enchants" -> HIDE_ENCHANTS;
            case "hide_attributes" -> HIDE_ATTRIBUTES;
            case "hide_additional", "hide_additional_tooltip" -> HIDE_ADDITIONAL;
            case "hide_unbreakable" -> HIDE_UNBREAKABLE;
            default -> null;
        };
    }
}