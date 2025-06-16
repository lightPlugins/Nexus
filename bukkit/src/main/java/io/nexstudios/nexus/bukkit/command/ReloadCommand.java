package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.Nexus;
import org.bukkit.command.CommandSender;

@CommandAlias("nexus")
public class ReloadCommand extends BaseCommand {

    @Subcommand("reload")
    @CommandCompletion("@inventories")
    @CommandPermission("nexus.command.admin.reload")
    @Description("Reloads the plugin configuration and settings.")
    public void onReload(CommandSender sender) {

        Nexus.getInstance().onReload();
        Nexus.getInstance().messageSender.send(sender, "general.reload");

    }


}
