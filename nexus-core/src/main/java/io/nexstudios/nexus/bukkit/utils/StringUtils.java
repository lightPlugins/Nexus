package io.nexstudios.nexus.bukkit.utils;

import io.nexstudios.nexus.bukkit.NexusPlugin;
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
                }
            }
            case "minecraft": {

                if(itemSplit.length != 2) {
                    NexusPlugin.nexusLogger.error("Could not find item with id: " + item);
                    NexusPlugin.nexusLogger.error("Please use the format: vanilla:id or minecraft:id");
                    return ItemStack.of(Material.DEEPSLATE);
                }

                return ItemStack.of(Material.valueOf(itemSplit[1].toUpperCase()));
            }
            case "vanilla": {

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
}
