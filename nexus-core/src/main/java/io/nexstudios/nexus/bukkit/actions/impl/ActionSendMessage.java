package io.nexstudios.nexus.bukkit.actions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ActionSendMessage implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation) {

        if(!data.validate(data.getData().get("message"), String.class)) {
            NexusPlugin.nexusLogger.warning("Invalid message data.");
            return;
        }

        String message = (String) data.getData().getOrDefault("message", "Not found");

        Component component;
        if(NexusPlugin.getInstance().papiHook != null) {
            component = NexusPlugin.getInstance().papiHook.translate(player, message);
        } else {
            component = MiniMessage.miniMessage().deserialize(message);
        }

        player.sendMessage(component);
    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation, Map<String, Object> params) {

        if(!data.validate(data.getData().get("message"), String.class)) {
            NexusPlugin.nexusLogger.warning("Invalid message data.");
            return;
        }

        String message = StringUtils.replaceInternalPlaceholders(
                (String) data.getData().getOrDefault("message", "Not found"), params);

        Component component;
        if(NexusPlugin.getInstance().papiHook != null) {
            component = NexusPlugin.getInstance().papiHook.translate(player, message);
        } else {
            component = MiniMessage.miniMessage().deserialize(message);
        }

        player.sendMessage(component);

    }

    @Override
    public void execute(Player player, ActionData data, Location location, TagResolver tagResolver) {


        if(!data.validate(data.getData().get("message"), String.class)) {
            NexusPlugin.nexusLogger.warning("Invalid message data.");
            return;
        }

        String message = (String) data.getData().getOrDefault("message", "Not found");
        Component component = MiniMessage.miniMessage().deserialize(message, tagResolver);
        player.sendMessage(component);

    }
}
