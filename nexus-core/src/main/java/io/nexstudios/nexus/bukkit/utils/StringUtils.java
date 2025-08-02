package io.nexstudios.nexus.bukkit.utils;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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
        ItemStack is;

        if(itemSplit.length != 2) {
            return ItemStack.of(Material.DEEPSLATE);
        }

        switch (itemSplit[0]) {

            case "ecoitems": {
                if(NexusPlugin.getInstance().getEcoItemsHook() != null) {
                    return NexusPlugin.getInstance().getEcoItemsHook().getEcoItem(itemSplit[1]);
                }
            }
            case "minecraft": {
                return ItemStack.of(Material.valueOf(itemSplit[1].toUpperCase()));
            }
            case "vanilla": {
                return ItemStack.of(Material.valueOf(itemSplit[1].toUpperCase()));
            }

            default: {
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
