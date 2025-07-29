package io.nexstudios.nexus.bukkit.utils;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.entity.Player;

import java.util.List;

public class MiniMessageUtil {


    public static Component replace(Component component, TagResolver resolver, Player player) {

        TagResolver.Builder builder = TagResolver.builder();

        builder.resolvers(resolver);
        builder.resolvers(StandardTags.color());
        builder.resolvers(StandardTags.gradient());
        builder.resolvers(StandardTags.decorations());

        // Schritt 1: MiniMessage-Tags durch den TagResolver ersetzen
        String miniMessageText = MiniMessage.miniMessage().serialize(component).replace("\\", "");
        Component resolvedComponent = MiniMessage.miniMessage().deserialize(miniMessageText, builder.build());

        // Schritt 2: Den Platzhaltertext für PlaceholderAPI holen (nach Verarbeitung der Tags)
        String resolvedText = MiniMessage.miniMessage().serialize(resolvedComponent);

        // PlaceholderAPI-Anwendung
        if (NexusPlugin.getInstance().papiHook != null) {
            String translatedText = PlaceholderAPI.setPlaceholders(player, resolvedText);

            // Zurück zu MiniMessage nach PlaceholderAPI-Verarbeitung
            return MiniMessage.miniMessage().deserialize(translatedText);
        }

        // Fallback (wenn PlaceholderAPI nicht vorhanden)
        return resolvedComponent;
    }

}
