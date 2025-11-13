
package io.nexstudios.nexus.bukkit.handler;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import io.nexstudios.nexus.bukkit.placeholder.NexusPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public record MessageSender(NexusLanguage language) {

    public void send(CommandSender sender, String path) {
        if (sender == null) {
            NexusPlugin.nexusLogger.warning(List.of(
                    "Cannot send message: CommandSender or Component is null."
            ));
            return;
        }

        if (sender instanceof Player player) {
            UUID playerUUID = player.getUniqueId();

            if (isListPath(playerUUID, path)) {
                List<Component> components = language.getTranslationList(playerUUID, path, true);
                for (Component component : components) {
                    // 1. NexusPlaceholders resolven
                    component = NexusPlaceholders.resolveWithPlayer(component, player);

                    // 2. PlaceholderAPI resolven (falls vorhanden)
                    if (NexusPlugin.getInstance().papiHook != null) {
                        String legacyText = MiniMessage.miniMessage().serialize(component);
                        component = NexusPlugin.getInstance().papiHook.translate(player, legacyText);
                    }
                    player.sendMessage(component);
                }
            } else {
                Component component = language.getTranslation(playerUUID, path, true);

                // 1. NexusPlaceholders resolven
                component = NexusPlaceholders.resolveWithPlayer(component, player);

                // 2. PlaceholderAPI resolven (falls vorhanden)
                if (NexusPlugin.getInstance().papiHook != null) {
                    String legacyText = MiniMessage.miniMessage().serialize(component);
                    component = NexusPlugin.getInstance().papiHook.translate(player, legacyText);
                }
                player.sendMessage(component);
            }
        } else {
            UUID consoleUUID = NexusPlugin.getInstance().getNexusLanguage().getConsoleUUID();

            if (isListPath(consoleUUID, path)) {
                List<Component> components = language.getTranslationList(consoleUUID, path, true);
                for (Component component : components) {
                    // NexusPlaceholders resolven (ohne Player für Console)
                    component = NexusPlaceholders.resolve(component);

                    String legacyText = PlainTextComponentSerializer.plainText().serialize(component);
                    NexusPlugin.nexusLogger.info(legacyText);
                }
            } else {
                Component component = language.getTranslation(consoleUUID, path, true);

                // NexusPlaceholders resolven (ohne Player für Console)
                component = NexusPlaceholders.resolve(component);

                String legacyText = PlainTextComponentSerializer.plainText().serialize(component);
                NexusPlugin.nexusLogger.info(legacyText);
            }
        }
    }

    public void send(CommandSender sender, String path, boolean withPrefix) {
        if (sender == null) {
            NexusPlugin.nexusLogger.warning(List.of(
                    "Cannot send message: CommandSender or Component is null."
            ));
            return;
        }

        if (sender instanceof Player player) {
            UUID playerUUID = player.getUniqueId();

            if (isListPath(playerUUID, path)) {
                List<Component> components = language.getTranslationList(playerUUID, path, withPrefix);
                for (Component component : components) {
                    // 1. NexusPlaceholders resolven
                    component = NexusPlaceholders.resolveWithPlayer(component, player);

                    // 2. PlaceholderAPI resolven (falls vorhanden)
                    if (NexusPlugin.getInstance().papiHook != null) {
                        String legacyText = MiniMessage.miniMessage().serialize(component);
                        component = NexusPlugin.getInstance().papiHook.translate(player, legacyText);
                    }
                    player.sendMessage(component);
                }
            } else {
                Component component = language.getTranslation(playerUUID, path, withPrefix);

                // 1. NexusPlaceholders resolven
                component = NexusPlaceholders.resolveWithPlayer(component, player);

                // 2. PlaceholderAPI resolven (falls vorhanden)
                if (NexusPlugin.getInstance().papiHook != null) {
                    String legacyText = MiniMessage.miniMessage().serialize(component);
                    component = NexusPlugin.getInstance().papiHook.translate(player, legacyText);
                }
                player.sendMessage(component);
            }
        } else {
            UUID consoleUUID = NexusPlugin.getInstance().getNexusLanguage().getConsoleUUID();

            if (isListPath(consoleUUID, path)) {
                List<Component> components = language.getTranslationList(consoleUUID, path, withPrefix);
                for (Component component : components) {
                    // NexusPlaceholders resolven (ohne Player für Console)
                    component = NexusPlaceholders.resolve(component);

                    String legacyText = PlainTextComponentSerializer.plainText().serialize(component);
                    NexusPlugin.nexusLogger.info(legacyText);
                }
            } else {
                Component component = language.getTranslation(consoleUUID, path, withPrefix);

                // NexusPlaceholders resolven (ohne Player für Console)
                component = NexusPlaceholders.resolve(component);

                String legacyText = PlainTextComponentSerializer.plainText().serialize(component);
                NexusPlugin.nexusLogger.info(legacyText);
            }
        }
    }

    public void send(CommandSender sender, String path, TagResolver tagResolver) {
        if (sender == null) {
            NexusPlugin.nexusLogger.warning(List.of(
                    "Cannot send message: CommandSender is null."
            ));
            return;
        }

        TagResolver safeResolver = tagResolver != null ? tagResolver : TagResolver.empty();
        MiniMessage mm = MiniMessage.miniMessage();

        if (sender instanceof Player player) {
            UUID playerUUID = player.getUniqueId();

            if (isListPath(playerUUID, path)) {
                List<Component> components = language.getTranslationList(playerUUID, path, true);
                for (Component component : components) {
                    // 1. NexusPlaceholders resolven
                    component = NexusPlaceholders.resolveWithPlayer(component, player);

                    String miniMessageText = mm.serialize(component);

                    // 2. PlaceholderAPI resolven (falls vorhanden)
                    if (NexusPlugin.getInstance().papiHook != null) {
                        miniMessageText = PlaceholderAPI.setPlaceholders(player, miniMessageText);
                    }

                    // 3. Custom TagResolver anwenden
                    String unescaped = miniMessageText.replace("\\", "");
                    Component result = mm.deserialize(unescaped, safeResolver);
                    player.sendMessage(result);
                }
            } else {
                Component component = language.getTranslation(playerUUID, path, true);

                // 1. NexusPlaceholders resolven
                component = NexusPlaceholders.resolveWithPlayer(component, player);

                String miniMessageText = mm.serialize(component);

                // 2. PlaceholderAPI resolven (falls vorhanden)
                if (NexusPlugin.getInstance().papiHook != null) {
                    miniMessageText = PlaceholderAPI.setPlaceholders(player, miniMessageText);
                }

                // 3. Custom TagResolver anwenden
                String unescaped = miniMessageText.replace("\\", "");
                Component result = mm.deserialize(unescaped, safeResolver);
                player.sendMessage(result);
            }
            return;
        }

        // Console
        UUID consoleUUID = language.getConsoleUUID();

        if (isListPath(consoleUUID, path)) {
            List<Component> components = language.getTranslationList(consoleUUID, path, true);
            for (Component component : components) {
                // 1. NexusPlaceholders resolven
                component = NexusPlaceholders.resolve(component);

                // 2. Custom TagResolver anwenden
                String serialized = mm.serialize(component).replace("\\", "");
                Component resolved = mm.deserialize(serialized, safeResolver);
                NexusPlugin.nexusLogger.info(mm.serialize(resolved));
            }
        } else {
            Component component = language.getTranslation(consoleUUID, path, true);

            // 1. NexusPlaceholders resolven
            component = NexusPlaceholders.resolve(component);

            // 2. Custom TagResolver anwenden
            String serialized = mm.serialize(component).replace("\\", "");
            Component resolved = mm.deserialize(serialized, safeResolver);
            NexusPlugin.nexusLogger.info(mm.serialize(resolved));
        }
    }

    public void send(CommandSender sender, String path, TagResolver tagResolver, boolean withPrefix) {
        if (sender == null) {
            NexusPlugin.nexusLogger.warning(List.of(
                    "Cannot send message: CommandSender is null."
            ));
            return;
        }

        TagResolver safeResolver = tagResolver != null ? tagResolver : TagResolver.empty();
        MiniMessage mm = MiniMessage.miniMessage();

        if (sender instanceof Player player) {
            UUID playerUUID = player.getUniqueId();

            if (isListPath(playerUUID, path)) {
                List<Component> components = language.getTranslationList(playerUUID, path, withPrefix);
                for (Component component : components) {
                    // 1. NexusPlaceholders resolven
                    component = NexusPlaceholders.resolveWithPlayer(component, player);

                    String miniMessageText = mm.serialize(component);

                    // 2. PlaceholderAPI resolven (falls vorhanden)
                    if (NexusPlugin.getInstance().papiHook != null) {
                        miniMessageText = PlaceholderAPI.setPlaceholders(player, miniMessageText);
                    }

                    // 3. Custom TagResolver anwenden
                    String unescaped = miniMessageText.replace("\\", "");
                    Component result = mm.deserialize(unescaped, safeResolver);
                    player.sendMessage(result);
                }
            } else {
                Component component = language.getTranslation(playerUUID, path, withPrefix);

                // 1. NexusPlaceholders resolven
                component = NexusPlaceholders.resolveWithPlayer(component, player);

                String miniMessageText = mm.serialize(component);

                // 2. PlaceholderAPI resolven (falls vorhanden)
                if (NexusPlugin.getInstance().papiHook != null) {
                    miniMessageText = PlaceholderAPI.setPlaceholders(player, miniMessageText);
                }

                // 3. Custom TagResolver anwenden
                String unescaped = miniMessageText.replace("\\", "");
                Component result = mm.deserialize(unescaped, safeResolver);
                player.sendMessage(result);
            }
            return;
        }

        // Console
        UUID consoleUUID = language.getConsoleUUID();

        if (isListPath(consoleUUID, path)) {
            List<Component> components = language.getTranslationList(consoleUUID, path, withPrefix);
            for (Component component : components) {
                // 1. NexusPlaceholders resolven
                component = NexusPlaceholders.resolve(component);

                // 2. Custom TagResolver anwenden
                String serialized = mm.serialize(component).replace("\\", "");
                Component resolved = mm.deserialize(serialized, safeResolver);
                NexusPlugin.nexusLogger.info(mm.serialize(resolved));
            }
        } else {
            Component component = language.getTranslation(consoleUUID, path, withPrefix);

            // 1. NexusPlaceholders resolven
            component = NexusPlaceholders.resolve(component);

            // 2. Custom TagResolver anwenden
            String serialized = mm.serialize(component).replace("\\", "");
            Component resolved = mm.deserialize(serialized, safeResolver);
            NexusPlugin.nexusLogger.info(mm.serialize(resolved));
        }
    }

    public Component stringToComponent(Player player, String legacyText) {
        String text = legacyText;

        // 1. PlaceholderAPI resolven (falls vorhanden)
        if (NexusPlugin.getInstance().papiHook != null) {
            text = PlaceholderAPI.setPlaceholders(player, legacyText);
        }

        // 2. Zu Component konvertieren und NexusPlaceholders resolven
        Component component = MiniMessage.miniMessage()
                .deserialize(text)
                .decoration(TextDecoration.ITALIC, false);

        // 3. NexusPlaceholders resolven
        return NexusPlaceholders.resolveWithPlayer(component, player);
    }

    public Component stringToComponent(Player player, String legacyText, TagResolver resolver) {
        String text = legacyText;

        // 1. PlaceholderAPI resolven (falls vorhanden)
        if (NexusPlugin.getInstance().papiHook != null) {
            text = PlaceholderAPI.setPlaceholders(player, legacyText).replace("\\", "");
        }

        // 2. Zu Component konvertieren mit TagResolver
        Component component = MiniMessage.miniMessage()
                .deserialize(text, resolver)
                .decoration(TextDecoration.ITALIC, false);

        // 3. NexusPlaceholders resolven
        return NexusPlaceholders.resolveWithPlayer(component, player);
    }

    private boolean isListPath(UUID uuid, String path) {
        String lang = language.getSelectedLanguage(uuid);
        var languageConfig = language.getLoadedLanguages().get(lang);

        if (languageConfig == null) {
            languageConfig = language.getLoadedLanguages().get("english");
        }

        if (languageConfig != null && languageConfig.contains(path)) {
            return languageConfig.isList(path);
        }

        return false;
    }

}