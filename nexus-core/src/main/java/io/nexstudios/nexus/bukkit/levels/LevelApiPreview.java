package io.nexstudios.nexus.bukkit.levels;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/**
 * Preview / example class for using the Nexus Level API from another plugin.
 * <p>
 * This class is <b>not</b> part of the public API; it is only meant as
 * documentation and reference code for plugin developers.
 * <p>
 * It demonstrates:
 * <ul>
 *     <li>How to obtain the {@link LevelService} from the Nexus core</li>
 *     <li>How to read a "level definition" with {@code levels} and
 *         {@code level-up-actions} from a {@link ConfigurationSection}</li>
 *     <li>How to build a {@link LevelRewardConfig} from that section</li>
 *     <li>How to register that config in the central {@link LevelRewardRegistry}</li>
 *     <li>How to register the level-type in the {@link LevelService}</li>
 *     <li>How XP operations automatically trigger level-up actions via
 *         the central {@code LevelActionListener} + {@code ActionFactory}</li>
 *     <li>How to explicitly persist a single player's level data to the database
 *         using {@link LevelService#safeToDatabase(UUID)}</li>
 *     <li>How to query required XP per level using
 *         {@link LevelService#getRequiredXp(String, String, int)}</li>
 * </ul>
 *
 *
 */
public final class LevelApiPreview {

    private LevelApiPreview() {}

    // ... existing javadoc ...

    private void demo(Player player, ConfigurationSection levelRoot) {
        // 1) Obtain LevelService from Nexus core
        LevelService levels = NexusPlugin.getInstance().getLevelService();
        if (levels == null) {
            player.sendMessage("LevelService not available. Is the core loaded?");
            return;
        }

        // 2) Define namespace and key for this level-type.
        //    In a real plugin, 'ns' should be your plugin name (lowercase),
        //    and 'key' a stable identifier for the skill/type.
        String ns = "demo";
        String key = "mining";
        UUID pid = player.getUniqueId();

        // 3) Build a LevelRewardConfig from the provided configuration section.
        //    This will:
        //    - Read the XP requirements from "levels"
        //    - Read level-up actions from "level-up-actions"
        LevelRewardConfig rewardConfig = LevelRewardConfig.fromStandardSection(levelRoot);

        // 4) Register the reward-config in the central registry.
        //    From this point on, the central LevelActionListener will:
        //    - Listen to NexLevelUpEvent
        //    - Look up the LevelRewardConfig for (ns, key)
        //    - Execute the configured actions via the ActionFactory / NexusAction system.
        LevelRewardRegistry.register(ns, key, rewardConfig);

        // 5) Register the level-type in the LevelService (XP requirements -> level cap).
        levels.registerLevel(ns, key, rewardConfig.requiredXpPerLevel());

        // 6) Demonstrate XP gain.
        //    This will:
        //    - Update the player's XP/level in the in-memory cache
        //    - Potentially fire NexLevelUpEvent
        //    - Trigger configured level-up actions for any reached levels.
        LevelProgress afterGain = levels.addXp(pid, ns, key, 42.5);
        player.sendMessage("After gain: level=" + afterGain.getLevel()
                + ", xpInLevel=" + fmt(afterGain.getXp())
                + ", totalXp=" + fmt(afterGain.getTotalXp()));

        // 6a) Show how to query required XP for current and next level.
        int currentLevel = afterGain.getLevel();
        double reqCurrent = levels.getRequiredXp(ns, key, Math.max(1, currentLevel == 0 ? 1 : currentLevel));
        player.sendMessage("Required XP for current level " + currentLevel + ": " + fmt(reqCurrent));

        if (currentLevel > 0) {
            int nextLevel = currentLevel + 1;
            double reqNext = levels.getRequiredXp(ns, key, nextLevel);
            player.sendMessage("Required XP for level " + nextLevel + ": " + fmt(reqNext));
        }

        // 7) Demonstrate setting an absolute level.
        //    This may again cause level-up events (and level-down if you lower it),
        //    which will also be handled by the central listener.
        LevelProgress afterSetLevel = levels.setLevel(pid, ns, key, 3);
        player.sendMessage("After setLevel(3): level=" + afterSetLevel.getLevel()
                + ", xpInLevel=" + fmt(afterSetLevel.getXp())
                + ", totalXp=" + fmt(afterSetLevel.getTotalXp()));

        // 7a) Read back current progress explicitly via getProgress.
        LevelProgress progressNow = levels.getProgress(pid, ns, key);
        player.sendMessage("Current progress: level=" + progressNow.getLevel()
                + ", xpInLevel=" + fmt(progressNow.getXp())
                + ", totalXp=" + fmt(progressNow.getTotalXp()));

        double reqForCurrent = levels.getRequiredXp(ns, key, Math.max(1, progressNow.getLevel() == 0 ? 1 : progressNow.getLevel()));
        player.sendMessage("Current level requires " + fmt(reqForCurrent) + " XP (per level definition).");

        // 8) Explicitly persist this player's level entries to the database.
        //    Normally you can rely on the periodic/batch flush;
        //    calling safeToDatabase is mainly useful for tests or "save now" commands.
        levels.safeToDatabase(pid);
    }

    /**
     * Small helper to format XP values with a fixed locale.
     *
     * @param v numeric value to format
     * @return string formatted as "%.2f" using {@link Locale#ROOT}
     */
    private String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}