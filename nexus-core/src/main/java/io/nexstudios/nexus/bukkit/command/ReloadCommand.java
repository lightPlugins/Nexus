package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.dialog.NexDialogBuilder;
import io.nexstudios.nexus.bukkit.dialog.NexDialogBuilderFactory;
import io.nexstudios.nexus.bukkit.dialog.NexDialogResult;
import io.nexstudios.nexus.bukkit.effects.NexusEffectsApi;
import io.nexstudios.nexus.bukkit.hologram.NexHologramService;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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

    @Subcommand("dialog-settings")
    @CommandPermission("nexus.command.admin")
    @Description("Opens the account settings dialog defined in settings.yml")
    public void onDialogSettings(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }

        // Load the "dialog" section from settings.yml
        ConfigurationSection dialogSection = NexusPlugin.getInstance()
                .getSettingsFile()
                .getConfig()
                .getConfigurationSection("dialog");

        if (dialogSection == null) {
            player.sendMessage(Component.text("Dialog section 'dialog' not found in settings.yml"));
            return;
        }

        // Directly obtain a builder from NexServices
        NexDialogBuilder builder = NexServices.newDialogBuilder()
                .fromConfig(dialogSection)
                .primaryTextKey("username")
                .onSubmit((p, result) -> handleDialogSubmit(p, result))
                .onCancel(p -> p.sendMessage(
                        MiniMessage.miniMessage().deserialize("<yellow>Du hast den Dialog abgebrochen.</yellow>")
                ));

        builder.open(player);
    }

    private void handleDialogSubmit(Player player, NexDialogResult result) {
        // Read values from the dialog result
        String username = result.getString("username");
        int volume = result.getInt("volume", 50);
        boolean notifications = result.getBoolean("notifications", false);

        Component msg = MiniMessage.miniMessage().deserialize(
                "<gray>Einstellungen gespeichert:</gray>\n" +
                        "<white>Username:</white> <green>" + username + "</green>\n" +
                        "<white>Lautstärke:</white> <aqua>" + volume + "</aqua>\n" +
                        "<white>Benachrichtigungen:</white> " +
                        (notifications ? "<green>aktiv</green>" : "<red>deaktiviert</red>")
        );

        player.sendMessage(msg);
    }
}
