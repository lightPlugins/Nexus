package io.nexstudios.nexus.bukkit.inventory.models;

import io.nexstudios.nexus.bukkit.Nexus;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.component.DataComponentType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryData {

    private Component title;
    private final FileConfiguration fileConfiguration;
    private final int size;
    private final int updateInterval;
    private final ItemStack decorationItem;
    private final Map<String, InventoryItem> navigationItems;
    private final Map<String, InventoryItem> staticItems;
    private final Map<String, InventoryItem> customItems;
    private final ConfigurationSection extraSettings;

    public InventoryData(FileConfiguration config) {
        // Basiswerte aus der Konfiguration laden
        this.title = colorize(config.getString("title", "Default Title"));
        this.fileConfiguration = config;
        this.size = config.getInt("size", 6);
        this.updateInterval = config.getInt("update-intervall", 2);

        // Dekorationselement
        if (config.getBoolean("content.decoration.enable", false)) {
            String decorationMaterial = config.getString("content.decoration.item", "BLACK_STAINED_GLASS_PANE");
            this.decorationItem = createSimpleItem(Material.matchMaterial(decorationMaterial));
        } else {
            this.decorationItem = null;
        }

        // Navigations-Items laden
        this.navigationItems = loadItems(config, "content.navigation");

        // Statische Items laden
        this.staticItems = loadItems(config, "content.static");

        // Benutzerdefinierte Items laden
        this.customItems = loadItems(config, "content.custom");

        // read extra settings from the inventory file
        this.extraSettings = config.getConfigurationSection("content.extra-settings");
    }

    /**
     * Lädt Items basierend auf einem Konfigurationspfad.
     *
     * @param config Die Konfigurationsdatei.
     * @param path   Der Pfad im YAML.
     * @return Eine Map der geladenen Items.
     */
    private Map<String, InventoryItem> loadItems(FileConfiguration config, String path) {
        Map<String, InventoryItem> items = new HashMap<>();
        if (config.contains(path)) {
            for (String key : config.getConfigurationSection(path).getKeys(false)) {
                InventoryItem item = new InventoryItem(config, path + "." + key);
                items.put(key, item);
            }
        }
        return items;
    }

    public Component replaceTitle(Player player, Map<String, String> replacements) {
        // Serialize the Component to a MiniMessage-compatible String
        String titleString = MiniMessage.miniMessage().serialize(title);

        // Apply replacements
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            titleString = titleString.replace(entry.getKey(), entry.getValue());
        }

        // Deserialize the modified String back to a Component
        Component finalTitle = MiniMessage.miniMessage().deserialize(titleString)
                .decoration(TextDecoration.ITALIC, false);

        // Update the title and send it to the player
        this.title = finalTitle;

        return finalTitle;
    }

    /**
     * Erstellt ein einfaches Dekorationselement.
     *
     * @param material Das Material für den ItemStack.
     * @return Der erstellte ItemStack.
     */
    private ItemStack createSimpleItem(Material material) {
        if (material == null) material = Material.STONE; // Fallback-Material
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * Färbt einen Text mithilfe der AdventureAPI.
     *
     * @param text Der zu färbende Text.
     * @return Der gefärbte Text als String.
     */
    private Component colorize(String text) {
        if (text == null) return Component.text("Not Found");
        return MiniMessage.miniMessage().deserialize(text)
                .decoration(TextDecoration.ITALIC, false);
    }


    @Getter
    // @API.Status(Experimental) for Item DataComponents
    // @SuppressWarnings("UnstableApiUsage")
    public static class InventoryItem {
        private final List<Integer> slots;
        private final int page;
        private final boolean usePageAsAmount;
        private final ItemStack itemStack;

        public InventoryItem(FileConfiguration config, String path) {
            // Slots auslesen
            this.slots = config.getIntegerList(path + ".slots").stream()
                    .map(slot -> slot - 1)
                    .toList();
            this.page = config.getInt(path + ".page", 1);
            this.usePageAsAmount = config.getBoolean(path + ".use-page-number-as-amount", false);

            // Material laden
            String materialName = config.getString(path + ".item", "STONE");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                Nexus.nexusLogger.error(List.of(
                        "Material " + materialName + " in "
                                + config.getName().replace(".yml", " ") + " - " + path,
                        "could not be found! Please use a valid material name!"
                ));
                throw new IllegalArgumentException("Material " + materialName + " in " + path + " not found!");
            }

            // ItemStack erstellen
            this.itemStack = new ItemStack(material);

            // Meta-Daten setzen
            ItemMeta meta = this.itemStack.getItemMeta();
            if (meta != null) {
                // Displayname
                String displayName = config.getString(path + ".display-name");
                if (displayName != null) {
                    Component displayNameComponent = MiniMessage.miniMessage().deserialize(displayName)
                            .decoration(TextDecoration.ITALIC, false);
                    meta.displayName(displayNameComponent);
                }

                // Lore
                List<String> lore = config.getStringList(path + ".lore");
                if (!lore.isEmpty()) {
                    List<Component> loreComponents = new ArrayList<>();
                    for (String line : lore) {
                        loreComponents.add(MiniMessage.miniMessage().deserialize(line)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                    meta.lore(loreComponents);
                }

                // TODO: Reword the new custom model data system ItemStack#setData(DataComponentType, DataComponent)
//                if (config.contains(path + ".custom-model-data")) {
//                    // meta.setCustomModelData(config.getInt(path + ".custom-model-data"));
//                    meta.setItemModel();
//                    itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
//                            .addString(0.5f)
//                            .addFlag(true)
//                            .build());
//                }

                this.itemStack.setItemMeta(meta);
            }
        }
    }
}
