package io.nexstudios.nexus.bukkit.levels;// Java

import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Level API Preview:
 * - This class shows how you would use the LevelService from another plugin.
 * - Call LevelApiPreview.demo(player) from a command/listener to see it in action.
 * - All comments are written to explain the intended usage pattern.
 */
public final class LevelApiPreview {

    private LevelApiPreview() {}

    /**
     * Minimal end-to-end preview:
     * - Registers a level type
     * - Adds and removes XP
     * - Sets level and XP
     * - Reads back current progress
     * - Flushes to the database (optional for tests)
     */
    private void demo(Player player) {
        // 1) Get the LevelService from the core
        LevelService levels = NexusPlugin.getInstance().getLevelService();
        if (levels == null) {
            player.sendMessage("LevelService not available. Is the core loaded?");
            return;
        }

        // 2) Namespace: typically your plugin name (lowercase)
        String ns = "demo"; // or: yourPlugin.getName().toLowerCase(Locale.ROOT)
        String key = "mining";
        UUID pid = player.getUniqueId();

        // 3) Register a level type (cap = size of the list)
        //    Requirement list means: Level 1 requires 5 XP, Level 2 requires 10 XP, and so on.
        levels.registerLevel(ns, key, List.of(5d, 10d, 20d, 50d, 100d, 200d, 300d, 500d, 1000d));

        // 4) Add XP (cache only; DB writes happen asynchronously)
        LevelProgress afterGain = levels.addXp(pid, ns, key, 42.5);

        // 5) Remove XP (can cause level down; never goes below level 0 / xp 0)
        LevelProgress afterRemove = levels.removeXp(pid, ns, key, 10.0);

        // 6) Set an absolute level (clamped to [0..cap], XP will be clamped accordingly)
        LevelProgress afterSetLevel = levels.setLevel(pid, ns, key, 3);

        // 7) Set absolute XP (overflow will promote levels as needed)
        LevelProgress afterSetXp = levels.setXp(pid, ns, key, 250.0);

        // 8) Read current progress (fast; from cache)
        LevelProgress current = levels.getProgress(pid, ns, key);

        // 9) Optional: force a DB flush for this player (useful in tests)
        levels.flushPlayer(pid);
    }

    /**
     * Preview for offline access:
     * - Demonstrates how you would read/manipulate a player by UUID (even if they are offline).
     * - Works because the core preloads all level progress entries on server start.
     */
    private void demoOffline(UUID playerId) {
        LevelService levels = NexusPlugin.getInstance().getLevelService();
        if (levels == null) {
            System.out.println("[LevelAPI] LevelService not available.");
            return;
        }

        String ns = "plugin-name";
        String key = "mining";

        // Read without the player being online
        LevelProgress p = levels.getProgress(playerId, ns, key);
        System.out.println("[LevelAPI-Offline] level=" + p.getLevel() + ", xp=" + fmt(p.getXp()));
    }

    // Small helper to format doubles
    private String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}