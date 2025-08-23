package io.nexstudios.nexus.bukkit.inv;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import io.nexstudios.nexus.bukkit.inv.fill.InvFillStrategy;
import io.nexstudios.nexus.bukkit.inv.pagination.NexPageSource;
import io.nexstudios.nexus.bukkit.items.ItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemHideFlag;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
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
        String rawTitle = inv.titleSupplier().apply(player);

        // Neu: Language-Keys direkt via NexusLanguage auflösen, sonst MessageSender nutzen
        Component titleComp = resolveTitle(rawTitle, inv.inventoryId(), player);

        this.top = Bukkit.createInventory(inv, inv.size(), titleComp);
        player.openInventory(this.top);
        NexInventoryManager.get().register(player.getUniqueId(), this);
        renderAll();
    }

    private static Component resolveTitle(String raw, String inventoryId, Player player) {
        if (raw == null || raw.isBlank()) {
            return Component.text("Inventory");
        }
        String s = raw.trim();

        if (s.startsWith("#language:")) {
            // "#language:..." -> Pfad für NexusLanguage bilden
            String path = languageTitlePath(s, inventoryId);
            return io.nexstudios.nexus.bukkit.NexusPlugin.getInstance()
                    .getNexusLanguage()
                    .getTranslation(player.getUniqueId(), path, false);
        }

        // Kein Language-Key: nutze bestehende MessageSender-Pipeline (PAPI + MiniMessage)
        return io.nexstudios.nexus.bukkit.NexusPlugin.getInstance()
                .messageSender
                .stringToComponent(player, raw);
    }

    private static String languageTitlePath(String raw, String invId) {
        String s = raw;

        // Präfix entfernen
        if (s.startsWith("#language:")) {
            s = s.substring("#language:".length());
        }
        // trailing "#": entfernen, falls vorhanden
        if (s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }

        // Bereits vollständiger Pfad
        if (s.startsWith("inventories.")) {
            return s;
        }

        // Standardfall: nur "title" oder custom Schlüssel
        if ("title".equalsIgnoreCase(s)) {
            return "inventories." + invId + ".title";
        }
        return "inventories." + invId + "." + s;
    }


//    public void setBodyItems(List<?> models, NexOnClick clickHandler) {
//        this.bodyModels = (List<Object>) (models != null ? models : Collections.emptyList());
//        this.bodyClickHandler = clickHandler;
//        this.pageIndex = 0;
//        renderAll();
//    }

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
        deco = NexServices.newItemBuilder().itemStack(deco).displayName(Component.text(" ")).build();
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
            deco = NexServices.newItemBuilder().itemStack(deco).displayName(Component.text(" ")).build();
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

        // Language für Name/Lore pro Spieler anwenden (falls konfiguriert)
        stack = applyLanguageForPlayer(stack, cfg, inv.inventoryId(), player.getUniqueId());

        int newRank = rankForNamespace(namespace);

        for (Integer s1b : cfg.slots1b) {
            int s0 = Math.max(0, s1b - 1);
            if (s0 < 0 || s0 >= inv.size()) continue;

            Integer existingRank = staticPriorities.get(s0);
            if (existingRank != null && existingRank >= newRank) {
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
            if (s0 >= inv.size()) continue;

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
            ItemStack st = inv.renderer().renderStatic(cfg, inv.inventoryId());
            if (st != null) {
                st = applyLanguageForPlayer(st, cfg, inv.inventoryId(), player.getUniqueId());
            }
            return st;
        }
        return null;
    }
    private ItemStack applyLanguageForPlayer(ItemStack stack, NexItemConfig cfg, String inventoryId, java.util.UUID playerId) {
        try {
            if (cfg == null) return stack;

            // Spezifikation prüfen
            String spec = (cfg.itemSpec == null) ? "" : cfg.itemSpec.trim().toLowerCase(java.util.Locale.ROOT);
            boolean isVanilla = spec.startsWith("minecraft:") || spec.startsWith("vanilla:") || !spec.contains(":");
            if (!isVanilla) {
                // Externe Items nicht anfassen (keine Anreicherung via Builder)
                return stack;
            }

            // Material auflösen
            String normalized = normalizeMinecraftSpec(spec);
            Material mat = Material.matchMaterial(normalized, false);
            if (mat == null) {
                String enumName = normalized.substring(normalized.indexOf(':') + 1).toUpperCase(java.util.Locale.ROOT);
                try { mat = Material.valueOf(enumName); } catch (IllegalArgumentException ignored) {}
            }
            if (mat == null) mat = Material.STONE;

            // Language: Name/Lore Pfade berechnen und via NexusLanguage auflösen
            Component nameComp = null;
            if (cfg.name != null && cfg.name.startsWith("#language:")) {
                String path = languagePath(cfg.name, inventoryId);
                nameComp = io.nexstudios.nexus.bukkit.NexusPlugin.getInstance()
                        .getNexusLanguage()
                        .getTranslation(playerId, path, false);
            }

            java.util.List<Component> loreComp = null;
            if (cfg.lore != null) {
                if (cfg.lore instanceof String s && s.startsWith("#language:")) {
                    String path = languagePath(s, inventoryId);
                    loreComp = io.nexstudios.nexus.bukkit.NexusPlugin.getInstance()
                            .getNexusLanguage()
                            .getTranslationList(playerId, path, false);
                } else if (cfg.lore instanceof java.util.List<?> list) {
                    java.util.List<Component> out = new java.util.ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof String ls && ls.startsWith("#language:")) {
                            String path = languagePath(ls, inventoryId);
                            java.util.List<Component> parts = io.nexstudios.nexus.bukkit.NexusPlugin.getInstance()
                                    .getNexusLanguage()
                                    .getTranslationList(playerId, path, false);
                            if (parts != null && !parts.isEmpty()) out.addAll(parts);
                        }
                    }
                    if (!out.isEmpty()) loreComp = out;
                }
            }

            // Builder aufsetzen
            int amount = (cfg.amount != null ? Math.max(1, cfg.amount) : Math.max(1, stack.getAmount()));
            ItemBuilder b = NexServices.newItemBuilder()
                    .material(mat)
                    .amount(amount);

            if (nameComp != null) b.displayName(nameComp);
            if (loreComp != null && !loreComp.isEmpty()) b.lore(loreComp);
            if (cfg.modelData != null) b.modelData(cfg.modelData);
            if (cfg.tooltipStyle != null && !cfg.tooltipStyle.isBlank()) {
                NamespacedKey style = parseKey(cfg.tooltipStyle.trim());
                b.tooltipStyle(style);
            }

            // Enchants / Hide-Flags aus der Config übernehmen
            java.util.Map<Enchantment, Integer> enchants = parseEnchantments(cfg.enchantments);
            if (enchants != null && !enchants.isEmpty()) b.enchantments(enchants);

            java.util.Set<ItemHideFlag> flags = parseHideFlags(cfg);
            if (flags != null && !flags.isEmpty()) b.hideFlags(flags);

            return b.build();
        } catch (Exception ignored) {
            return stack; // Fallback: unverändert lassen
        }
    }

    private static String normalizeMinecraftSpec(String spec) {
        if (spec == null || spec.isBlank()) return "minecraft:stone";
        if (!spec.contains(":")) return "minecraft:" + spec.toLowerCase(java.util.Locale.ROOT);
        if (spec.startsWith("vanilla:")) return "minecraft:" + spec.substring("vanilla:".length());
        return spec.toLowerCase(java.util.Locale.ROOT);
    }

    private static NamespacedKey parseKey(String value) {
        String v = value.toLowerCase(java.util.Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(v);
        if (key == null) key = NamespacedKey.minecraft(v);
        return key;
    }

    private static java.util.Map<Enchantment, Integer> parseEnchantments(java.util.List<java.util.Map<String, Object>> raw) {
        if (raw == null || raw.isEmpty()) return null;
        java.util.Map<Enchantment, Integer> out = new java.util.HashMap<>();
        for (java.util.Map<String, Object> e : raw) {
            if (e == null) continue;
            Object idObj = e.get("id");
            Object valObj = e.get("value");
            if (idObj == null) continue;

            String id = String.valueOf(idObj).toLowerCase(java.util.Locale.ROOT).trim();
            NamespacedKey key = NamespacedKey.fromString(id);
            if (key == null) key = NamespacedKey.minecraft(id);
            Enchantment ench = io.papermc.paper.registry.RegistryAccess.registryAccess()
                    .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT).get(key);
            if (ench == null) continue;

            int lvl = 1;
            if (valObj instanceof Number n) lvl = n.intValue();
            else if (valObj instanceof String s) {
                try { lvl = Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
            }
            out.put(ench, Math.max(1, lvl));
        }
        return out.isEmpty() ? null : out;
    }


    private static java.util.Set<ItemHideFlag> parseHideFlags(NexItemConfig cfx) {
        java.util.List<String> list = cfx.hideFlags;
        if (list == null || list.isEmpty()) return null;
        java.util.Set<ItemHideFlag> out = java.util.EnumSet.noneOf(ItemHideFlag.class);
        for (String f : list) {
            if (f == null) continue;
            String key = f.toLowerCase(java.util.Locale.ROOT).trim();
            ItemHideFlag hiddenFlag = ItemHideFlag.fromString(key);
            if (hiddenFlag != null) out.add(hiddenFlag);
        }
        return out.isEmpty() ? null : out;
    }

    private static String languagePath(String raw, String invId) {
        if (raw == null) return null;
        String s = raw;

        // "#language:" Präfix entfernen
        if (s.startsWith("#language:")) {
            s = s.substring("#language:".length());
        }
        // trailing "#": entfernen, falls vorhanden
        if (s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }

        if (s.startsWith("inventories.")) {
            return s;
        }
        if (s.startsWith("navigation.") || s.startsWith("required.") || s.startsWith("custom.")) {
            return "inventories." + invId + ".content." + s;
        }
        return "inventories." + invId + "." + s;
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
