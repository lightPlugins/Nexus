package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.Nexus;
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

        Nexus.getInstance().getNexusLanguage().selectLanguage(player.getUniqueId(), language);

        TagResolver tagResolver = Placeholder.parsed("nex_language", language);
        Nexus.getInstance().messageSender.send(player, "general.switch-language", tagResolver);

    }

}
