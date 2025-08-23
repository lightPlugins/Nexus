package io.nexstudios.nexus.bukkit.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Versionsneutrales Builder-Interface.
 * Die konkrete Implementierung wird versioniert per Items.ItemBuilders (Reflection) geladen.
 *
 * Ziel: Nutzung der Paper Data Components API in der jeweiligen Implementierung (ItemStack#setData(...)).
 */
public interface ItemBuilder {

    // Grundlegendes
    ItemBuilder material(Material material);
    ItemBuilder amount(int amount);
    ItemBuilder stackSize(int stackSize);

    // Anzeige
    ItemBuilder displayName(Component name);
    ItemBuilder lore(List<Component> lines);

    // Enchantments (id -> level)
    ItemBuilder enchantments(Map<Enchantment, Integer> enchants);

    // Einzelnes Attribut (ein oder mehrere Slots)
    ItemBuilder attribute(String name, NamespacedKey attributeKey, double amount, AttributeOperation operation, EquipmentSlot... slots);

    // Mehrere Attribute auf einmal
    ItemBuilder attributes(ItemAttributeSpec... specs);
    ItemBuilder attributes(Collection<ItemAttributeSpec> specs);

    // Hide-Flags (interpretation implementierungsabhängig)
    ItemBuilder hideFlags(Set<ItemHideFlag> flags);

    // Model Data (CustomModelData, integer)
    ItemBuilder modelData(Integer modelData);

    // Tooltip Style (z. B. "minecraft:legendary")
    ItemBuilder tooltipStyle(NamespacedKey styleKey);

    // Item Model (z. B. "my_pack:items/sword_01")
    ItemBuilder itemModel(NamespacedKey modelKey);

    // Unbreakable optional (hilfreich zusammen mit hide flags)
    ItemBuilder unbreakable(Boolean unbreakable);

    // Build
    ItemStack build();

    // Convenience
    static ItemBuilder create() {
        return ItemBuilders.create();
    }
}