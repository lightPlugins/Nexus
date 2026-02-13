package io.nexstudios.proxy.velocity.file;

import lombok.Getter;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Reads YAML files from a subdirectory inside the plugin data directory.
 * This reader is read-only (it does not generate or write files).
 */
@Getter
public final class ProxyFileReader {

    private final Path directory;
    private final List<Path> files = new ArrayList<>();

    private final List<ConfigurationNode> configs = new ArrayList<>();
    private final Map<String, ConfigurationNode> configMap = new HashMap<>();

    public ProxyFileReader(Path dataDirectory, String directoryPath) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(directoryPath, "directoryPath");

        this.directory = dataDirectory.resolve(directoryPath);
        reload();
    }

    public void reload() {
        this.files.clear();
        this.configs.clear();
        this.configMap.clear();

        loadYmlFiles();
        readConfigs();
    }

    private void readConfigs() {
        for (Path file : files) {
            ConfigurationNode cfg = loadNode(file);
            configs.add(cfg);
            configMap.put(getFileNameWithoutExtension(file.getFileName().toString()), cfg);
        }
    }

    private void loadYmlFiles() {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Files.walkFileTree(directory, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!file.toString().endsWith(".yml")) return FileVisitResult.CONTINUE;

                    String fileName = file.getFileName().toString();
                    if (fileName.equalsIgnoreCase("_example.yml")) return FileVisitResult.CONTINUE;

                    files.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Something went wrong in ProxyFileReader", e);
        }
    }

    private static ConfigurationNode loadNode(Path path) {
        try {
            return YamlConfigurationLoader.builder()
                    .path(path)
                    .build()
                    .load();
        } catch (IOException e) {
            throw new RuntimeException("Could not load yaml: " + path, e);
        }
    }

    public static String getFileNameWithoutExtension(String fileName) {
        int pos = fileName.lastIndexOf('.');
        return (pos > 0) ? fileName.substring(0, pos) : fileName;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> castListOfMap(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object element : list) {
                if (element instanceof Map<?, ?> map) {
                    out.add((Map<String, Object>) map);
                }
            }
            return out;
        }
        return List.of();
    }

    public static List<Map<String, Object>> getMapList(ConfigurationNode config, String path) {
        if (config == null || path == null || path.isBlank()) return List.of();

        ConfigurationNode node = config;
        for (String p : path.split("\\.")) {
            if (p.isEmpty()) continue;
            node = node.node(p);
        }

        Object raw = node.raw();
        return castListOfMap(raw);
    }
}