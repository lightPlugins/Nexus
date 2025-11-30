package io.nexstudios.nexus.bukkit.actions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexParams;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.actions.NexusActionContext;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
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
    public void execute(NexusActionContext context) {
        ActionData data = context.data();
        Player player = context.requirePlayer();
        NexParams params = context.params() == null ? NexParams.empty() : context.params();

        Object raw = data.getData().get("message");
        if (!(raw instanceof String rawMessage)) {
            NexusPlugin.nexusLogger.warning("Invalid message data. Expected 'message' as String.");
            return;
        }

        // Interne Platzhalter direkt via NexParams ersetzen
        String messageWithInternal = StringUtils.replaceKeyWithValue(rawMessage, params);

        Component component;
        if (NexusPlugin.getInstance().papiHook != null) {
            component = NexusPlugin.getInstance().papiHook.translate(player, messageWithInternal);
        } else {
            component = MiniMessage.miniMessage().deserialize(messageWithInternal);
        }

        player.sendMessage(component);
    }
}