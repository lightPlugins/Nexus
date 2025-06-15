package io.nexstudios.nexus.bukkit.handler;

import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
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

public class MessageSender {

    private final NexusLanguage language;

    public MessageSender(NexusLanguage language) {
        this.language = language;
    }

    public void send(CommandSender sender, String path) {

        Component component;

        if(sender == null) {
            Nexus.nexusLogger.warning(List.of(
                    "Cannot send message: CommandSender or Component is null."
            ));
            return;
        }

        if (sender instanceof Player player) {
            component = language.getTranslation(player.getUniqueId(), path);
            if(Nexus.getInstance().papiHook != null) {
                String legacyText = PlainTextComponentSerializer.plainText().serialize(component);
                component = Nexus.getInstance().papiHook.translate(player, legacyText);
            }
            player.sendMessage(component);
        } else {
            UUID consoleUUID = Nexus.getInstance().getNexusLanguage().getConsoleUUID();
            component = language.getTranslation(consoleUUID, path);
            String legacyText = PlainTextComponentSerializer.plainText().serialize(component);
            Nexus.nexusLogger.info(legacyText);
        }
    }

    public void send(CommandSender sender, String path, TagResolver tagResolver) {
        if (sender == null) {
            Nexus.nexusLogger.warning(List.of(
                    "Cannot send message: CommandSender is null."
            ));
            return;
        }

        Component component;

        if (sender instanceof Player player) {
            component = language.getTranslation(player.getUniqueId(), path);

            if (Nexus.getInstance().papiHook != null) {
                String miniMessageText = MiniMessage.miniMessage().serialize(component);
                String translatedText = PlaceholderAPI.setPlaceholders(player, miniMessageText);
                component = MiniMessage.miniMessage().deserialize(translatedText, tagResolver);
            } else {
                // replace any backslashes in the serialized component.
                // Without -> ("\<nex_placeholder>")
                // This is a workaround for the issue where MiniMessage placeholders would not translate via TagResolver
                // because of the backslashes being present in the serialized string.
                String plainText = MiniMessage.miniMessage().serialize(component).replace("\\", "");
                component = MiniMessage.miniMessage().deserialize(plainText, tagResolver);
            }

            player.sendMessage(component);
        } else {
            UUID consoleUUID = language.getConsoleUUID();
            component = language.getTranslation(consoleUUID, path);

            if (tagResolver != null) {
                component = MiniMessage.miniMessage().deserialize(
                        MiniMessage.miniMessage().serialize(component),
                        tagResolver
                );
            }

            Nexus.nexusLogger.info(MiniMessage.miniMessage().serialize(component));
        }
    }

    public Component stringToComponent(Player player, String legacyText) {
        String text = legacyText;
        if (Nexus.getInstance().papiHook != null) {
            text = PlaceholderAPI.setPlaceholders(player, legacyText);
        }
        return MiniMessage.miniMessage()
                .deserialize(text)
                .decoration(TextDecoration.ITALIC, false);
    }

    public Component stringToComponent(Player player, String legacyText, TagResolver resolver) {
        String text = legacyText;
        if (Nexus.getInstance().papiHook != null) {
            text = PlaceholderAPI.setPlaceholders(player, legacyText).replace("\\", "");
        }
        return MiniMessage.miniMessage()
                .deserialize(text, resolver)
                .decoration(TextDecoration.ITALIC, false);
    }
}
