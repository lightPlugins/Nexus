package io.nexstudios.nexus.bukkit.inventory;

import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.bukkit.inventory.models.InventoryData;
import io.nexstudios.nexus.bukkit.inventory.models.MenuItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NexusInventory extends NexusMenu {

    private InventoryData configInventory;
    private final JavaPlugin plugin;

    public NexusInventory(JavaPlugin plugin, InventoryData configInventory) {
        super(plugin, configInventory.getTitle(), configInventory.getSize());
        this.configInventory = configInventory;
        this.plugin = plugin;

        // apply the interval coming from the config
        this.updateInterval = configInventory.getUpdateInterval();

        // Initialisiere das Standard-Inventar
        populateInventory();
    }

    @Override
    protected void updateInventory() {
        // inventory.clear();
        populateInventory();

    }

    private void populateInventory() {

        // Navigation, statische Items und benutzerdefinierte Elemente verarbeiten
        // processItems(configInventory.getNavigationItems());
        processNavigationItems();
        processItems(configInventory.getStaticItems());
        processItems(configInventory.getCustomItems());

        // Dekorationselemente setzen
        if (configInventory.getDecorationItem() != null) {
            ItemStack decorationItem = configInventory.getDecorationItem();
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    setDecorativeItems(decorationItem, true);
                }
            }
        }
    }

    private void processNavigationItems() {

        ItemStack previousPageItem = configInventory.getNavigationItems().get("previous-page").getItemStack();
        ItemStack nextPageItem = configInventory.getNavigationItems().get("next-page").getItemStack();
        ItemStack closeItem = configInventory.getNavigationItems().get("close").getItemStack();

        List<Integer> previousPageSlots = configInventory.getNavigationItems().get("previous-page").getSlots();
        List<Integer> nextPageSlots = configInventory.getNavigationItems().get("next-page").getSlots();
        List<Integer> closeSlots = configInventory.getNavigationItems().get("close").getSlots();

        // Navigations
        addNavigationItems(
                previousPageItem,
                nextPageItem,
                previousPageSlots,
                nextPageSlots,
                configInventory.getNavigationItems().get("previous-page").isUsePageAsAmount()
        );

        // Close inventory
        setCloseItem(closeItem, closeSlots);

    }

    private void processItems(Map<String, InventoryData.InventoryItem> items) {
        items.forEach((namespaceKey, configItem) -> {
            ItemStack itemStack = configItem.getItemStack();

            // Setze die Items in die entsprechenden Slots
            for (int slot : configItem.getSlots()) {
                if (slot < inventory.getSize()) {
                    setItem(currentPage, slot, new MenuItem(itemStack, (event, menuItem) -> {
                        Player player = (Player) event.getWhoClicked();

                        // Beispiel: Schließe das Inventar, wenn der namespace "close" ist
                        if ("close".equals(namespaceKey)) {
                            player.closeInventory();
                        }

                    }));
                }
            }
        });
    }

    public void applyPlaceholders(Player player) {
        applyPlaceholdersToItems(player, configInventory.getNavigationItems());
        applyPlaceholdersToItems(player, configInventory.getStaticItems());
        applyPlaceholdersToItems(player, configInventory.getCustomItems());
    }

    @SuppressWarnings("UnstableApiUsage")
    private void applyPlaceholdersToItems(Player player, Map<String, InventoryData.InventoryItem> items) {
        items.forEach((key, configItem) -> {
            ItemStack itemStack = configItem.getItemStack();

            // Placeholder für Displaynamen anwenden
            Component displayName = itemStack.getData(DataComponentTypes.ITEM_NAME);
            if(displayName == null) {
                return;
            }
            String rawDisplayName = PlainTextComponentSerializer.plainText().serialize(displayName);
            Component comp = Nexus.getInstance().messageSender.stringToComponent(player, rawDisplayName);
            itemStack.setData(DataComponentTypes.ITEM_NAME, comp);

            // Placeholder für Lore anwenden
            if(itemStack.hasData(DataComponentTypes.LORE)) {
                List<Component> newLore = new ArrayList<>();
                ItemLore itemLore = itemStack.getData(DataComponentTypes.LORE);
                if(itemLore != null) {
                    for (Component line : itemLore.lines()) {
                        String rawLoreLine = PlainTextComponentSerializer.plainText().serialize(line);
                        Component loreLine = Nexus.getInstance().messageSender.stringToComponent(player, rawLoreLine);
                        newLore.add(loreLine);
                    }
                    itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(newLore));
                }
            }
        });
    }

    public void reloadConfig(InventoryData newConfigInventory) {
        this.configInventory = newConfigInventory;
        updateInventory();
    }

}
