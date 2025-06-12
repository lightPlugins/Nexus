package io.nexstudios.nexus.common.logging.files;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Getter
public class NexusFileReader {

    private final String directoryPath;
    private List<File> files;
    private final List<FileConfiguration> bukkitFiles;
    // Optional: Map to store FileConfiguration by file name without extension
    private final Map<String, FileConfiguration> bukkitFileMap;
    private final JavaPlugin plugin;

    /**
     * Constructor for NexusFileReader
     * <p>IMPORTANT: This Reader will never generate a file, it will only read files from the specified directory.
     * @param directoryPath the path to the directory containing the yml files (folders inside the plugin folder)
     * @param plugin the JavaPlugin instance
     */
    public NexusFileReader(String directoryPath, JavaPlugin plugin) {
        this.plugin = plugin;
        this.directoryPath = "plugins/" + plugin.getName() + "/" +directoryPath;
        this.bukkitFileMap = new HashMap<>();
        this.bukkitFiles = new ArrayList<>();
        loadYmlFiles();
        if(files != null && !files.isEmpty()) {
            readBukkitFiles();
        }
    }

    private void readBukkitFiles() {

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            bukkitFiles.add(config);
            bukkitFileMap.put(getFileNameWithoutExtension(file), config);
        }
    }

    /**
     * Load all yml files found in the specified directory
     * Save all found files in the "List<File> yamlFiles" field
     */
    private void loadYmlFiles() {
        files = new ArrayList<>();
        Path directory = Paths.get(directoryPath);
        try  {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
            Files.walkFileTree(directory, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".yml")) {
                        String fileName = file.getFileName().toString();
                        // Exclude the example file with the name "_example.yml"
                        if (!fileName.equalsIgnoreCase("_example.yml")) {
                            files.add(file.toFile());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Something went wrong in Nexus File Reader", e);
        }
    }

    /**
     * Reload all yml files found in the specified directory
     */
    public void reload() {
        loadYmlFiles();
    }

    /**
     * Get the file name without the extension (any extension)
     */
    public String getFileNameWithoutExtension(File file) {
        String fileName = file.getName();
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }

}
