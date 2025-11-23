package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.items.AttributeOperation;
import io.nexstudios.nexus.bukkit.items.ItemAttributeSpec;
import io.nexstudios.nexus.bukkit.items.ItemHideFlag;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.server.dialog.body.DialogBodyTypes;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
