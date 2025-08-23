package io.nexstudios.nexus.bukkit.inv;

import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import io.nexstudios.nexus.bukkit.inv.fill.InvFillStrategy;
import io.nexstudios.nexus.bukkit.inv.pagination.NexPageSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class NexInventoryView {

    private final NexInventory inv;
    private final Player player;
    private Inventory top; // nicht final: wird beim Open korrekt gesetzt
    private int pageIndex = 0;

    private List<Object> bodyModels = Collections.emptyList();
    private NexOnClick bodyClickHandler;

    // Slot -> statischer Klick-Handler (Navigation/Required/Custom/Extra)
    private final Map<Integer, NexOnClick> staticClickHandlers = new HashMap<>();
    // Slot -> Namespace (z. B. navigation:*, required:*, custom:*, extra)
    private final Map<Integer, String> staticNamespaces = new HashMap<>();
    // Body-Index (pro Seite) -> Handler (normalerweise bodyClickHandler)
    private final Map<Integer, NexOnClick> bodyHandlersPerIndex = new HashMap<>();

    // NEU: Handler-Maps
    private final Map<String, NexOnClick> requiredHandlers = new HashMap<>();
    private NexOnClick requiredGlobal;

    private final Map<String, NexOnClick> customHandlers = new HashMap<>();
    private NexOnClick customGlobal;

    private final Map<String, NexOnClick> navigationHandlers = new HashMap<>();
    private NexOnClick navigationGlobal;

    // Optional: Override der Body-Zone/Alignment (für populateFiller(...))
    private InvFillStrategy.BodyZone overrideZone;
    private InvAlignment overrideAlignment;

    private final Map<Integer, Integer> staticPriorities = new HashMap<>();


    public NexInventoryView(NexInventory inv, Player player) {
        this.inv = inv;
        this.player = player;
        this.top = inv.getInventory();
    }


    public Player player() { return player; }

    public void open() {
        String titleStr = inv.titleSupplier().apply(player);
        this.top = Bukkit.createInventory(inv, inv.size(), titleStr != null ? titleStr : "Inventory");
        player.openInventory(this.top);
        NexInventoryManager.get().register(player.getUniqueId(), this);
        renderAll();
    }


    public void setBodyItems(List<?> models, NexOnClick clickHandler) {
        this.bodyModels = (List<Object>) (models != null ? models : Collections.emptyList());
        this.bodyClickHandler = clickHandler;
        this.pageIndex = 0;
        renderAll();
    }

    public void nextPage() {
        NexPageSource ps = inv.pageSourceFor(bodyModels.size());
        int newIndex = ps.clampPage(pageIndex + 1);
        if (newIndex != pageIndex) {
            pageIndex = newIndex;
            renderBodyOnly();
            renderNavigation();
        }
    }

    public void prevPage() {
        NexPageSource ps = inv.pageSourceFor(bodyModels.size());
        int newIndex = ps.clampPage(pageIndex - 1);
        if (newIndex != pageIndex) {
            pageIndex = newIndex;
            renderBodyOnly();
            renderNavigation();
        }
    }

    // Rendering

    private void renderAll() {
        clearAll();
        placeDecoration();
        renderNavigation();
        renderRequired();
        renderCustom();
        renderBodyOnly();
    }


    private void clearAll() {
        for (int i = 0; i < inv.size(); i++) top.setItem(i, null);
        staticClickHandlers.clear();
        staticNamespaces.clear();
        bodyHandlersPerIndex.clear();
        staticPriorities.clear(); // NEU
    }


    private void placeDecoration() {
        if (!inv.decorationEnabled()) return;
        ItemStack deco = renderItemSpec(inv.decorationItemSpec(), null);
        if (deco == null) return;
        // Deko füllt alles – wird später von höher priorisierten Items überschrieben
        for (int i = 0; i < inv.size(); i++) {
            top.setItem(i, deco);
        }
    }

    private void renderNavigation() {
        Map<String, NexItemConfig> nav = inv.navigation();
        NexItemConfig prev = nav.get("previous-page");
        NexItemConfig next = nav.get("next-page");
        NexItemConfig close = nav.get("close");

        NexPageSource ps = inv.pageSourceFor(bodyModels.size());
        boolean hasPrev = pageIndex > 0;
        boolean hasNext = pageIndex < (ps.totalPages() - 1);

        if (prev != null && hasPrev) {
            placeStaticItem(prev, "navigation:previous-page", (e, ctx) -> {
                prevPage(); // Standardaktion
                fireNavigationCustom("previous-page", e, ctx);
            });
        }
        if (next != null && hasNext) {
            placeStaticItem(next, "navigation:next-page", (e, ctx) -> {
                nextPage(); // Standardaktion
                fireNavigationCustom("next-page", e, ctx);
            });
        }
        if (close != null) {
            placeStaticItem(close, "navigation:close", (e, ctx) -> {
                player.closeInventory(); // Standardaktion
                fireNavigationCustom("close", e, ctx);
            });
        }
    }
    
    private void fireNavigationCustom(String id, InventoryClickEvent e, NexClickContext ctx) {
        NexOnClick h = navigationHandlers.get(id);
        if (h != null) h.onClick(e, ctx);
        if (navigationGlobal != null) navigationGlobal.onClick(e, ctx);
    }



    private void renderRequired() {
        for (NexItemConfig cfg : inv.required()) {
            String ns = "required:" + cfg.id;
            NexOnClick target = requiredHandlers.get(cfg.id);
            if (target == null) target = requiredGlobal != null ? requiredGlobal : (e, c) -> {};
            placeStaticItem(cfg, ns, target);
        }
    }


    private void renderCustom() {
        for (NexItemConfig cfg : inv.custom()) {
            String ns = "custom:" + cfg.id;
            NexOnClick target = customHandlers.get(cfg.id);
            if (target == null) target = customGlobal != null ? customGlobal : (e, c) -> {};
            placeStaticItem(cfg, ns, target);
        }
    }


    private void renderBodyOnly() {
        InvFillStrategy.BodyZone zone = (overrideZone != null ? overrideZone : inv.bodyZone());
        InvAlignment align = (overrideAlignment != null ? overrideAlignment : inv.bodyAlignment());

        // Lokale Page-Berechnung basierend auf aktiver Zone
        int pageSize = Math.max(1, zone.slots.size());
        int totalItems = bodyModels.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        if (pageIndex >= totalPages) pageIndex = totalPages - 1;
        if (pageIndex < 0) pageIndex = 0;

        int pageOffset = pageIndex * pageSize;
        int itemsOnPage = Math.max(0, Math.min(pageSize, totalItems - pageOffset));

        // WICHTIG: Body-Zone nur leeren, wenn wir auch wirklich Body-Items setzen.
        if (itemsOnPage <= 0) {
            // Keine Filler/Body-Inhalte -> Deko und statische Items bleiben unangetastet.
            return;
        }

        // Body-Zone leeren
        for (int slot : zone.slots) {
            top.setItem(slot, null);
        }
        bodyHandlersPerIndex.clear();

        Map<Integer, Integer> map = inv.fillStrategy().assignSlots(
                zone, pageIndex, pageSize, pageOffset, itemsOnPage, align
        );

        // Filler setzen
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            int slot = e.getKey();
            int bodyIndexInPage = e.getValue();
            Object model = bodyModels.get(pageOffset + bodyIndexInPage);

            ItemStack stack = renderBodyModel(model);
            if (stack != null) {
                top.setItem(slot, stack);
                if (bodyClickHandler != null) {
                    bodyHandlersPerIndex.put(bodyIndexInPage, bodyClickHandler);
                }
            }
        }

        // Restlöcher in der Body-Zone wieder mit Deko füllen (falls aktiviert)
        if (inv.decorationEnabled()) {
            ItemStack deco = renderItemSpec(inv.decorationItemSpec(), null);
            if (deco != null) {
                for (int slot : zone.slots) {
                    if (top.getItem(slot) == null) {
                        top.setItem(slot, deco);
                    }
                }
            }
        }
    }

    private void placeStaticItem(NexItemConfig cfg, String namespace, NexOnClick override) {
        ItemStack stack = inv.renderer().renderStatic(cfg, inv.inventoryId());
        if (stack == null) return;

        int newRank = rankForNamespace(namespace);

        for (Integer s1b : cfg.slots1b) {
            int s0 = Math.max(0, s1b - 1);
            if (s0 < 0 || s0 >= inv.size()) continue;

            // Prioritätsprüfung: nur setzen, wenn Slot leer oder niedrigere Priorität
            Integer existingRank = staticPriorities.get(s0);
            if (existingRank != null && existingRank >= newRank) {
                // vorhandenes Item hat höhere/gleiche Priorität -> nicht überschreiben
                continue;
            }

            top.setItem(s0, stack);
            NexOnClick h = (override != null) ? override : (event, ctx) -> {};
            staticClickHandlers.put(s0, h);
            staticNamespaces.put(s0, namespace);
            staticPriorities.put(s0, newRank);
        }
    }

    private int rankForNamespace(String ns) {
        if (ns == null) return -1;
        if (ns.startsWith("navigation:")) return 3;
        if (ns.startsWith("required:"))   return 2;
        if (ns.startsWith("custom:"))     return 1;
        if ("extra".equals(ns))           return 0;
        return -1; // Deko/Unbekannt
    }


    public void bindRequired(String idOrNull, NexOnClick handler) {
        if (handler == null) return;
        if (idOrNull == null) { requiredGlobal = handler; }
        else { requiredHandlers.put(idOrNull, handler); }
        // Re-render der Required-Items
        renderRequired();
    }

    public void bindCustom(String idOrNull, NexOnClick handler) {
        if (handler == null) return;
        if (idOrNull == null) { customGlobal = handler; }
        else { customHandlers.put(idOrNull, handler); }
        renderCustom();
    }

    public void bindNavigation(String idOrNull, NexOnClick handler) {
        if (handler == null) return;
        if (idOrNull == null) { navigationGlobal = handler; }
        else { navigationHandlers.put(idOrNull, handler); }
        renderNavigation();
    }

    public void populateFillerStacks(List<ItemStack> items, int startSlot1b, int endSlot1b, InvAlignment alignment,
                                     NexOnClick clickHandler) {
        if (items == null) items = List.of();

        // Normalisiere Range
        int start1 = Math.max(1, Math.min(startSlot1b, endSlot1b));
        int end1   = Math.max(start1, Math.max(startSlot1b, endSlot1b));

        // 1) Rechteckige BodyZone aus der Range ableiten:
        // linke/rechte Grenzen symmetrisch anhand der linken Randbreite (startCol)
        int start0 = start1 - 1;
        int end0   = end1 - 1;

        int startRow = start0 / 9;
        int endRow   = end0   / 9;

        int leftMargin = start0 % 9;        // Anzahl Spalten links (0-basiert)
        int rightCol   = 8 - leftMargin;    // symmetrische rechte Grenze

        // Rechteckige Slotliste aufbauen (alle Zeilen gleich breite Spalten von leftMargin..rightCol)
        List<Integer> slots0b = new ArrayList<>();
        for (int row = startRow; row <= endRow; row++) {
            for (int col = leftMargin; col <= rightCol; col++) {
                int s0 = row * 9 + col;
                if (s0 >= 0 && s0 < inv.size()) {
                    slots0b.add(s0);
                }
            }
        }

        // 2) BodyZone/Alignment setzen (Alignment ist für rechteckige Zone nicht mehr kritisch,
        //    wir lassen es aber bestehen, falls du später andere Strategien nutzt)
        this.overrideZone = new InvFillStrategy.BodyZone(
                Math.max(1, endRow - startRow + 1),
                Math.max(1, rightCol - leftMargin + 1),
                java.util.Collections.unmodifiableList(slots0b)
        );
        this.overrideAlignment = alignment;

        // 3) Modelle/Handler übernehmen
        this.bodyModels = new ArrayList<>(items);
        this.bodyClickHandler = clickHandler;

        // 4) Rendern
        renderBodyOnly();
        renderNavigation();
    }


    public void addExtraItem(ItemStack stack, int[] slots1b, NexOnClick handler) {
        if (stack == null || slots1b == null || slots1b.length == 0) return;
        int newRank = rankForNamespace("extra");
        for (int s1 : slots1b) {
            int s0 = Math.max(0, s1 - 1);
            if (s0 < 0 || s0 >= inv.size()) continue;

            Integer existingRank = staticPriorities.get(s0);
            if (existingRank != null && existingRank >= newRank) {
                continue; // höher priorisiertes Item vorhanden
            }

            top.setItem(s0, stack);
            staticClickHandlers.put(s0, handler != null ? handler : (e, c) -> {});
            staticNamespaces.put(s0, "extra");
            staticPriorities.put(s0, newRank);
        }
    }


    private ItemStack renderItemSpec(String spec, NexItemConfig cfg) {
        return inv.renderer().renderStatic(
                cfg != null ? cfg :
                        new NexItemConfig("deco", "decoration", spec, 1, List.of(),
                                false, null, null, null, null, null,
                                List.of(), List.of(), List.of(), Map.of()),
                inv.inventoryId()
        );
    }

    private ItemStack renderBodyModel(Object model) {
        // Filler unterstützt direkte ItemStacks; optional auch NexItemConfig
        if (model instanceof ItemStack is) {
            return is;
        }
        if (model instanceof NexItemConfig cfg) {
            return inv.renderer().renderStatic(cfg, inv.inventoryId());
        }
        return null;
    }


    // Dispatch aus Listener

    public void handleClick(InventoryClickEvent event, int topSlot) {
        NexOnClick staticHandler = staticClickHandlers.get(topSlot);
        if (staticHandler != null) {
            String ns = staticNamespaces.getOrDefault(topSlot, "static");
            staticHandler.onClick(event, makeCtx(ns, topSlot, null));
            return;
        }

        // Lokale Page-Berechnung basierend auf aktiver Zone
        InvFillStrategy.BodyZone zone = (overrideZone != null ? overrideZone : inv.bodyZone());
        InvAlignment align = (overrideAlignment != null ? overrideAlignment : inv.bodyAlignment());
        int pageSize = Math.max(1, zone.slots.size());
        int totalItems = bodyModels.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));

        if (pageIndex >= totalPages) pageIndex = totalPages - 1;
        if (pageIndex < 0) pageIndex = 0;

        int pageOffset = pageIndex * pageSize;
        int itemsOnPage = Math.max(0, Math.min(pageSize, totalItems - pageOffset));

        Map<Integer, Integer> map = inv.fillStrategy().assignSlots(
                zone, pageIndex, pageSize, pageOffset, itemsOnPage, align
        );

        Integer bodyIdx = map.get(topSlot);
        if (bodyIdx != null) {
            NexOnClick h = bodyHandlersPerIndex.get(bodyIdx);
            if (h != null) {
                h.onClick(event, makeCtx("body", topSlot, bodyIdx));
            }
        }
    }



    private NexClickContext makeCtx(String namespace, int slot, Integer bodyIndex) {
        final boolean nav = namespace.startsWith("navigation:");
        final boolean req = namespace.startsWith("required:");
        final boolean cus = namespace.startsWith("custom:");
        final boolean body = "body".equals(namespace);

        return new NexClickContext() {
            @Override public Player player() { return NexInventoryView.this.player; }
            @Override public String inventoryId() { return inv.inventoryId(); }
            @Override public int pageIndex() { return NexInventoryView.this.pageIndex; }
            @Override public int slot() { return slot; }
            @Override public String namespace() { return namespace; }
            @Override public boolean isNavigation() { return nav; }
            @Override public boolean isRequired() { return req; }
            @Override public boolean isCustom() { return cus; }
            @Override public boolean isBody() { return body; }
            @Override public Integer bodyIndex() { return bodyIndex; }
        };
    }

}
