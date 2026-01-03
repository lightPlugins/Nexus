package io.nexstudios.nexus.bukkit.utils;

import com.willfp.eco.core.items.CustomItem;
import com.willfp.eco.core.items.Items;
import dev.lone.itemsadder.api.CustomStack;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.NexParams;
import io.nexstudios.nexus.bukkit.items.AttributeOperation;
import io.nexstudios.nexus.bukkit.items.ItemAttributeSpec;
import io.nexstudios.nexus.bukkit.items.ItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemHideFlag;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;

import java.util.*;

public class StringUtils {

    /**
     * Parses a string representation of an item and returns the corresponding ItemStack.
     * The input string must be in the format "namespace:item", where
     * "namespace" defines the category (e.g., ecoitems, minecraft, vanilla) and
     * "item" represents the specific item identifier.
     *
     * @param item a string representing the namespace and item identifier, separated by a colon
     * @return the corresponding ItemStack if the input is valid; otherwise, a default ItemStack of DEEPSLATE is returned
     */
    public static ItemStack parseItem(String item) {

        String[] itemSplit = item.split(":");

        if(itemSplit.length < 2) {
            return ItemStack.of(Material.DEEPSLATE);
        }

        switch (itemSplit[0]) {

            case "nexitems": {

                if(itemSplit.length != 2) {
                    NexusPlugin.nexusLogger.error("Could not find item with id: " + item);
                    NexusPlugin.nexusLogger.error("Please use the format: nexitems:templateid");
                    return ItemStack.of(Material.COBBLESTONE);
                }

                if(NexusPlugin.getInstance().getNexItemsHook() != null) {
                    String id = itemSplit[1];
                    return NexusPlugin.getInstance().getNexItemsHook().getItemById(id);
                } else {
                    NexusPlugin.nexusLogger.error("Could not find item with id: " + item);
                    NexusPlugin.nexusLogger.error("MMOItems is not installed on your Server!");
                    return ItemStack.of(Material.DEEPSLATE);
                }
            }

            case "mmoitems": {

                if(itemSplit.length != 3) {
                    NexusPlugin.nexusLogger.error("Could not find item with id: " + item);
                    NexusPlugin.nexusLogger.error("Please use the format: mmoitems:category:id");
                    return ItemStack.of(Material.DEEPSLATE);
                }

                if(NexusPlugin.getInstance().getMmoItemsHook() != null) {
                    String category = itemSplit[1];
                    String id = itemSplit[2];
                    return NexusPlugin.getInstance().getMmoItemsHook().getMMOItemsStack(category, id);
                } else {
                    NexusPlugin.nexusLogger.error("Could not find item with id: " + item);
                    NexusPlugin.nexusLogger.error("MMOItems is not installed on your Server!");
                    return ItemStack.of(Material.DEEPSLATE);
                }
            }

            case "ecoitems": {

                if(itemSplit.length != 2) {
                    NexusPlugin.nexusLogger.error("Could not find item with id: " + item);
                    NexusPlugin.nexusLogger.error("Please use the format: ecoitems:id");
                    return ItemStack.of(Material.DEEPSLATE);
                }

                if(NexusPlugin.getInstance().getEcoItemsHook() != null) {
                    return NexusPlugin.getInstance().getEcoItemsHook().getEcoItem(itemSplit[1]);
                } else {
                    NexusPlugin.nexusLogger.error("Could not find item with id: " + item);
                    NexusPlugin.nexusLogger.error("EcoItems is not installed on your Server!");
                    return ItemStack.of(Material.DEEPSLATE);
                }
            }
            case "minecraft", "vanilla": {

                if(itemSplit.length != 2) {
                    NexusPlugin.nexusLogger.error("Could not find item with id: " + item);
                    NexusPlugin.nexusLogger.error("Please use the format: vanilla:id or minecraft:id");
                    return ItemStack.of(Material.DEEPSLATE);
                }

                return ItemStack.of(Material.valueOf(itemSplit[1].toUpperCase()));
            }

            default: {
                NexusPlugin.nexusLogger.error("Could not find item from id system: " + item);
                NexusPlugin.nexusLogger.error("Possible values: mmoitems:category:id, ecoitems:id, vanilla:id or minecraft:id");
                return ItemStack.of(Material.DEEPSLATE);
            }
        }
    }

    /**
     * Checks if the given ItemStack is a custom item from a supported plugin.
     * Currently supports: EcoItems, MMOItems, ItemsAdder.
     *
     * @param itemStack the ItemStack to check
     * @return true if the item is recognized as a custom item; false otherwise
     */
    public static boolean isCustomItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }

        NexusPlugin nexus = NexusPlugin.getInstance();

        if(nexus.getNexItemsHook() != null) {
            if(nexus.getNexItemsHook().isNexusItem(itemStack)) {
                return true;
            }
        }

        // Check EcoItems
        if (nexus.getEcoItemsHook() != null) {
            // EcoItems speichert die ID meist in einem speziellen NBT/DataContainer.
            // Items.isCustomItem ist eine Methode in deren API.
            if (Items.isCustomItem(itemStack)) {
                return true;
            }
        }

        // Check MMOItems
        if (nexus.getMmoItemsHook() != null) {
            if (Type.get(itemStack) != null) {
                return true;
            }
        }

        // Check ItemsAdder
        if (nexus.getItemsAdderHook() != null) {
            return CustomStack.byItemStack(itemStack) != null;
        }

        return false;
    }

    /**
     * Retrieves a unique identifier for a custom item from supported plugins.
     * Returns strings like "ecoitems:id", "mmoitems:type:id", or "itemsadder:id".
     *
     * @param itemStack the ItemStack to identify
     * @return the custom ID string, or null if it's a vanilla item
     */
    @Nullable
    public static String getCustomId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }

        NexusPlugin nexus = NexusPlugin.getInstance();

        // Check NexItems
        if(nexus.getNexItemsHook() != null) {
            if(nexus.getNexItemsHook().isNexusItem(itemStack)) {
                return "nexitems:" + nexus.getNexItemsHook().getItemId(itemStack);
            }
        }

        // Check EcoItems
        if (nexus.getEcoItemsHook() != null) {
            CustomItem ecoItem = Items.getCustomItem(itemStack);
            if(ecoItem != null) {
                return "ecoitems:" + ecoItem.getKey().value().toLowerCase(Locale.ROOT);
            }
        }

        // Check MMOItems
        if (nexus.getMmoItemsHook() != null) {
            String type = MMOItems.getTypeName(itemStack);
            String id = MMOItems.getID(itemStack);

            if(type != null && id != null) {
                return "mmoitems:" + type.toLowerCase(Locale.ROOT) + ":" + id.toLowerCase(Locale.ROOT);
            }
        }

        // Check ItemsAdder
        if (nexus.getItemsAdderHook() != null) {
            CustomStack cs = CustomStack.byItemStack(itemStack);
            if (cs != null) return "itemsadder:" + cs.getNamespacedID();
        }

        return null;
    }

    public static String replaceKeyWithValue(String input, Map<String, Object> placeholders) {
        for(Map.Entry<String, Object> entry : placeholders.entrySet()) {
            input = input.replace("#" + entry.getKey() + "#", entry.getValue().toString());
        }
        return input;
    }

    public static String replaceKeyWithValue(String input, NexParams params) {
        if (params == null || params.isEmpty() || input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, String> entry : params.asMap().entrySet()) {
            String value = entry.getValue();
            result = result.replace("#" + entry.getKey() + "#", value == null ? "null" : value);
        }
        return result;
    }

    public static String parsePlaceholderAPI(OfflinePlayer offlinePlayer, String input) {

        if(NexusPlugin.getInstance().getPapiHook() != null) {
            return NexusPlugin.getInstance().getPapiHook().translateIntoString(offlinePlayer, input);
        }
        return input;
    }

    @SuppressWarnings("ApiStatus.Experimental")
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static ItemStack parseCustomItem(String itemParams) {

        String[] parts = itemParams.split(" ");

        if(parts.length == 0) {
            return null;
        }

        String[] materialParts = parts[0].split(":");
        Material material;
        // prevents IllegalArgumentException if material is not valid or not found
        try {
            material = Material.valueOf(materialParts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        if(!material.isItem()) { return null; }

        int amount = Math.min(Integer.parseInt(materialParts[1]), 99);
        if(amount < 1) { amount = 1; }

        ItemStack is = ItemStack.of(material, amount);

        for(String part : parts) {

            if(part.contains("model-data:")) {
                String[] modelDataSplit = part.split(":");
                if(modelDataSplit.length < 1) {
                    NexusPlugin.nexusLogger.error("Could not find valid model-data value for item: " + itemParams);
                    NexusPlugin.nexusLogger.error("Fallback to normal itemstack without custom model data!");
                    continue;
                }
                try {
                    float modelData = Float.parseFloat(modelDataSplit[1]);
                    is.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData
                            .customModelData()
                            .addFloat(modelData)
                            .build());
                } catch (NumberFormatException e) {
                    NexusPlugin.nexusLogger.error("Could not parse model-data for item: " + itemParams);
                    NexusPlugin.nexusLogger.error("Fallback to normal itemstack without custom model data!");
                }
            }
        }

        return is;


    }

    public static ItemStack parseConfigItem(Map<String, Object> itemParams, TagResolver resolver, Player player) {

        if(itemParams == null) {
            return new ItemStack(Material.COBBLESTONE);
        }

        try {
            // Pflichtfeld: item -> Base-Item erstellen (unterstützt z. B. "minecraft:diamond_pickaxe")
            Object rawItem = itemParams.get("item");
            if (rawItem == null) {
                return new ItemStack(Material.COBBLESTONE);
            }

            String itemId = String.valueOf(rawItem).trim();
            ItemStack baseStack = parseItem(itemId);

            // Builder with Base Item
            ItemBuilder builder = NexServices.newItemBuilder().itemStack(baseStack);

            // Optional: amount
            Object rawAmount = itemParams.get("amount");
            if (rawAmount instanceof Number n) {
                builder.amount(Math.max(1, n.intValue()));
            } else if (rawAmount instanceof String s && !s.isBlank()) {
                try { builder.amount(Math.max(1, Integer.parseInt(s.trim()))); } catch (NumberFormatException ignored) {}
            }

            // Optional: displayname
            Object rawName = itemParams.get("displayname");
            if (rawName instanceof String nameStr && !nameStr.isBlank()) {
                Component nameComp = MiniMessage.miniMessage().deserialize(nameStr);
                // keine speziellen Platzhalter -> TagResolver.empty(), Player-Kontext nicht vorhanden (null ok)
                nameComp = MiniMessageUtil.replace(
                        nameComp,
                        resolver != null ? resolver : TagResolver.empty(),
                        player
                );
                builder.displayName(nameComp);
            }

            // Optional: lore (Liste von Strings -> MiniMessageUtil.replace)
            Object rawLore = itemParams.get("lore");
            if (rawLore instanceof List<?> list) {
                List<Component> lore = new ArrayList<>();
                for (Object o : list) {
                    if (o == null) continue;
                    String line = String.valueOf(o);
                    Component comp = MiniMessage.miniMessage().deserialize(line);
                    comp = MiniMessageUtil.replace(
                            comp,
                            TagResolver.empty(),
                            null
                    );
                    lore.add(comp);
                }
                if (!lore.isEmpty()) builder.lore(lore);
            }

            // Optional: hide-flags (Strings oder {id: ...})
            Object rawHide = itemParams.get("hide-flags");
            if (rawHide instanceof List<?> hideList) {
                Set<ItemHideFlag> hideFlags = new HashSet<>();
                for (Object e : hideList) {
                    String idStr = null;
                    if (e instanceof Map<?, ?> m) {
                        Object id = m.get("id");
                        if (id != null) idStr = String.valueOf(id);
                    } else if (e != null) {
                        idStr = String.valueOf(e);
                    }
                    if (idStr == null || idStr.isBlank()) continue;
                    try {
                        hideFlags.add(ItemHideFlag.valueOf(idStr.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
                if (!hideFlags.isEmpty()) builder.hideFlags(hideFlags);
            }

            // Optional: enchantments (über Paper-Registry auflösen)
            Object rawEnch = itemParams.get("enchantments");
            if (rawEnch instanceof List<?> enchList) {
                var registry = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.ENCHANTMENT);
                Map<Enchantment, Integer> enchants = new HashMap<>();
                for (Object e : enchList) {
                    if (!(e instanceof Map<?, ?> m)) continue;
                    Object idObj = m.get("id");
                    if (idObj == null) continue;

                    String id = String.valueOf(idObj).toLowerCase(Locale.ROOT).trim();
                    NamespacedKey key = NamespacedKey.fromString(id);
                    if (key == null) key = NamespacedKey.minecraft(id);

                    Enchantment ench = registry.get(key);
                    if (ench == null) continue;

                    int lvl = 1;
                    Object lvlObj = m.get("level");
                    if (lvlObj instanceof Number n) lvl = n.intValue();
                    else if (lvlObj instanceof String s && !s.isBlank()) {
                        try { lvl = Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
                    }
                    enchants.put(ench, Math.max(1, lvl));
                }
                if (!enchants.isEmpty()) builder.enchantments(enchants);
            }

            // Optional: attributes
            Object rawAttrs = itemParams.get("attributes");
            if (rawAttrs instanceof List<?> attrList) {
                List<ItemAttributeSpec> specs = new ArrayList<>();
                for (Object e : attrList) {
                    if (!(e instanceof Map<?, ?> m)) continue;

                    Object idObj = m.get("id");
                    if (idObj == null) continue;
                    String attrId = String.valueOf(idObj).trim().toLowerCase(Locale.ROOT);
                    org.bukkit.NamespacedKey attrKey = org.bukkit.NamespacedKey.fromString(attrId);
                    if (attrKey == null) continue;

                    double amount = 0.0;
                    Object amt = m.get("amount");
                    if (amt instanceof Number n) amount = n.doubleValue();
                    else if (amt instanceof String s && !s.isBlank()) {
                        try { amount = Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) {}
                    }

                    AttributeOperation operation =
                            AttributeOperation.ADD_NUMBER;
                    Object op = m.get("operation");
                    if (op != null) {
                        String opStr = String.valueOf(op).trim().toUpperCase(Locale.ROOT);
                        operation = switch (opStr) {
                            case "ADD", "ADD_NUMBER" -> AttributeOperation.ADD_NUMBER;
                            case "ADD_SCALAR", "SCALAR" ->
                                    AttributeOperation.ADD_SCALAR;
                            case "MULTIPLY_SCALAR_1", "MULTIPLY" ->
                                    AttributeOperation.MULTIPLY_SCALAR_1;
                            default -> operation;
                        };
                    }

                    Set<EquipmentSlot> slotsSet = new HashSet<>();
                    Object slots = m.get("slots");
                    if (slots instanceof List<?> sl) {
                        for (Object s : sl) {
                            if (s == null) continue;
                            switch (String.valueOf(s).toLowerCase(Locale.ROOT)) {
                                case "hand", "mainhand", "main_hand" -> slotsSet.add(EquipmentSlot.HAND);
                                case "offhand", "off_hand" -> slotsSet.add(EquipmentSlot.OFF_HAND);
                                case "head", "helmet" -> slotsSet.add(EquipmentSlot.HEAD);
                                case "chest", "chestplate" -> slotsSet.add(EquipmentSlot.CHEST);
                                case "legs", "leggings" -> slotsSet.add(EquipmentSlot.LEGS);
                                case "feet", "boots" -> slotsSet.add(EquipmentSlot.FEET);
                            }
                        }
                    }

                    specs.add(new ItemAttributeSpec(
                            "cfg",
                            attrKey,
                            amount,
                            operation,
                            slotsSet
                    ));
                }
                if (!specs.isEmpty()) builder.attributes(specs);
            }

            // Optional: model-data-float
            Object rawModelData = itemParams.get("model-data-float");
            if (rawModelData instanceof Number n) {
                builder.modelData(n.intValue());
            } else if (rawModelData instanceof String s && !s.isBlank()) {
                try { builder.modelData((int) Math.floor(Double.parseDouble(s.trim()))); } catch (NumberFormatException ignored) {}
            }

            // Optional: item-model
            Object rawItemModel = itemParams.get("item-model");
            if (rawItemModel instanceof String s && !s.isBlank()) {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(s.trim());
                if (key != null) builder.itemModel(key);
            }

            // Optional: tooltip-model
            Object rawTooltipModel = itemParams.get("tooltip-model");
            if (rawTooltipModel instanceof String s && !s.isBlank()) {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(s.trim());
                if (key != null) builder.tooltipStyle(key);
            }

            // Optional: unbreakable
            Object rawUnbreakable = itemParams.get("unbreakable");
            if (rawUnbreakable instanceof Boolean b) {
                builder.unbreakable(b);
            } else if (rawUnbreakable instanceof String s && !s.isBlank()) {
                builder.unbreakable(Boolean.parseBoolean(s.trim()));
            }

            return builder.build();
        } catch (Exception ex) {
            NexusPlugin.nexusLogger.error("Could not parse item from Config with error: " + ex.getMessage());
            NexusPlugin.nexusLogger.error("Fallback to default itemstack: COOBLESTONE");
            // TODO: remove stacktrace here later
            ex.printStackTrace();
            return new ItemStack(Material.COBBLESTONE);
        }
    }

}
