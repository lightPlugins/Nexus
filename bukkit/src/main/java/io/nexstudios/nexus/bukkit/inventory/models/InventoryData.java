package io.nexstudios.nexus.bukkit.inventory.models;

import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Getter
@Setter
public class InventoryData {

    private Component title;
    private NexusLanguage nexusLanguage;
    private final FileConfiguration fileConfiguration;
    private final int size;
    private final int updateInterval;
    private final ItemStack decorationItem;
    private Map<String, InventoryItem> navigationItems;
    private Map<String, InventoryItem> staticItems;
    private Map<String, InventoryItem> customItems;
    private final ConfigurationSection extraSettings;
    private final String inventoryID;

    public InventoryData(FileConfiguration config, NexusLanguage nexusLanguage, String inventoryID) {
        // Basiswerte aus der Konfiguration laden
        this.title = colorize(config.getString("title", "Default Title"));
        this.fileConfiguration = config;
        this.inventoryID = inventoryID;
        this.nexusLanguage = nexusLanguage;
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

    @SuppressWarnings("UnstableApiUsage")
    public void updateLanguage(UUID uuid) {
        // Aktualisiert die Titelkomponente basierend auf der aktuellen Sprache
        this.title = nexusLanguage.getTranslation(uuid,
                "inventories." + inventoryID + ".title", false);

        // Aktualisiert die Navigations-Items
        for (InventoryItem item : navigationItems.values()) {
            Component displayName = nexusLanguage.getTranslation(uuid,
                    "inventories." + inventoryID + ".content." + item.getKey() + ".display-name", false);
            item.getItemStack().setData(DataComponentTypes.ITEM_NAME, displayName);
            List<Component> lore = nexusLanguage.getTranslationList(uuid,
                    "inventories." + inventoryID + ".content." + item.getKey() + ".lore", false);
            item.getItemStack().setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        }

        // Aktualisiert die statischen Items
        for (InventoryItem item : staticItems.values()) {
            Component displayName = nexusLanguage.getTranslation(uuid,
                    "inventories." + inventoryID + ".content." + item.getKey() + ".display-name", false);
            item.getItemStack().setData(DataComponentTypes.ITEM_NAME, displayName);
            List<Component> lore = nexusLanguage.getTranslationList(uuid,
                    "inventories." + inventoryID + ".content." + item.getKey() + ".lore", false);
            item.getItemStack().setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        }

        // Aktualisiert die benutzerdefinierten Items
        for (InventoryItem item : customItems.values()) {
            Component displayName = nexusLanguage.getTranslation(uuid,
                    "inventories." + inventoryID + ".content." + item.getKey() + ".display-name", false);
            item.getItemStack().setData(DataComponentTypes.ITEM_NAME, displayName);
            List<Component> lore = nexusLanguage.getTranslationList(uuid,
                    "inventories." + inventoryID + ".content." + item.getKey() + ".lore", false);
            item.getItemStack().setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        }
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
            ConfigurationSection section = config.getConfigurationSection(path);
            if(section == null) {
                Nexus.nexusLogger.error(List.of(
                        "Error loading items from path: " + path,
                        "Configuration section is null. Please check the configuration file."
                ));
                return items; // Return empty map if section is null
            }
            for (String key : section.getKeys(false)) {
                InventoryItem item = new InventoryItem(config, path + "." + key, key, nexusLanguage, inventoryID);
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
    @SuppressWarnings("UnstableApiUsage")
    private ItemStack createSimpleItem(Material material) {
        if (material == null) material = Material.STONE; // Fallback-Material

        ItemStack stack = new ItemStack(material);
        // stack.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        stack.setData(DataComponentTypes.ITEM_NAME, Component.text(""));
        return stack;
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
    @SuppressWarnings("UnstableApiUsage")
    public static class InventoryItem implements Cloneable {
        private final List<Integer> slots;
        private final int page;
        private final boolean usePageAsAmount;
        private final ItemStack itemStack;
        private final String key;

        @Override
        public InventoryItem clone() {
            try {
                // Falls `ItemStack` tiefer geklont werden muss, hier anpassen
                return (InventoryItem) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError("Cloning not supported", e);
            }
        }

        public InventoryItem(FileConfiguration config, String path, String key, NexusLanguage nexusLanguage, String inventoryID) {
            // Slots auslesen
            this.slots = config.getIntegerList(path + ".slots").stream()
                    .map(slot -> slot - 1)
                    .toList();
            this.page = config.getInt(path + ".page", 1);
            this.key = key;
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

            // Displayname
            Component displayNameComponent = nexusLanguage.getTranslation(nexusLanguage.getConsoleUUID(),
                    "inventories." + inventoryID + ".content." + key + ".display-name", false);
            this.itemStack.setData(DataComponentTypes.ITEM_NAME, displayNameComponent);

            // Lore
            List<Component> loreComponents = nexusLanguage.getTranslationList(nexusLanguage.getConsoleUUID(),
                    "inventories." + inventoryID + ".content." + key + ".lore",
                    false);
            itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(loreComponents));
        }
    }
}
