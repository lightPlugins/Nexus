package io.nexstudios.nexus.bukkit.items;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Interface for building and customizing item stacks through a fluent API.
 * Defines methods for configuring various attributes and properties of an item,
 * such as enchantments, display name, lore, durability settings, and more.
 * After configuration, the item can be finalized using the {@code build()} method,
 * producing an immutable {@link ItemStack}.
 */
public interface ItemBuilder {

    /**
     * Sets the amount of items in the item stack being built.
     *
     * @param amount the quantity of the item to be set; must be a positive integer.
     * @return the current instance of {@link ItemBuilder} for method chaining.
     */
    ItemBuilder setAmount(int amount);
    /**
     * Adds an enchantment with the specified level to the item being built.
     *
     * @param enchantment the enchantment to be added; must not be null.
     * @param level the level of the enchantment; must be a positive integer.
     * @return the current instance of {@link ItemBuilder} for method chaining.
     */
    ItemBuilder addEnchantment(Enchantment enchantment, int level);
    /**
     * Sets the display name of the item being built.
     *
     * @param displayName the display name to set, represented as a {@link Component};
     *                    must not be null.
     * @return the current instance of {@link ItemBuilder} for method chaining.
     */
    ItemBuilder setDisplayName(Component displayName);
    /**
     * Adds a lore line to the item being built.
     *
     * @param loreLine the text to add to the item's lore, represented as a {@link Component};
     *                 must not be null.
     * @return the current instance of {@link ItemBuilder} for method chaining.
     */
    ItemBuilder addLoreLine(Component loreLine);
    /**
     * Adds multiple lines of lore to the item's existing lore list.
     * Each line is represented as an instance of {@link Component}.
     *
     * @param loreLines a list of {@link Component} instances representing the lines of lore to add;
     *                  must not be null and should contain valid lore components.
     * @return the current instance of {@link ItemBuilder} for method chaining.
     */
    ItemBuilder addLoreLines(List<Component> loreLines);
    /**
     * Sets whether the item being built should be unbreakable.
     * An unbreakable item will not take durability damage during usage.
     *
     * @param unbreakable true if the item should be unbreakable; false otherwise.
     * @return the current instance of {@link ItemBuilder} for method chaining.
     */
    ItemBuilder setUnbreakable(boolean unbreakable);
    /**
     * Sets the custom model data for the item being built.
     * Custom model data is used to reference custom textures or models from resource packs.
     *
     * @param customModelData the integer value representing the custom model data to be set.
     *                        This value should be a non-negative integer.
     * @return the current instance of {@link ItemBuilder} for method chaining.
     */
    ItemBuilder setCustomModelData(int customModelData);
    /**
     * Sets the tooltip style for the item being built.
     * The tooltip style is defined by a {@link Key} instance and determines the visual
     * appearance or configuration of the tooltip associated with the item.
     *
     * @param tooltipStyle the {@link Key} representing the desired tooltip style;
     *                     must not be null.
     * @return the current instance of {@link ItemBuilder} for method chaining.
     */
    ItemBuilder setTooltipStyle(Key tooltipStyle);
    /**
     * Builds and finalizes the {@link ItemBuilder}, creating an immutable representation
     * of the configured item stack.
     *
     * This method should be called after setting all desired item properties using the
     * builder's configuration methods. Once called, it generates the resulting item stack,
     * and further modifications to the builder instance will not affect the already-built item.
     *
     * @return the current instance of {@link ItemBuilder}, representing the finalized item stack.
     */
    ItemBuilder build();
    /**
     * Retrieves the {@link ItemStack} instance that has been constructed based on the
     * current configuration of the {@link ItemBuilder}.
     *
     * This method provides the fully built and finalized {@link ItemStack}, which
     * reflects all the modifications and properties set through the {@link ItemBuilder}'s
     * methods. The returned {@link ItemStack} is immutable and represents the completed
     * item configuration.
     *
     * @return the finalized {@link ItemStack} object.
     */
    ItemStack getItemStack();
}
