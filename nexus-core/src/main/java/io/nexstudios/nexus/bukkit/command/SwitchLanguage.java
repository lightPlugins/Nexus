package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

@CommandAlias("nexus")
public class SwitchLanguage extends BaseCommand {

    @Subcommand("language")
    @CommandPermission("nexus.command.admin.reload")
    @CommandCompletion("@languages")
    @Description("Reloads the plugin configuration and settings.")
    public void onLanguageSwitch(Player player, String language) {

        NexusPlugin.getInstance().getNexusLanguage().selectLanguage(player.getUniqueId(), language);

        TagResolver tagResolver = Placeholder.parsed("nex_language", language);
        NexusPlugin.getInstance().messageSender.send(player, "general.switch-language", tagResolver);

    }

}
