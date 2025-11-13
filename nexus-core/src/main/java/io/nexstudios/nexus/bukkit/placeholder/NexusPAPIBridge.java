package io.nexstudios.nexus.bukkit.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Bridge between NexusPlaceholders and PlaceholderAPI.
 *
 * Automatically exposes all registered NexusPlaceholder providers to PlaceholderAPI.
 *
 * Format mapping:
 * - NexusPlaceholder: #namespace:key#
 * - PlaceholderAPI:   %namespace_key%
 *
 * Example:
 * - #nexus:playername# becomes %nexus_playername%
 * - #myplugin:health# becomes %myplugin_health%
 *
 * Notes:
 * - Colons (:) in keys are automatically converted to underscores (_) for PAPI
 * - Only works for namespaces registered in NexusPlaceholderRegistry
 * - Only registers if provider.papiSupport() returns true
 * - Thread-safe
 */
public final class NexusPAPIBridge extends PlaceholderExpansion {

    private final String namespace;
    private final String version;
    private final String author;

    public NexusPAPIBridge(String namespace, String version, String author) {
        this.namespace = namespace.toLowerCase(Locale.ROOT);
        this.version = version;
        this.author = author;
    }

    @Override
    public @NotNull String getIdentifier() {
        return namespace;
    }

    @Override
    public @NotNull String getAuthor() {
        return author;
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        // Keep this expansion loaded even if the plugin reloads
        return true;
    }

    @Override
    public boolean canRegister() {
        // Check if the namespace exists in NexusPlaceholderRegistry
        var reg = NexusPlaceholderRegistry.getRegistration(namespace);
        return reg.map(registration -> registration.provider().papiSupport()).orElse(false);

        // Check if provider supports PAPI
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        // Convert PAPI format to Nexus format
        // %namespace_some_key% -> key = "some_key" -> resolve as "some:key" if needed
        String key = params.replace("_", ":").toLowerCase(Locale.ROOT);

        Player player = offlinePlayer.isOnline() ? offlinePlayer.getPlayer() : null;

        // Try to resolve through NexusPlaceholders
        var registration = NexusPlaceholderRegistry.getRegistration(namespace);
        if (registration.isEmpty()) {
            return null;
        }

        var provider = registration.get().provider();

        PlaceholderValue value;
        if (player != null) {
            value = provider.resolve(player, key);
        } else {
            value = provider.resolve(key);
        }

        if (value == null) {
            // Try fallback
            return provider.fallback(player, key);
        }

        // Return string value if available
        if (value.stringValue() != null) {
            return value.stringValue();
        }

        // Convert component to string if only component is available
        if (value.componentValue() != null) {
            return PlainTextComponentSerializer.plainText().serialize(value.componentValue());
        }

        return null;
    }
}