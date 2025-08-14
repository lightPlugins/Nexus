package io.nexstudios.nexus.bukkit.utils;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

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

    public static String parsePlaceholderAPI(OfflinePlayer offlinePlayer, String input) {

        if(NexusPlugin.getInstance().getPapiHook() != null) {
            return NexusPlugin.getInstance().getPapiHook().translateIntoString(offlinePlayer, input);
        }
        return input;
    }

    @SuppressWarnings("ApiStatus.Experimental")
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
}
