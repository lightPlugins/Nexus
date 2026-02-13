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

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public void send(CommandSender sender, String path) {
        sendInternal(sender, path, null, true);
    }

    public void send(CommandSender sender, String path, boolean withPrefix) {
        sendInternal(sender, path, null, withPrefix);
    }

    public void send(CommandSender sender, String path, TagResolver tagResolver) {
        sendInternal(sender, path, tagResolver, true);
    }

    public void send(CommandSender sender, String path, TagResolver tagResolver, boolean withPrefix) {
        sendInternal(sender, path, tagResolver, withPrefix);
    }

    private void sendInternal(CommandSender sender, String path, TagResolver tagResolver, boolean withPrefix) {
        if (sender == null) {
            NexusPlugin.nexusLogger.warning(List.of("Cannot send message: CommandSender is null."));
            return;
        }

        if (sender instanceof Player player) {
            UUID playerUUID = player.getUniqueId();
            boolean isList = isListPath(playerUUID, path);

            // Prefix rule: only for players and only for non-list paths.
            boolean effectiveWithPrefix = withPrefix && !isList;

            if (isList) {
                // For lists: never prefix
                List<Component> components = language.getTranslationList(playerUUID, path, false);
                for (Component component : components) {
                    player.sendMessage(resolveForPlayer(player, component, tagResolver));
                }
            } else {
                Component component = language.getTranslation(playerUUID, path, effectiveWithPrefix);
                player.sendMessage(resolveForPlayer(player, component, tagResolver));
            }
            return;
        }

        // Console: never send prefix (even if path is a list)
        UUID consoleUUID = language.getConsoleUUID();
        boolean isList = isListPath(consoleUUID, path);

        if (isList) {
            List<Component> components = language.getTranslationList(consoleUUID, path, false);
            for (Component component : components) {
                logToConsole(resolveForConsole(component, tagResolver));
            }
        } else {
            Component component = language.getTranslation(consoleUUID, path, false);
            logToConsole(resolveForConsole(component, tagResolver));
        }
    }

    private Component resolveForPlayer(Player player, Component component, TagResolver tagResolver) {
        Component resolved = NexusPlaceholders.resolveWithPlayer(component, player);

        // If a TagResolver is provided, we must go through MiniMessage to apply it.
        if (tagResolver != null) {
            String miniMessageText = MM.serialize(resolved);

            if (NexusPlugin.getInstance().papiHook != null) {
                miniMessageText = PlaceholderAPI.setPlaceholders(player, miniMessageText);
            }

            String unescaped = miniMessageText.replace("\\", "");
            return MM.deserialize(unescaped, tagResolver);
        }

        // No TagResolver: keep the component pipeline and use the plugin hook.
        if (NexusPlugin.getInstance().papiHook != null) {
            String legacyText = MM.serialize(resolved);
            return NexusPlugin.getInstance().papiHook.translate(player, legacyText);
        }

        return resolved;
    }

    private Component resolveForConsole(Component component, TagResolver tagResolver) {
        Component resolved = NexusPlaceholders.resolve(component);

        if (tagResolver == null) {
            return resolved;
        }

        String serialized = MM.serialize(resolved).replace("\\", "");
        return MM.deserialize(serialized, tagResolver);
    }

    private void logToConsole(Component component) {
        NexusPlugin.nexusLogger.info(PLAIN.serialize(component));
    }

    public Component stringToComponent(Player player, String legacyText) {
        String text = legacyText;

        if (NexusPlugin.getInstance().papiHook != null) {
            text = PlaceholderAPI.setPlaceholders(player, legacyText);
        }

        Component component = MM.deserialize(text)
                .decoration(TextDecoration.ITALIC, false);

        return NexusPlaceholders.resolveWithPlayer(component, player);
    }

    public Component stringToComponent(Player player, String legacyText, TagResolver resolver) {
        String text = legacyText;

        if (NexusPlugin.getInstance().papiHook != null) {
            text = PlaceholderAPI.setPlaceholders(player, legacyText).replace("\\", "");
        }

        Component component = MM.deserialize(text, resolver)
                .decoration(TextDecoration.ITALIC, false);

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