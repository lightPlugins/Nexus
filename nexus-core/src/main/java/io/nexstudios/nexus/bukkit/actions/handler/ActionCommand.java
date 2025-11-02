package io.nexstudios.nexus.bukkit.actions.handler;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ActionCommand implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation) {

        if(!data.validate(data.getData().get("command"), String.class)) {
            NexusPlugin.nexusLogger.error("Invalid command data.");
            NexusPlugin.nexusLogger.error("Missing 'command' parameter for action");
            return;
        }

        if(!data.validate(data.getData().get("use-console"), Boolean.class)) {
            NexusPlugin.nexusLogger.error("Invalid command data.");
            NexusPlugin.nexusLogger.error("Missing 'use-console' parameter for action");
            return;
        }

        String command = (String) data.getData().getOrDefault("command", "Not found");
        // placeholderapi translation
        command = StringUtils.parsePlaceholderAPI(player, command);
        // internal placeholder translation
        command = command.replace("#player#", player.getName());

        boolean useConsole = (boolean) data.getData().getOrDefault("use-console", false);

        if(useConsole) {
            NexusPlugin.getInstance().getServer().dispatchCommand(NexusPlugin.getInstance().getServer().getConsoleSender(), command);
            return;
        }

        player.performCommand(command);

    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation, Map<String, Object> params) {

    }
}
