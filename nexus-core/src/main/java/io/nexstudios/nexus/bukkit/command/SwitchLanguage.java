package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.items.AttributeOperation;
import io.nexstudios.nexus.bukkit.items.ItemAttributeSpec;
import io.nexstudios.nexus.bukkit.items.ItemHideFlag;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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

        ItemStack item = NexServices.newItemBuilder()
                .material(Material.DIAMOND_SWORD)
                .stackSize(99)
                .amount(1)
                .displayName(Component.text("<red>Example Item"))
                .lore(List.of(
                        Component.text("This is an example item"),
                        Component.text("PlaceholderAPI support %player_name%")
                ))
                .enchantments(Map.of(
                        Enchantment.SHARPNESS, 5,
                        Enchantment.UNBREAKING, 3
                ))
                .hideFlags(Set.of(
                        ItemHideFlag.HIDE_ENCHANTS,
                        ItemHideFlag.HIDE_ATTRIBUTES
                ))
                // Attribute: name, attributeKey, amount, operation, slot
                .attributes(
                        new ItemAttributeSpec(
                                "custom_attack_speed",
                                NamespacedKey.fromString("minecraft:attack_speed"),
                                10.0,
                                AttributeOperation.ADD_NUMBER,
                                Set.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
                        ),
                        new ItemAttributeSpec(
                                "custom_attack_damage",
                                NamespacedKey.fromString("minecraft:attack_damage"),
                                20.0,
                                AttributeOperation.ADD_NUMBER,
                                Set.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)
                        )
                )

                .modelData(1337)
                //.tooltipStyle(NamespacedKey.fromString("minecraft:legendary"))
                //.itemModel(NamespacedKey.fromString("my_pack:items/sword_01"))
                .unbreakable(false)
                .build();

        player.getInventory().addItem(item);

    }

}
