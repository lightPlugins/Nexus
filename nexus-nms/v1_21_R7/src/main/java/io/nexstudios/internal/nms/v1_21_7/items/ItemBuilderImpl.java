package io.nexstudios.internal.nms.v1_21_7.items;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.items.ItemBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemBuilderImpl implements ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilderImpl(Material material) {
        this.itemStack = ItemStack.of(material);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        itemStack.setData(DataComponentTypes.ENCHANTMENTS, ItemEnchantments.itemEnchantments().add(enchantment, level).build());
        return this;
    }

    @Override
    public ItemBuilder setAmount(int amount) {
        if(amount < 1 || amount > 99) {
            NexusPlugin.nexusLogger.warning(List.of(
                    "Found illegal amount in ItemBuilder",
                    "Error: Amount must be between 1 and 99!",
                    "Falling back to amount 1"
            ));
            amount = 1;
        }
        itemStack.setAmount(amount);
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ItemBuilder setDisplayName(Component displayName) {
        itemStack.setData(DataComponentTypes.ITEM_NAME, displayName);
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ItemBuilder addLoreLine(Component loreLine) {
        itemStack.setData(DataComponentTypes.LORE, ItemLore
                .lore()
                .addLine(loreLine)
                .build());
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ItemBuilder addLoreLines(List<Component> loreLines) {
        itemStack.setData(DataComponentTypes.LORE, ItemLore
                .lore()
                .addLines(loreLines)
                .build());
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        itemStack.setData(DataComponentTypes.UNBREAKABLE);
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ItemBuilder setCustomModelData(int customModelData) {
        itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData
                .customModelData()
                .addString(String.valueOf(customModelData))
                .build());
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ItemBuilder setTooltipStyle(Key tooltipStyle) {
        itemStack.setData(DataComponentTypes.TOOLTIP_STYLE, tooltipStyle);
        return this;
    }

    @Override
    public ItemBuilder build() {
        return this;
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemStack;
    }
}
