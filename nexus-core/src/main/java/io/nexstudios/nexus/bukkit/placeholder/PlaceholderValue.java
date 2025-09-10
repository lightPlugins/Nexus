package io.nexstudios.nexus.bukkit.placeholder;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result container for a resolved placeholder.
 * <p>- stringValue: used by string resolvers.
 * <p>- componentValue: used by component resolvers.
 * <p>If both are present, component resolvers prefer componentValue and string resolvers prefer stringValue.
 * <p>
 * Caching hints:
 * <p>- cacheable: whether this key/value may be cached (provider-level control).
 * <p>- ttlMillisOverride: optional per-result TTL; if null, registry policy will be used.
 * <p>
 * Notes:
 * - This class is immutable and thread-safe.
 */
public record PlaceholderValue(@Nullable String stringValue, @Nullable Component componentValue, boolean cacheable,
                               @Nullable Long ttlMillisOverride) {

    public static PlaceholderValue ofString(String value) {
        return new PlaceholderValue(value, null, true, null);
    }

    public static PlaceholderValue ofComponent(Component value) {
        return new PlaceholderValue(null, value, true, null);
    }

    public static PlaceholderValue of(String stringValue, Component componentValue) {
        return new PlaceholderValue(stringValue, componentValue, true, null);
    }

    public PlaceholderValue cacheable(boolean cacheable) {
        return new PlaceholderValue(this.stringValue, this.componentValue, cacheable, this.ttlMillisOverride);
    }

    public PlaceholderValue ttlOverride(Long ttlMillis) {
        return new PlaceholderValue(this.stringValue, this.componentValue, this.cacheable, ttlMillis);
    }

    @NotNull
    @Override
    public String toString() {
        return "PlaceholderValue{string=" + (stringValue == null ? "null" : '"' + stringValue + '"') +
                ", component=" + (componentValue == null ? "null" : componentValue.examinableName()) +
                ", cacheable=" + cacheable +
                ", ttlOverride=" + ttlMillisOverride +
                '}';
    }
}