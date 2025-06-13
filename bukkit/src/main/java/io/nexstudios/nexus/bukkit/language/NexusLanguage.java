package io.nexstudios.nexus.bukkit.language;

import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.common.logging.files.NexusFile;
import io.nexstudios.nexus.common.logging.files.NexusFileReader;
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
    private final NexusFileReader rawLanguageFiles;

    public NexusLanguage(NexusFileReader rawLanguageFiles) {
        this.rawLanguageFiles = rawLanguageFiles;

        rawLanguageFiles.getFiles().forEach(file -> {
           String languageName = file.getName().replace(".yml", "");
           availableLanguages.put(languageName, file);
        });
    }

    public String getSelectedLanguage(UUID uuid) {
        return userLanguage.getOrDefault(uuid, "english");
    }

    public Component getTranslation(UUID uuid, String path) {
        String lang = getSelectedLanguage(uuid);

        if(lang == null) {
            Nexus.nexusLogger.error(List.of(
                    "Language for UUID " + uuid + " not found.",
                    "Using default language: english"
            ));
            selectLanguage(uuid, "english");
        }

        File languageFile = availableLanguages.get(lang);

        if(languageFile == null) {
            Nexus.nexusLogger.error(List.of(
                    "Language file for " + lang + " not found.",
                    "Using default language: english"
            ));
            languageFile = availableLanguages.get("english");

            if(languageFile == null) {
                Nexus.nexusLogger.error(List.of(
                        "Default language file english.yml not found.",
                        "This is a critical problem and should be reported",
                        "to the developer: NexStudios"
                ));
                throw new IllegalArgumentException("english.yml -> language/file not found");
            }
        }

        FileConfiguration languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        if(languageConfig.contains(path)) {
            String translation = languageConfig.getString(path);
            if(translation == null) {
                Nexus.nexusLogger.error(List.of(
                        "Translation for path '" + path + "' in language '" + lang + "' is null.",
                        "Using default translation: " + path
                ));
                return Component.text(path);
            }
            return MiniMessage.miniMessage().deserialize(translation);
        }

        return Component.text(path);
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
