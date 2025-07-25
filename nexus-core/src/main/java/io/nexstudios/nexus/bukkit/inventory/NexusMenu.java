package io.nexstudios.nexus.bukkit.inventory;

import io.nexstudios.nexus.bukkit.inventory.models.MenuItem;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class NexusMenu implements InventoryHolder {


    protected Inventory inventory;
    protected JavaPlugin plugin;
    protected Map<Integer, MenuItem> menuItems;
    private final Map<Integer, Map<Integer, MenuItem>> itemsPerPage = new HashMap<>();

    protected int updateInterval = 3;
    protected int currentPage = 1; // Standard ist Seite 1
    protected int totalPages = 1;
    private final Map<UUID, Integer> clickTracker = new HashMap<>();
    private final Map<UUID, Long> cooldownTracker = new HashMap<>();

    private int updaterTaskId = -1;

    public NexusMenu(JavaPlugin plugin, Component title, int size) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, size * 9, title);
        this.menuItems = new HashMap<>();
        this.currentPage = 1;
        this.totalPages = 1;
    }

    public void setItem(int page, int slot, MenuItem item) {
        if (page < 0 || page > totalPages) {
            throw new IllegalArgumentException("Invalid page number " + page + " > " + totalPages);
        }
        int adjustedSlot = page * inventory.getSize() + slot;
        menuItems.put(adjustedSlot, item);
        if (page == currentPage) {
            inventory.setItem(slot, item.getItemStack());
        }
    }

    /**
     * Setzt eine Liste von Items in einem definierten Bereich und verteilt sie
     * bei Bedarf auf mehrere Seiten. Aktualisiert auch die Gesamtseitenzahl.
     *
     * @param items     Die Liste der MenuItems, die in den Bereich gesetzt werden sollen.
     * @param startSlot Der erste Slot des Bereichs (z. B. 19).
     * @param endSlot   Der letzte Slot des Bereichs (z. B. 34).
     */
    public void setItemList(List<MenuItem> items, int startSlot, int endSlot) {
        // Überprüfen, ob Start- und Endslots gültig sind
        if (startSlot < 0 || endSlot >= inventory.getSize() || startSlot > endSlot) {
            throw new IllegalArgumentException("Start and end slots must be within the inventory size and start slot must not be greater than end slot.");
        }

        // Standard-Breite des Inventars (9 Slots pro Reihe)
        int inventoryWidth = 9;

        // Berechnung: Anzahl der Spalten und Reihen
        int columnsInRow = (endSlot % inventoryWidth) - (startSlot % inventoryWidth) + 1;
        int startRow = startSlot / inventoryWidth;
        int endRow = endSlot / inventoryWidth;
        int availableRows = endRow - startRow + 1;

        // Maximale Slots pro Seite berechnen
        int maxSlotsPerPage = columnsInRow * availableRows;

        // Gesamtseitenzahl berechnen
        this.totalPages = (int) Math.ceil((double) items.size() / maxSlotsPerPage);

        // Globaler Fortschritts-Index für die Items
        int currentItemIndex = 0;

        // Vorherigen Seiteninhalt leeren
        itemsPerPage.clear();

        // Alle Seiten berechnen und speichern
        for (int page = 1; page <= totalPages; page++) {
            // Map für die aktuelle Seite initialisieren
            Map<Integer, MenuItem> pageItems = new HashMap<>();

            // Lokal auf dieser Seite alle Slots durchlaufen
            for (int slotIndex = 0; slotIndex < maxSlotsPerPage; slotIndex++) {

                // Prüfen, ob alle Items bereits platziert wurden
                if (currentItemIndex >= items.size()) {
                    break;
                }

                // Berechnung der Slot-Position (Reihe und Spalte)
                int row = (slotIndex / columnsInRow) + startRow;
                int column = (slotIndex % columnsInRow) + (startSlot % inventoryWidth);
                int slot = row * inventoryWidth + column;

                // Überspringen, wenn der Slot außerhalb des gültigen Bereichs liegt
                if (slot > endSlot) {
                    continue;
                }

                // Hole das aktuelle Item und speichere es
                MenuItem item = items.get(currentItemIndex);
                pageItems.put(slot, item);

                // Fortschritt aktualisieren
                currentItemIndex++;
            }

            // Seite speichern
            itemsPerPage.put(page, pageItems);

            // Keine weiteren Seiten mehr, wenn alle Items verarbeitet wurden
            if (currentItemIndex >= items.size()) {
                break;
            }
        }
    }

    private void loadPage(int page) {
        if (!itemsPerPage.containsKey(page)) {
            return;
        }

        // Slots im derzeit sichtbaren Bereich leeren
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Die gespeicherten Items für diese Seite holen
        Map<Integer, MenuItem> pageItems = itemsPerPage.get(page);

        // Items in den entsprechenden Slots setzen
        for (Map.Entry<Integer, MenuItem> entry : pageItems.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
        }
    }

    public void setItem(List<Integer> pages, int slot, MenuItem item) {
        for (int page : pages) {
            if (page < 0 || page >= totalPages) {
                throw new IllegalArgumentException("Invalid page number: " + page);
            }
            int adjustedSlot = page * inventory.getSize() + slot;
            menuItems.put(adjustedSlot, item);
            if (page == currentPage) {
                inventory.setItem(slot, item.getItemStack());
            }
        }
    }

    public void setItems(List<Integer> pages, List<Integer> slots, MenuItem item) {
        if (pages == null || pages.isEmpty()) {
            // Wenn keine spezifischen Seiten angegeben sind, setzen wir das Item in alle Seiten.
            for (int page = 0; page < totalPages; page++) {
                setItemsInPage(page, slots, item);
            }
        } else {
            // Wenn Seiten angegeben werden, iterieren wir nur über diese.
            for (int page : pages) {
                setItemsInPage(page, slots, item);
            }
        }
    }

    public void open(Player player) {
        updateInventory();
        loadPage(currentPage);
        updateInventory();
        player.openInventory(inventory);
        // Starte den Update-Timer, wenn er nicht läuft
        if (updaterTaskId == -1) {
            startUpdateTimer();
        }

    }

    public void handleMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        if (cooldownTracker.containsKey(playerId) && cooldownTracker.get(playerId) > System.currentTimeMillis()) {
            event.setCancelled(true);
            return;
        }

        clickTracker.put(playerId, clickTracker.getOrDefault(playerId, 0) + 1);

        if (clickTracker.get(playerId) > 20) {
            cooldownTracker.put(playerId, System.currentTimeMillis() + 1000);
            clickTracker.put(playerId, 0);
            event.setCancelled(true);
            return;
        }

        int slot = event.getSlot() + (currentPage * inventory.getSize());
        if (menuItems.containsKey(slot)) {
            menuItems.get(slot).onClick(event);
        }

        // check null on non-multi page actions
        // coming from setItemList()
        if(itemsPerPage.get(currentPage) == null) {
            return;
        }
        // check null on non-multiple page actions
        // coming from setItemList()
        if(itemsPerPage.get(currentPage).get(event.getSlot()) == null) {
            return;
        }

        itemsPerPage.get(currentPage).get(event.getSlot()).onClick(event);

        GlobalRegionScheduler globalScheduler = plugin.getServer().getGlobalRegionScheduler();
        globalScheduler.runDelayed(plugin, scheduledTask -> {
            int clicks = clickTracker.getOrDefault(playerId, 0) - 1;
            if (clicks <= 0) {
                clickTracker.remove(playerId);
            } else {
                clickTracker.put(playerId, clicks);
            }
        }, 20);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
    protected abstract void updateInventory();

    public void nextPage(Player player) {
        if (currentPage < totalPages) {
            currentPage++;
            loadPage(currentPage);
            updateInventory();
        }
    }


    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadPage(currentPage);
            updateInventory();
        }
    }

    protected void addNavigationItems(
            ItemStack previousPageItem,
            ItemStack nextPageItem,
            List<Integer> slotPrevious,
            List<Integer> slotNext,
            boolean pageAsAmount) {

        // only apply navigation items if there are more than 1 page
        if(totalPages <= 1) {
            return;
        }

        // Apply stack amount in terms of current page
        if(pageAsAmount) {
            nextPageItem.setAmount(currentPage + 1);
            previousPageItem.setAmount(currentPage - 1);
        }

        setItems(List.of(currentPage), slotPrevious, new MenuItem(null, (event, item) -> {}));
        setItems(List.of(currentPage), slotNext, new MenuItem(null, (event, item) -> {}));

        if(totalPages > 1 && currentPage < totalPages) {
            setItems(List.of(currentPage), slotNext, new MenuItem(nextPageItem, (event, item) -> {
                nextPage((Player) event.getWhoClicked());
            }));
        }

        if(currentPage > 1) {
            // Add next page item
            setItems(List.of(currentPage), slotPrevious, new MenuItem(previousPageItem, (event, item) -> {
                previousPage();
            }));
        }
    }

    protected void setCloseItem(ItemStack closeItem, List<Integer> slots) {
        setItems(List.of(currentPage), slots, new MenuItem(closeItem, (event, item) -> {
            Player player = (Player) event.getWhoClicked();
            player.closeInventory();
        }));
    }

    protected void setDecorativeItems(ItemStack decorativeItem, boolean use) {

        // check, if we should add deco items
        if(!use) {
            return;
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, decorativeItem);
            }
        }
    }

    /**
     * Startet einen regelmäßigen Timer, der alle 1 Sekunde `updateInventory()` aufruft.
     */
    private void startUpdateTimer() {
        updaterTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                () -> {
                    if (inventory.getViewers().isEmpty()) {
                        stopUpdateTimer(); // Stoppe den Timer, wenn keine Spieler das Menü geöffnet haben
                        return;
                    }
                    updateInventory(); // Aktualisiere das Inventar
                },
                0L, // Sofort starten
                updateInterval * 20L// Alle 20 Ticks = 1 Sekunde
        );
    }

    /**
     * Beendet den Timer, wenn kein Spieler mehr das Menü geöffnet hat.
     */
    private void stopUpdateTimer() {
        if (updaterTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updaterTaskId);
            updaterTaskId = -1;
        }
    }

    /**
     * Hilfsmethode: Setzt ein Item in mehrere Slots auf einer angegebenen Seite.
     *
     * @param page  Die Seite, auf der die Items gesetzt werden sollen.
     * @param slots Die Slots, in denen die Items gesetzt werden sollen.
     * @param item  Das MenuItem, das gesetzt werden soll.
     */
    private void setItemsInPage(int page, List<Integer> slots, MenuItem item) {
        for (int slot : slots) {
            setItem(page, slot, item); // Bereits vorhandene Methode wird verwendet.
        }
    }
}
