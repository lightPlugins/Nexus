package io.nexstudios.nexus.bukkit.handler;

import io.nexstudios.nexus.bukkit.NexusPlugin;
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

public record MessageSender(NexusLanguage language) {

    public void send(CommandSender sender, String path) {
        Component component;

        if (sender == null) {
            NexusPlugin.nexusLogger.warning(List.of(
                    "Cannot send message: CommandSender or Component is null."
            ));
            return;
        }

        if (sender instanceof Player player) {
            component = language.getTranslation(player.getUniqueId(), path, true);
            if (NexusPlugin.getInstance().papiHook != null) {
                String legacyText = MiniMessage.miniMessage().serialize(component);
                component = NexusPlugin.getInstance().papiHook.translate(player, legacyText);
            }
            player.sendMessage(component);
        } else {
            UUID consoleUUID = NexusPlugin.getInstance().getNexusLanguage().getConsoleUUID();
            component = language.getTranslation(consoleUUID, path, true);
            String legacyText = PlainTextComponentSerializer.plainText().serialize(component);
            NexusPlugin.nexusLogger.info(legacyText);
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
            Component component = language.getTranslation(player.getUniqueId(), path, true);
            String miniMessageText = mm.serialize(component);

            if (NexusPlugin.getInstance().papiHook != null) {
                miniMessageText = PlaceholderAPI.setPlaceholders(player, miniMessageText);
            }

            String unescaped = miniMessageText.replace("\\", "");

            Component result = mm.deserialize(unescaped, safeResolver);
            player.sendMessage(result);
            return;
        }

        // Console
        UUID consoleUUID = language.getConsoleUUID();
        Component component = language.getTranslation(consoleUUID, path, true);

        String serialized = mm.serialize(component).replace("\\", "");
        Component resolved = mm.deserialize(serialized, safeResolver);

        NexusPlugin.nexusLogger.info(mm.serialize(resolved));
    }

    public Component stringToComponent(Player player, String legacyText) {
        String text = legacyText;
        if (NexusPlugin.getInstance().papiHook != null) {
            text = PlaceholderAPI.setPlaceholders(player, legacyText);
        }
        return MiniMessage.miniMessage()
                .deserialize(text)
                .decoration(TextDecoration.ITALIC, false);
    }

    public Component stringToComponent(Player player, String legacyText, TagResolver resolver) {
        String text = legacyText;
        if (NexusPlugin.getInstance().papiHook != null) {
            text = PlaceholderAPI.setPlaceholders(player, legacyText).replace("\\", "");
        }
        return MiniMessage.miniMessage()
                .deserialize(text, resolver)
                .decoration(TextDecoration.ITALIC, false);
    }
}
