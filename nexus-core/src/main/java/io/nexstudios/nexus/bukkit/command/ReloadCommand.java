package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.NexusEffectsApi;
import io.nexstudios.nexus.bukkit.hologram.NexHologramService;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;

import java.util.List;
import java.util.Locale;

@CommandAlias("nexus")
public class ReloadCommand extends BaseCommand {

    @Subcommand("reload")
    @CommandCompletion("@inventories")
    @CommandPermission("nexus.command.admin.reload")
    @Description("Reloads the plugin configuration and settings.")
    public void onReload(CommandSender sender) {

        NexusPlugin.getInstance().onReload();

        NexusEffectsApi.removeExternalNamespace(NexusPlugin.getInstance());
        NexusEffectsApi.registerBindingsFromSection(
                NexusPlugin.getInstance(),
                NexusPlugin.getInstance().getSettingsFile().getConfig()
        );

        NexusPlugin.getInstance().logEffectSystemStats();

        String namespace = NexusPlugin.getInstance().getName().toLowerCase(Locale.ROOT);
        NexusPlugin.getInstance().getInvService().registerNamespace(
                namespace,
                NexusPlugin.getInstance().getInventoryFiles());
        NexusPlugin.getInstance().getInvService().reload();


        NexusPlugin.getInstance().messageSender.send(sender, "general.reload");

    }
}
