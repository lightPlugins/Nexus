package io.nexstudios.nexus.bukkit.language;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.utils.NexusLogger;
import io.nexstudios.nexus.bukkit.files.NexusFileReader;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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

    public FileConfiguration getSelectedLanguageConfig(UUID uuid) {
        return loadedLanguages.get(getSelectedLanguage(uuid));
    }

    public String getSelectedLanguage(UUID uuid) {
        return userLanguage.getOrDefault(uuid, "english");
    }

    public boolean hasPlayerDefaultLanguage(UUID uuid) {
        return userLanguage.containsKey(uuid) && userLanguage.get(uuid).equals("english");
    }

    public String getPrefix(UUID uuid) {
        String lang = getSelectedLanguage(uuid);
        FileConfiguration languageConfig = loadedLanguages.get(lang);

        if (languageConfig == null) {
            NexusPlugin.nexusLogger.error(List.of(
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

    public List<Component> getTranslationList(UUID uuid, String path, boolean withPrefix) {
        String lang = getSelectedLanguage(uuid);
        FileConfiguration languageConfig = loadedLanguages.get(lang);

        if (languageConfig == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Language configuration for " + lang + " not found.",
                    "Using default language: english"
            ));
            languageConfig = loadedLanguages.get("english");
        }

        List<Component> components = new ArrayList<>();
        if (languageConfig != null && languageConfig.contains(path)) {
            List<String> translations = languageConfig.getStringList(path);
            for (String translation : translations) {
                if(withPrefix) {
                    components.add(MiniMessage.miniMessage().deserialize(getPrefix(uuid) + " " + translation)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    components.add(MiniMessage.miniMessage().deserialize(translation)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
        } else {
            if(withPrefix) {
                components.add(Component.text(getPrefix(uuid) + " " + path)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                components.add(Component.text(path)
                        .decoration(TextDecoration.ITALIC, false));
            }

        }
        return components;
    }

    public List<Component> getTranslationList(UUID uuid, String path, boolean withPrefix, TagResolver extraResolver) {
        String lang = getSelectedLanguage(uuid);
        FileConfiguration languageConfig = loadedLanguages.get(lang);

        if (languageConfig == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Language configuration for " + lang + " not found.",
                    "Using default language: english"
            ));
            languageConfig = loadedLanguages.get("english");
        }

        TagResolver resolver = (extraResolver != null) ? extraResolver : TagResolver.empty();
        List<Component> components = new ArrayList<>();
        if (languageConfig != null && languageConfig.contains(path)) {
            List<String> translations = languageConfig.getStringList(path);
            for (String translation : translations) {
                String text = withPrefix ? (getPrefix(uuid) + " " + translation) : translation;
                components.add(MiniMessage.miniMessage()
                        .deserialize(text, resolver)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            String fallback = withPrefix ? (getPrefix(uuid) + " " + path) : path;
            components.add(Component.text(fallback).decoration(TextDecoration.ITALIC, false));
        }
        return components;
    }

    public Component getTranslation(UUID uuid, String path, boolean withPrefix) {
        String lang = getSelectedLanguage(uuid);

        if (lang == null) {
            NexusPlugin.nexusLogger.debug(List.of(
                    "Language for UUID " + uuid + " not found.",
                    "Using default language: english"
            ), 3);
            selectLanguage(uuid, "english");
            lang = "english";
        }

        FileConfiguration languageConfig = loadedLanguages.get(lang);

        if (languageConfig == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Language configuration for " + lang + " not found.",
                    "Using default language: english"
            ));
            languageConfig = loadedLanguages.get("english");

            if (languageConfig == null) {
                NexusPlugin.nexusLogger.error(List.of(
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
                NexusPlugin.nexusLogger.error(List.of(
                        "Translation for path '" + path + "' in language '" + lang + "' is null.",
                        "Using default translation: english.yml"
                ));
                return Component.text(path);
            }
            if(withPrefix) {
                return MiniMessage.miniMessage().deserialize(getPrefix(uuid) + " " + translation)
                        .decoration(TextDecoration.ITALIC, false);
            } else {
                return MiniMessage.miniMessage().deserialize(translation)
                        .decoration(TextDecoration.ITALIC, false);
            }
        }
        if(withPrefix) {
            return Component.text(getPrefix(uuid) + " " + path)
                    .decoration(TextDecoration.ITALIC, false);
        } else {
            return Component.text(path)
                    .decoration(TextDecoration.ITALIC, false);
        }

    }

    public Component getTranslation(UUID uuid, String path, boolean withPrefix, TagResolver extraResolver) {
        String lang = getSelectedLanguage(uuid);

        if (lang == null) {
            NexusPlugin.nexusLogger.debug(List.of(
                    "Language for UUID " + uuid + " not found.",
                    "Using default language: english"
            ), 3);
            selectLanguage(uuid, "english");
            lang = "english";
        }

        FileConfiguration languageConfig = loadedLanguages.get(lang);

        if (languageConfig == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Language configuration for " + lang + " not found.",
                    "Using default language: english"
            ));
            languageConfig = loadedLanguages.get("english");

            if (languageConfig == null) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Default language configuration english.yml not found.",
                        "This is a critical problem and should be reported",
                        "to the developer: NexStudios"
                ));
                throw new IllegalArgumentException("english.yml -> language/configuration not found");
            }
        }

        TagResolver resolver = (extraResolver != null) ? extraResolver : TagResolver.empty();

        if (languageConfig.contains(path)) {
            String translation = languageConfig.getString(path);
            if (translation == null) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Translation for path '" + path + "' in language '" + lang + "' is null.",
                        "Using default translation: english.yml"
                ));
                return Component.text(path);
            }
            String text = withPrefix ? (getPrefix(uuid) + " " + translation) : translation;
            return MiniMessage.miniMessage()
                    .deserialize(text, resolver)
                    .decoration(TextDecoration.ITALIC, false);
        }

        String fallback = withPrefix ? (getPrefix(uuid) + " " + path) : path;
        return Component.text(fallback).decoration(TextDecoration.ITALIC, false);
    }

    public void selectLanguage(UUID uuid, String language) {
        if(!availableLanguages.containsKey(language)) {
            if(language.equals("english")) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Could not find the default english file.",
                        "This is a critical problem and should be reported",
                        "to the developer: NexStudios"
                ));
                throw new IllegalArgumentException("english.yml -> language/file not found");
            }
            NexusPlugin.nexusLogger.error(List.of(
                    "Provided language file " + language + " does not exist.",
                    "Selecting Fallback language: english"
            ));
            language = "english";
        }

        userLanguage.put(uuid, language);
    }
}
