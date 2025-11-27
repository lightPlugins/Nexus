package io.nexstudios.nexus.bukkit.dialog;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Core-level abstraction for dialog results.
 *
 * Implementations are provided by NMS/Paper modules and may do
 * additional parsing / mapping as needed.
 */
public interface NexDialogResult {

    /**
     * Low-level string access. Implementations should return the
     * raw string representation for the given key, or null.
     */
    String getString(String key);

    /**
     * Alias for getString(key) for legacy code that expects "text".
     */
    default String getText(String key) {
        return getString(key);
    }

    default int getInt(String key) {
        return getInt(key, 0);
    }

    default int getInt(String key, int def) {
        String s = getString(key);
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    default double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    default double getDouble(String key, double def) {
        String s = getString(key);
        if (s == null) return def;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    default boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    default boolean getBoolean(String key, boolean def) {
        String s = getString(key);
        if (s == null) return def;
        String v = s.trim().toLowerCase();
        if (v.equals("true") || v.equals("yes") || v.equals("y") || v.equals("1")) return true;
        if (v.equals("false") || v.equals("no") || v.equals("n") || v.equals("0")) return false;
        return def;
    }

    /**
     * Returns a list of strings for the given key.
     * Implementations may map a single string to a one-element list
     * or support more complex formats.
     */
    default List<String> getStringList(String key) {
        String s = getString(key);
        if (s == null || s.isBlank()) return List.of();
        String[] parts = s.split(",");
        if (parts.length == 0) return List.of();
        List<String> result = new java.util.ArrayList<>(parts.length);
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Optional: list of all known keys, may be empty.
     */
    default Set<String> keys() {
        return Set.of();
    }
}