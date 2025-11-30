package io.nexstudios.nexus.bukkit.actions;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Typsichere, immutable Parameter-Collection für Actions.
 * Key und Value sind immer Strings.
 *
 * Zusätzlich kann optional ein TagResolver mitgegeben werden,
 * z. B. für MiniMessage-Placeholders.
 */
public final class NexParams {

    private static final NexParams EMPTY = new NexParams(Collections.emptyMap(), TagResolver.empty());

    private final Map<String, String> values;
    private final TagResolver tagResolver;

    private NexParams(Map<String, String> values, TagResolver tagResolver) {
        this.values = Collections.unmodifiableMap(values);
        this.tagResolver = tagResolver == null ? TagResolver.empty() : tagResolver;
    }

    public static NexParams empty() {
        return EMPTY;
    }

    public static NexParams of(@NotNull Map<String, String> source) {
        if (source.isEmpty()) return EMPTY;
        return new NexParams(new HashMap<>(source), TagResolver.empty());
    }

    public static NexParams of(@NotNull Map<String, String> source,
                               @Nullable TagResolver tagResolver) {
        if (source.isEmpty() && (tagResolver == null || tagResolver == TagResolver.empty())) {
            return EMPTY;
        }
        return new NexParams(new HashMap<>(source), tagResolver);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean contains(@NotNull String key) {
        return values.containsKey(key);
    }

    @Nullable
    public String get(@NotNull String key) {
        return values.get(key);
    }

    @NotNull
    public String getOrDefault(@NotNull String key, @NotNull String def) {
        return values.getOrDefault(key, def);
    }

    public Map<String, String> asMap() {
        return values;
    }

    /**
     * Liefert den zugehörigen TagResolver (niemals null, mindestens TagResolver.empty()).
     */
    public TagResolver tagResolver() {
        return tagResolver;
    }

    /**
     * Erzeugt eine Kopie mit zusätzlichem/überschriebenem Key/Value.
     * Der TagResolver bleibt unverändert.
     */
    public NexParams with(@NotNull String key, @NotNull String value) {
        Map<String, String> copy = new HashMap<>(values);
        copy.put(key, value);
        return new NexParams(copy, tagResolver);
    }

    /**
     * Erzeugt eine Kopie mit einem (neuen) TagResolver.
     * Die bestehenden Key/Value-Parameter bleiben erhalten.
     */
    public NexParams withTagResolver(@Nullable TagResolver resolver) {
        return new NexParams(values, resolver == null ? TagResolver.empty() : resolver);
    }

    /**
     * Merged zwei NexParams:
     * - Keys von other überschreiben ggf. vorhandene Keys.
     * - TagResolver wird zu TagResolver.resolver(this.tagResolver, other.tagResolver) gemerged.
     */
    public NexParams merge(@NotNull NexParams other) {
        if (other.isEmpty() && other.tagResolver == TagResolver.empty()) return this;
        if (this.isEmpty() && this.tagResolver == TagResolver.empty()) return other;

        Map<String, String> copy = new HashMap<>(values);
        copy.putAll(other.values);

        TagResolver mergedResolver = TagResolver.resolver(this.tagResolver, other.tagResolver);
        return new NexParams(copy, mergedResolver);
    }

    public static final class Builder {
        private final Map<String, String> values = new HashMap<>();
        private TagResolver tagResolver = TagResolver.empty();

        public Builder put(@NotNull String key, @NotNull String value) {
            values.put(key, value);
            return this;
        }

        public Builder putAll(@NotNull Map<String, String> map) {
            values.putAll(map);
            return this;
        }

        public Builder putAll(@NotNull NexParams params) {
            values.putAll(params.values);
            // TagResolver nicht automatisch übernehmen – das wäre sonst leicht überraschend.
            return this;
        }

        /**
         * Setzt den TagResolver für diese Params.
         */
        public Builder tagResolver(@Nullable TagResolver resolver) {
            this.tagResolver = resolver == null ? TagResolver.empty() : resolver;
            return this;
        }

        public NexParams build() {
            return of(values, tagResolver);
        }
    }
}