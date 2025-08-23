package io.nexstudios.nexus.bukkit.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;

/**
 * Spezifikation für einen Attribut-Modifier-Eintrag.
 */
public record ItemAttributeSpec(
        String name,
        NamespacedKey attributeKey,
        double amount,
        AttributeOperation operation,
        Set<EquipmentSlot> slots

) {}