package io.nexstudios.nexus.bukkit.items;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.core.component.DataComponentType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public interface ItemBuilder {

    ItemBuilder setAmount(int amount);
    ItemBuilder setData(DataComponentType<?> data);
    ItemBuilder unsetData(DataComponentType<?> data);
    ItemBuilder setMaterial(Material material);
    ItemBuilder addEnchantment(Enchantment enchantment, int level);
    ItemBuilder setDisplayName(Component displayName);
    ItemBuilder addLoreLine(Component loreLine);
    ItemBuilder addLoreLines(Component... loreLines);
    ItemBuilder setUnbreakable(boolean unbreakable);
    ItemBuilder setCustomModelData(int customModelData);
    ItemBuilder setTooltipStyle(Key tooltipStyle);
    ItemBuilder build();
}
