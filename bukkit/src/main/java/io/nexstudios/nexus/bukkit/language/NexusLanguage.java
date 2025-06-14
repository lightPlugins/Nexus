package io.nexstudios.nexus.bukkit.language;

import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.common.logging.NexusLogger;
import io.nexstudios.nexus.common.files.NexusFileReader;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

@Getter
public class NexusLanguage {

    private final Map<UUID, String> userLanguage = new HashMap<>();
    private final Map<String, File> availableLanguages = new HashMap<>();
    private final Map<String, FileConfiguration> loadedLanguages = new HashMap<>();
    private final NexusFileReader rawLanguageFiles;
    // The UUID of the console, used for logging and default language selection
    public UUID consoleUUID = UUID.fromString("c860b2fe-cdd1-4e76-9ce5-874a83c44bc7");

    public NexusLanguage(NexusFileReader rawLanguageFiles, NexusLogger nexusLogger) {
        this.rawLanguageFiles = rawLanguageFiles;

        rawLanguageFiles.getFiles().forEach(file -> {
            String languageName = file.getName().replace(".yml", "");
            availableLanguages.put(languageName, file);
            loadedLanguages.put(languageName, YamlConfiguration.loadConfiguration(file));
        });

        nexusLogger.info("Loaded " + availableLanguages.size() + " languages.");
    }

    public String getSelectedLanguage(UUID uuid) {
        return userLanguage.getOrDefault(uuid, "english");
    }

    public String getPrefix(UUID uuid) {
        String lang = getSelectedLanguage(uuid);
        FileConfiguration languageConfig = loadedLanguages.get(lang);

        if (languageConfig == null) {
            Nexus.nexusLogger.error(List.of(
                    "Language configuration for " + lang + " not found.",
                    "Using default language: english"
            ));
            languageConfig = loadedLanguages.get("english");
        }

        if (languageConfig != null && languageConfig.contains("general.prefix")) {
            return languageConfig.getString("general.prefix", "NotFound");
        }

        return "<red>Null";
    }

    public Component getTranslation(UUID uuid, String path) {
        String lang = getSelectedLanguage(uuid);

        if (lang == null) {
            Nexus.nexusLogger.debug(List.of(
                    "Language for UUID " + uuid + " not found.",
                    "Using default language: english"
            ), 3);
            selectLanguage(uuid, "english");
            lang = "english";
        }

        FileConfiguration languageConfig = loadedLanguages.get(lang);

        if (languageConfig == null) {
            Nexus.nexusLogger.error(List.of(
                    "Language configuration for " + lang + " not found.",
                    "Using default language: english"
            ));
            languageConfig = loadedLanguages.get("english");

            if (languageConfig == null) {
                Nexus.nexusLogger.error(List.of(
                        "Default language configuration english.yml not found.",
                        "This is a critical problem and should be reported",
                        "to the developer: NexStudios"
                ));
                throw new IllegalArgumentException("english.yml -> language/configuration not found");
            }
        }

        if (languageConfig.contains(path)) {
            String translation = languageConfig.getString(path);
            if (translation == null) {
                Nexus.nexusLogger.error(List.of(
                        "Translation for path '" + path + "' in language '" + lang + "' is null.",
                        "Using default translation: english.yml"
                ));
                return Component.text(path);
            }
            return MiniMessage.miniMessage().deserialize(getPrefix(uuid) + " " + translation);
        }

        return Component.text(getPrefix(uuid) + " " + path);
    }

    public void selectLanguage(UUID uuid, String language) {
        if(!availableLanguages.containsKey(language)) {
            if(language.equals("english")) {
                Nexus.nexusLogger.error(List.of(
                        "Could not find the default english file.",
                        "This is a critical problem and should be reported",
                        "to the developer: NexStudios"
                ));
                throw new IllegalArgumentException("english.yml -> language/file not found");
            }
            Nexus.nexusLogger.error(List.of(
                    "Provided language file " + language + " does not exist.",
                    "Selecting Fallback language: english"
            ));
            language = "english";
        }

        userLanguage.put(uuid, language);
    }
}
