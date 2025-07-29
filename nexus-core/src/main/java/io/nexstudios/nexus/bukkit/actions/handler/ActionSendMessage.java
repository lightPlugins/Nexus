package io.nexstudios.nexus.bukkit.actions.handler;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ActionSendMessage implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(Player player, ActionData data) {

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
}
