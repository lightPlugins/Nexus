package io.nexstudios.nexus.bukkit.placeholder.internal;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.levels.LevelProgress;
import io.nexstudios.nexus.bukkit.levels.LevelRewardConfig;
import io.nexstudios.nexus.bukkit.levels.LevelRewardRegistry;
import io.nexstudios.nexus.bukkit.levels.LevelService;
import io.nexstudios.nexus.bukkit.placeholder.NexPlaceholderIntrospector;
import io.nexstudios.nexus.bukkit.placeholder.NexPlaceholderProvider;
import io.nexstudios.nexus.bukkit.placeholder.PlaceholderValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Core placeholder provider for namespace "nexus".
 *
 * Supports:
 * <ul>
 *     <li><b>playername</b> – same as old NexusPlayerNamePlaceholder</li>
 *     <li><b>level_{namespace}_{key}_current_level</b></li>
 *     <li><b>level_{namespace}_{key}_current_exp</b></li>
 *     <li><b>level_{namespace}_{key}_required_exp</b></li>
 * </ul>
 *
 * Notes on key format:
 * <ul>
 *     <li>Nexus native syntax: {@code #nexus:level_demo_mining_current_level#}</li>
 *     <li>PlaceholderAPI syntax: {@code %nexus_level_demo_mining_current_level%}</li>
 *     <li>PAPI parameters are transformed by NexusPAPIBridge from underscores to colons,
 *         e.g. {@code level_demo_mining_current_level} -> {@code level:demo:mining:current:level}.
 *         This provider normalizes ':' back to '_' internally, so both forms work.</li>
 * </ul>
 */
public final class NexusCorePlaceholderProvider implements NexPlaceholderProvider, NexPlaceholderIntrospector {

    private static final String KEY_PLAYERNAME = "playername";

    @Override
    public @Nullable PlaceholderValue resolve(String key) {
        // Non-player context: we can only provide generic or cache-friendly fallbacks.
        String norm = normalize(key);

        if (KEY_PLAYERNAME.equals(norm)) {
            return PlaceholderValue.of("unknown", Component.text("unknown")).cacheable(true);
        }

        // Level placeholders without player context don't make much sense -> null.
        if (isLevelKey(norm)) {
            return null;
        }

        return null;
    }

    @Override
    public @Nullable PlaceholderValue resolve(Player player, String key) {
        String norm = normalize(key);

        if (KEY_PLAYERNAME.equals(norm)) {
            Component display = player.displayName();
            String plain = PlainTextComponentSerializer.plainText().serialize(display);
            return PlaceholderValue.of(plain, display).cacheable(true);
        }

        if (!isLevelKey(norm)) {
            return null;
        }

        return resolveLevelPlaceholder(player, norm);
    }

    @Override
    public @Nullable String fallback(@Nullable Player player, String key) {
        String norm = normalize(key);

        if (KEY_PLAYERNAME.equals(norm)) {
            return (player != null) ? player.getName() : "unknown";
        }

        return null;
    }

    @Override
    public boolean isCacheable(String key) {
        return true;
    }

    @Override
    public Set<String> advertisedKeys() {
        return Set.of(
                KEY_PLAYERNAME,
                "level_*_*_current_level",
                "level_*_*_current_exp",
                "level_*_*_required_exp"
        );
    }

    private String normalize(String rawKey) {
        if (rawKey == null) return "";
        // Lowercase + unify ':' and '_' to a single separator so we can accept both
        // "level_demo_mining_current_level" and "level:demo:mining:current:level".
        String k = rawKey.toLowerCase(Locale.ROOT).trim();
        k = k.replace(':', '_');
        return k;
    }

    private boolean isLevelKey(String normKey) {
        return normKey.startsWith("level_");
    }

    private @Nullable PlaceholderValue resolveLevelPlaceholder(Player player, String normKey) {
        // Expected pattern after normalize():
        // level_{namespace}_{key}_current_level
        // level_{namespace}_{key}_current_exp
        // level_{namespace}_{key}_required_exp

        String[] parts = normKey.split("_");
        if (parts.length < 5) {
            return null;
        }

        if (!"level".equals(parts[0])) return null;

        String namespace = parts[1];
        String levelKey = parts[2];

        // Suffix detection
        String suffix = String.join("_", Arrays.copyOfRange(parts, 3, parts.length));
        boolean isCurrentLevel = suffix.equals("current_level") || suffix.equals("currentlevel");
        boolean isCurrentExp   = suffix.equals("current_exp")   || suffix.equals("currentexp");
        boolean isRequiredExp  = suffix.equals("required_exp")  || suffix.equals("requiredexp");

        if (!isCurrentLevel && !isCurrentExp && !isRequiredExp) {
            return null;
        }

        LevelService levels = NexusPlugin.getInstance().getLevelService();
        if (levels == null) {
            return null;
        }

        UUID pid = player.getUniqueId();
        LevelProgress progress = levels.getProgress(pid, namespace, levelKey);
        int currentLevel = progress.getLevel();
        double currentXp = progress.getXp();

        if (isCurrentLevel) {
            return PlaceholderValue.ofString(String.valueOf(currentLevel)).cacheable(false);
        }

        if (isCurrentExp) {
            return PlaceholderValue.ofString(fmt(currentXp)).cacheable(false);
        }

        // required_exp:
        // Try to determine the XP required to reach (currentLevel + 1)
        // using LevelRewardRegistry (only available for registered level types).
        double requiredExp = resolveRequiredExp(namespace, levelKey, currentLevel);
        return PlaceholderValue.ofString(fmt(requiredExp)).cacheable(false);
    }

    private double resolveRequiredExp(String namespace, String levelKey, int currentLevel) {
        LevelRewardConfig cfg = LevelRewardRegistry.get(namespace, levelKey);
        if (cfg == null) {
            // No reward-config registered -> we don't know the XP curve.
            return 0.0d;
        }

        var list = cfg.requiredXpPerLevel();
        if (list.isEmpty()) return 0.0d;

        int nextLevel = currentLevel + 1;
        if (nextLevel <= 0) nextLevel = 1;

        if (nextLevel > list.size()) {
            // Already at or above max level -> return requirement of last level as fallback.
            return list.getLast();
        }

        return list.get(nextLevel - 1);
    }

    private String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}