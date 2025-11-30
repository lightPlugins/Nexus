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
 *     <li>How to register the level-type itself in the {@link LevelService}</li>
 *     <li>How XP operations automatically trigger level-up actions via
 *         the central {@code LevelActionListener} + {@code ActionFactory}</li>
 *     <li>How to explicitly persist a single player's level data to the database
 *         using {@link LevelService#safeToDatabase(UUID)}</li>
 * </ul>
 *
 * <h2>Expected configuration format</h2>
 *
 * The provided {@code levelRoot} section is expected to contain the following keys:
 *
 * <pre>{@code
 * levels:
 *   - 5
 *   - 10
 *   - 20
 *
 * level-up-actions:
 *   - level: 1
 *     actions:
 *       - id: message
 *         message: "<green>Level 1 reached!</green>"
 *   - level: 2
 *     actions:
 *       - id: vault-add
 *         amount: "2000"
 *         multiplier: "1"
 * }</pre>
 *
 * This is exactly the "standard" schema that {@link LevelRewardConfig#fromStandardSection(ConfigurationSection)}
 * expects:
 * <ul>
 *     <li>{@code levels} – list of XP requirements per level (index 0 = level 1, index 1 = level 2, ...)</li>
 *     <li>{@code level-up-actions} – list of entries:
 *         <ul>
 *             <li>{@code level}: target level (int)</li>
 *             <li>{@code actions}: list of action-maps consumed by {@link io.nexstudios.nexus.bukkit.actions.ActionFactory}</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <h2>How to use this preview</h2>
 *
 * In your own plugin, you would typically:
 * <ol>
 *     <li>Load your own config file into a {@link org.bukkit.configuration.file.FileConfiguration}</li>
 *     <li>Obtain a sub-section for your level-type, e.g. {@code config.getConfigurationSection("mining")}</li>
 *     <li>Call {@code LevelApiPreview.demo(player, thatSection)} from a debug command or test listener</li>
 * </ol>
 */
public final class LevelApiPreview {

    private LevelApiPreview() {}

    /**
     * End-to-end demonstration for a single level-type.
     * <p>
     * Steps:
     * <ol>
     *     <li>Resolve the central {@link LevelService} from Nexus core</li>
     *     <li>Build a {@link LevelRewardConfig} from the given config section</li>
     *     <li>Register the reward-config in {@link LevelRewardRegistry}</li>
     *     <li>Register the level-type in the {@link LevelService}</li>
     *     <li>Perform some XP operations (add XP, set level)</li>
     *     <li>Explicitly flush this player's changes to the database</li>
     * </ol>
     *
     * @param player    the online player used for demonstration
     * @param levelRoot configuration section for this level-type, containing
     *                  {@code levels} and {@code level-up-actions} as described
     *                  in the class-level JavaDoc.
     */
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
        player.sendMessage("After gain: level=" + afterGain.getLevel() + ", xp=" + fmt(afterGain.getXp()));

        // 7) Demonstrate setting an absolute level.
        //    This may again cause level-up events (and level-down if you lower it),
        //    which will also be handled by the central listener.
        LevelProgress afterSetLevel = levels.setLevel(pid, ns, key, 3);
        player.sendMessage("After setLevel(3): level=" + afterSetLevel.getLevel() + ", xp=" + fmt(afterSetLevel.getXp()));

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