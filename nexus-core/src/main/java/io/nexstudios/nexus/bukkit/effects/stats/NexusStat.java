package io.nexstudios.nexus.bukkit.effects.stats;

import java.io.File;

public interface NexusStat {
    String id();          // z. B. "mining"
    String namespace();   // z. B. "deinplugin"
    String keyLevel();    // z. B. "stats:mining:level"
    File sourceFile();    // Datei, aus der der Stat geladen wurde (kann null sein)
}

