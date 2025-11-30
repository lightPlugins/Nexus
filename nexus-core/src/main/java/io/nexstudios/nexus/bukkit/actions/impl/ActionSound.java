package io.nexstudios.nexus.bukkit.actions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.actions.NexusActionContext;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ActionSound implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(NexusActionContext context) {
        ActionData data = context.data();
        Player player = context.requirePlayer();

        Object soundObj = data.getData().get("sound");
        if (!(soundObj instanceof String sound)) {
            NexusPlugin.nexusLogger.error("Invalid sound data.");
            NexusPlugin.nexusLogger.error("Missing or non-string 'sound' parameter for action");
            return;
        }

        String volumeStr = String.valueOf(data.getData().getOrDefault("volume", "1.0"));
        String pitchStr  = String.valueOf(data.getData().getOrDefault("pitch", "1.0"));

        float volume;
        float pitch;
        try {
            volume = Float.parseFloat(volumeStr);
        } catch (NumberFormatException e) {
            volume = 1.0f;
        }
        try {
            pitch = Float.parseFloat(pitchStr);
        } catch (NumberFormatException e) {
            pitch = 1.0f;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}