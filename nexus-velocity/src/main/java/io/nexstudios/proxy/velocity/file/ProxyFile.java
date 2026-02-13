package io.nexstudios.proxy.velocity.file;

import io.nexstudios.proxy.velocity.util.VelocityLogger;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Reads a YAML config for Velocity without ever writing it back.
 * Default configs are copied byte-for-byte from resources on first start.
 */
@Getter
public final class ProxyFile {

    private final Path dataDirectory;
    private final String fileName;
    private final String resourceName;
    private final VelocityLogger logger;
    private final ClassLoader resourceLoader;

    private Path filePath;
    private ConfigurationNode root;

    public ProxyFile(Path dataDirectory,
                     String fileName,
                     VelocityLogger logger,
                     ClassLoader resourceLoader) {
        this(dataDirectory, fileName, fileName, logger, resourceLoader);
    }

    public ProxyFile(Path dataDirectory,
                     String fileName,
                     String resourceName,
                     VelocityLogger logger,
                     ClassLoader resourceLoader) {
        this.dataDirectory = java.util.Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.fileName = java.util.Objects.requireNonNull(fileName, "fileName");
        this.resourceName = java.util.Objects.requireNonNull(resourceName, "resourceName");
        this.logger = java.util.Objects.requireNonNull(logger, "logger");
        this.resourceLoader = (resourceLoader != null) ? resourceLoader : ProxyFile.class.getClassLoader();

        ensureDefaultExists();
        reload();
    }

    public void reload() {
        this.filePath = dataDirectory.resolve(fileName);

        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create config directories: " + filePath.getParent(), e);
        }

        this.root = loadYaml(filePath);
    }

    public boolean exists() {
        return Files.exists(dataDirectory.resolve(fileName));
    }

    public @Nullable String getString(String path) {
        return node(path).getString();
    }

    public String getString(String path, String def) {
        String v = node(path).getString();
        return (v == null || v.isEmpty()) ? def : v;
    }

    public int getInt(String path, int def) {
        ConfigurationNode n = node(path);
        if (n.virtual()) return def;
        return n.getInt(def);
    }

    public boolean getBoolean(String path, boolean def) {
        ConfigurationNode n = node(path);
        if (n.virtual()) return def;
        return n.getBoolean(def);
    }

    public double getDouble(String path, double def) {
        ConfigurationNode n = node(path);
        if (n.virtual()) return def;
        return n.getDouble(def);
    }

    public java.util.List<String> getStringList(String path) {
        try {
            java.util.List<String> out = node(path).getList(String.class);
            return out == null ? java.util.List.of() : out;
        } catch (org.spongepowered.configurate.serialize.SerializationException e) {
            return java.util.List.of();
        }
    }

    public @Nullable ConfigurationNode getSection(String path) {
        ConfigurationNode n = node(path);
        return n.virtual() ? null : n;
    }

    // ---------------- internals ----------------

    private void ensureDefaultExists() {
        this.filePath = dataDirectory.resolve(fileName);

        try {
            Files.createDirectories(filePath.getParent());

            if (Files.exists(filePath)) {
                return;
            }

            try (InputStream in = resourceLoader.getResourceAsStream(resourceName)) {
                if (in == null) {
                    Files.createFile(filePath);
                    logger.warning("Default config resource not found: " + resourceName + " (created empty file: " + fileName + ")");
                    return;
                }

                // Byte-for-byte copy to preserve comments and formatting
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create default config: " + filePath, e);
        }
    }

    private static ConfigurationNode loadYaml(Path path) {
        try {
            return YamlConfigurationLoader.builder()
                    .path(path)
                    .build()
                    .load();
        } catch (IOException e) {
            throw new RuntimeException("Could not load yaml: " + path, e);
        }
    }

    private ConfigurationNode node(String path) {
        if (root == null) reload();
        if (path == null || path.isBlank()) return root;

        ConfigurationNode n = root;
        for (String p : path.split("\\.")) {
            if (p.isEmpty()) continue;
            n = n.node(p);
        }
        return n;
    }

    /**
     * Read-only helper to get the raw file content as UTF-8 (useful for debugging).
     */
    public @Nullable String readRawText() {
        Path p = dataDirectory.resolve(fileName);
        if (!Files.exists(p)) return null;
        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }
}