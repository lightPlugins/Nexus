package io.nexstudios.nexus.bukkit.actions.handler;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ActionSound implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation) {

        if(!data.validate(data.getData().get("sound"), String.class)) {
            NexusPlugin.nexusLogger.error("Invalid sound data.");
            NexusPlugin.nexusLogger.error("Missing 'sound' parameter for action");
            return;
        }

        String sound = (String) data.getData().getOrDefault("sound", "Not found");
        String volume = (String) data.getData().getOrDefault("volume", "1.0");
        String pitch = (String) data.getData().getOrDefault("pitch", "1.0");

        player.playSound(player.getLocation(), sound, Float.parseFloat(volume), Float.parseFloat(pitch));

    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation, Map<String, Object> params) {
        execute(player, data, targetLocation);
    }

    @Override
    public void execute(Player player, ActionData data, Location location, TagResolver tagResolver) {
        execute(player, data, location);
    }
}
