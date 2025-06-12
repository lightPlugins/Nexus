package io.nexstudios.nexus.bukkit.language;

import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.common.logging.files.NexusFileReader;
import lombok.Getter;

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
