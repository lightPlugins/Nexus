package io.nexstudios.nexus.bukkit.effects.stats;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

public final class DefaultNexusStat implements NexusStat {
    private final String id;
    private final String namespace;
    private final String keyLevel;
    private final File sourceFile;

    public DefaultNexusStat(String namespace, String id, File sourceFile) {
        this.id = Objects.requireNonNull(id, "id").toLowerCase(Locale.ROOT).trim();
        this.namespace = Objects.requireNonNull(namespace, "namespace").toLowerCase(Locale.ROOT).trim();
        this.keyLevel = "stats:" + this.id + ":level";
        this.sourceFile = sourceFile;
    }

    @Override public String id() { return id; }
    @Override public String namespace() { return namespace; }
    @Override public String keyLevel() { return keyLevel; }
    @Override public File sourceFile() { return sourceFile; }

    @Override
    public String toString() {
        return "NexusStat{" + namespace + ":" + id + "}";
    }
}
