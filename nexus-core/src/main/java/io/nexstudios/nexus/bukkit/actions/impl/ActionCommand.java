package io.nexstudios.nexus.bukkit.actions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexParams;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.actions.NexusActionContext;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ActionCommand implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(NexusActionContext context) {
        ActionData data = context.data();
        Player player = context.requirePlayer();
        Location location = context.location();
        NexParams params = context.params();

        if(player == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Failed to execute ActionCommand",
                    "ActionCommand is not compatible with the current context.",
                    "A Player is required to execute this action!"
            ));
            return;
        }

        Object cmdObj = data.getData().get("command");
        if (!(cmdObj instanceof String command)) {
            NexusPlugin.nexusLogger.error("Invalid command data.");
            NexusPlugin.nexusLogger.error("Missing or non-string 'command' parameter for action");
            return;
        }

        Object useConsoleObj = data.getData().get("use-console");
        if (!(useConsoleObj instanceof Boolean)) {
            NexusPlugin.nexusLogger.error("Invalid command data.");
            NexusPlugin.nexusLogger.error("Missing or non-boolean 'use-console' parameter for action");
            return;
        }

        command = StringUtils.parsePlaceholderAPI(player, command);

        if (params != null && !params.isEmpty()) {
            command = StringUtils.replaceKeyWithValue(command, params);
        }

        command = command.replace("#player#", player.getName());

        boolean useConsole = (Boolean) useConsoleObj;

        if (useConsole) {
            NexusPlugin.getInstance().getServer()
                    .dispatchCommand(
                            NexusPlugin.getInstance().getServer().getConsoleSender(),
                            command
                    );
        } else {
            player.performCommand(command);
        }
    }
}