package io.nexstudios.nexus.bukkit.placeholder.internal;

import io.nexstudios.nexus.bukkit.placeholder.PlaceholderProvider;
import io.nexstudios.nexus.bukkit.placeholder.PlaceholderValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Example provider for namespace "nexus".
 * Key:
 * - "playername": resolves to the player's display name component if player is present,
 *                 otherwise falls back to the offline name if available, else "unknown".
 *
 * Notes:
 * - Uses Adventure display name for components.
 * - Provides both component and string values.
 * - Marked as cacheable with a short default TTL to be configured on registration.
 * - Comments are in English.
 */
public final class NexusPlayerNamePlaceholder implements PlaceholderProvider {

    private final String KEY_PLAYERNAME = "playername";

    @Override
    public @Nullable PlaceholderValue resolve(String key) {
        // No player context: we can only provide a generic fallback.
        if (KEY_PLAYERNAME.equals(key)) {
            // Without a player, return a sensible generic value.
            return PlaceholderValue.of("unknown", Component.text("unknown")).cacheable(true);
        }
        return null;
    }

    @Override
    public @Nullable PlaceholderValue resolve(Player player, String key) {
        if (!KEY_PLAYERNAME.equals(key)) return null;

        // Use Adventure display name component if available.
        Component display = player.displayName();
        String plain = PlainTextComponentSerializer.plainText().serialize(display);
        return PlaceholderValue.of(plain, display).cacheable(true);
    }

    @Override
    public @Nullable String fallback(@Nullable Player player, String key) {
        if (!KEY_PLAYERNAME.equals(key)) return null;
        if (player != null) {
            return player.getName();
        }
        return "unknown";
    }

    @Override
    public boolean isCacheable(String key) {
        // Allow caching for playername
        return KEY_PLAYERNAME.equals(key);
    }
}