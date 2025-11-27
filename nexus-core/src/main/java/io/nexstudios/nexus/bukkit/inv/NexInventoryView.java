package io.nexstudios.nexus.bukkit.inv;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import io.nexstudios.nexus.bukkit.inv.fill.InvFillStrategy;
import io.nexstudios.nexus.bukkit.items.ItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemHideFlag;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.UnaryOperator;

public class NexInventoryView {

    private final NexInventory inv;
    private final Player player;
    private Inventory top; // not final: properly set during open
    private int pageIndex = 0;

    @Setter
    private String overrideTitleRaw;

    private List<Object> bodyModels = Collections.emptyList();
    private NexOnClick bodyClickHandler;

    // Per-model click handlers (absolute index -> handler)
    private List<NexOnClick> bodyHandlersByModelIndex = null;

    // Slot -> static click handler (navigation/required/custom/extra)
    private final Map<Integer, NexOnClick> staticClickHandlers = new HashMap<>();
    // Slot -> namespace (navigation:*, required:*, custom:*, extra)
    private final Map<Integer, String> staticNamespaces = new HashMap<>();
    // Body index on current page -> handler (usually bodyClickHandler or per-entry handler)
    private final Map<Integer, NexOnClick> bodyHandlersPerIndex = new HashMap<>();

    // Handler maps
    private final Map<String, NexOnClick> requiredHandlers = new HashMap<>();
    private NexOnClick requiredGlobal;

    private final Map<String, NexOnClick> customHandlers = new HashMap<>();
    private NexOnClick customGlobal;

    private final Map<String, NexOnClick> navigationHandlers = new HashMap<>();
    private NexOnClick navigationGlobal;

    // Optional overrides for body zone/alignment (used by populateFiller(...))
    private InvFillStrategy.BodyZone overrideZone;
    private InvAlignment overrideAlignment;

    private static NexusLanguage nexusLanguage;

    private final Map<Integer, Integer> staticPriorities = new HashMap<>();

    private int updateTaskId = -1;
    @Setter private TagResolver titleTagResolver;

    public NexInventoryView(NexInventory inv, Player player, NexusLanguage nexusLanguage) {
        this.inv = inv;
        this.player = player;
        this.top = inv.getInventory();
        NexInventoryView.nexusLanguage = nexusLanguage;
    }

    public Player player() { return player; }

    public void open() {
        String rawTitle;

        if (overrideTitleRaw != null && !overrideTitleRaw.isBlank()) {
            rawTitle = overrideTitleRaw;
        } else {
            rawTitle = inv.titleSupplier().apply(player);
        }

        Component titleComp = resolveTitle(rawTitle, inv.inventoryId(), player, titleTagResolver);
        this.top = Bukkit.createInventory(inv, inv.size(), titleComp);
        player.openInventory(this.top);
        NexInventoryManager.get().register(player.getUniqueId(), this);
        renderAll();
        startAutoUpdate();
    }

    public void applyTitleOverrideNow() {
        String rawTitle;
        if (overrideTitleRaw != null && !overrideTitleRaw.isBlank()) {
            rawTitle = overrideTitleRaw;
        } else {
            rawTitle = inv.titleSupplier().apply(player);
        }

        Component titleComp = resolveTitle(rawTitle, inv.inventoryId(), player, titleTagResolver);

        Inventory newTop = Bukkit.createInventory(inv, inv.size(), titleComp);
        if (this.top != null) {
            for (int i = 0; i < inv.size(); i++) {
                newTop.setItem(i, this.top.getItem(i));
            }
        }

        this.top = newTop;
        player.openInventory(this.top);
        NexInventoryManager.get().register(player.getUniqueId(), this);
    }

    private static Component resolveTitle(String raw,
                                          String inventoryId,
                                          Player player,
                                          TagResolver resolver) {
        if (raw == null || raw.isBlank()) {
            return Component.text("Inventory");
        }
        String s = raw.trim();

        // LANGUAGE-Titel
        if (s.startsWith("#language:")) {
            String path = languageTitlePath(s, inventoryId);
            if (resolver != null) {
                return nexusLanguage.getTranslation(player.getUniqueId(), path, false, resolver);
            } else {
                return nexusLanguage.getTranslation(player.getUniqueId(), path, false);
            }
        }

        // Direkter Titel aus Inventar-Config
        if (resolver != null) {
            return NexusPlugin.getInstance().messageSender
                    .stringToComponent(player, raw, resolver);
        } else {
            return NexusPlugin.getInstance().messageSender
                    .stringToComponent(player, raw);
        }
    }

    private void startAutoUpdate() {
        int period = inv.updateIntervalTicks();
        if (period <= 0 || updateTaskId != -1) return;

        updateTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                NexusPlugin.getInstance(),
                () -> {
                    // Stop when player is no longer viewing this inventory or offline
                    if (!player.isOnline()) { stopAutoUpdate(); return; }
                    var openView = player.getOpenInventory();
                    if (openView.getTopInventory() != this.top) {
                        stopAutoUpdate();
                        return;
                    }

                    // Lightweight refresh: body + navigation
                    try {
                        renderBodyOnly();
                        renderNavigation();
                    } catch (Throwable t) {
                        stopAutoUpdate();
                    }
                },
                period, period
        );
    }

    private void stopAutoUpdate() {
        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }

    public void dispose() {
        stopAutoUpdate();
    }

    private static String languageTitlePath(String raw, String invId) {
        String s = raw;

        if (s.startsWith("#language:")) {
            s = s.substring("#language:".length());
        }
        if (s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }

        if (s.startsWith("inventories.")) return s;

        if ("title".equalsIgnoreCase(s)) {
            return "inventories." + invId + ".title";
        }
        return "inventories." + invId + "." + s;
    }

    public void nextPage() {
        InvFillStrategy.BodyZone zone = (overrideZone != null ? overrideZone : inv.bodyZone());
        int pageSize = Math.max(1, zone.slots.size());

        int totalItems = bodyModels.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));

        int newIndex = Math.min(pageIndex + 1, totalPages - 1);
        if (newIndex != pageIndex) {
            pageIndex = newIndex;
            renderBodyOnly();
            renderNavigation();
        }
    }

    public void prevPage() {
        InvFillStrategy.BodyZone zone = (overrideZone != null ? overrideZone : inv.bodyZone());
        int pageSize = Math.max(1, zone.slots.size());

        int totalItems = bodyModels.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));

        int newIndex = Math.max(pageIndex - 1, 0);
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
        staticPriorities.clear();
    }

    private void placeDecoration() {
        if (!inv.decorationEnabled()) return;
        ItemStack deco = renderItemSpec(inv.decorationItemSpec(), null);
        deco = NexServices.newItemBuilder().itemStack(deco).displayName(Component.text(" ")).build();
        for (int i = 0; i < inv.size(); i++) {
            top.setItem(i, deco);
        }
    }

    private void renderNavigation() {
        Map<String, NexItemConfig> nav = inv.navigation();
        NexItemConfig prev = nav.get("previous-page");
        NexItemConfig next = nav.get("next-page");
        NexItemConfig close = nav.get("close");
        NexItemConfig back = nav.get("back");

        // Navigation-Slots komplett leeren (inkl. alter Deko)
        for (NexItemConfig cfg : nav.values()) {
            if (cfg == null || cfg.slots1b == null) continue;
            for (Integer s1b : cfg.slots1b) {
                int s0 = Math.max(0, s1b - 1);
                if (s0 >= inv.size()) continue;

                top.setItem(s0, null);
                staticClickHandlers.remove(s0);
                staticNamespaces.remove(s0);
                staticPriorities.remove(s0);
            }
        }

        InvFillStrategy.BodyZone zone = (overrideZone != null ? overrideZone : inv.bodyZone());
        int pageSize = Math.max(1, zone.slots.size());

        int totalItems = bodyModels.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));

        if (pageIndex >= totalPages) pageIndex = totalPages - 1;
        if (pageIndex < 0) pageIndex = 0;

        boolean hasPrev = pageIndex > 0;
        boolean hasNext = pageIndex < (totalPages - 1);

        if (prev != null && hasPrev) {
            placeStaticItem(prev, "navigation:previous-page", (e, ctx) -> {
                prevPage();
                fireNavigationCustom("previous-page", e, ctx);
            });
        }
        if (next != null && hasNext) {
            placeStaticItem(next, "navigation:next-page", (e, ctx) -> {
                nextPage();
                fireNavigationCustom("next-page", e, ctx);
            });
        }
        if (close != null) {
            placeStaticItem(close, "navigation:close", (e, ctx) -> {
                player.closeInventory();
                fireNavigationCustom("close", e, ctx);
            });
        }
        if (back != null) {
            placeStaticItem(back, "navigation:back", (e, ctx) -> {
                fireNavigationCustom("back", e, ctx);
            });
        }

        // Fehlt an einem Navigation-Slot ein Item, fülle ihn wieder mit der Deko
        if (inv.decorationEnabled()) {
            ItemStack deco = renderItemSpec(inv.decorationItemSpec(), null);
            deco = NexServices.newItemBuilder().itemStack(deco).displayName(Component.text(" ")).build();

            for (NexItemConfig cfg : nav.values()) {
                if (cfg == null || cfg.slots1b == null) continue;
                for (Integer s1b : cfg.slots1b) {
                    int s0 = Math.max(0, s1b - 1);
                    if (s0 >= inv.size()) continue;
                    if (top.getItem(s0) == null) {
                        top.setItem(s0, deco);
                    }
                }
            }
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
            if (target == null) target = (requiredGlobal != null ? requiredGlobal : (e, c) -> {});
            placeStaticItem(cfg, ns, target);
        }
    }

    private void renderCustom() {
        for (NexItemConfig cfg : inv.custom()) {
            String ns = "custom:" + cfg.id;
            NexOnClick target = customHandlers.get(cfg.id);
            if (target == null) target = (customGlobal != null ? customGlobal : (e, c) -> {});
            placeStaticItem(cfg, ns, target);
        }
    }

    private void renderBodyOnly() {
        InvFillStrategy.BodyZone zone = (overrideZone != null ? overrideZone : inv.bodyZone());
        InvAlignment align = (overrideAlignment != null ? overrideAlignment : inv.bodyAlignment());

        int pageSize = Math.max(1, zone.slots.size());
        int totalItems = bodyModels.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        if (pageIndex >= totalPages) pageIndex = totalPages - 1;
        if (pageIndex < 0) pageIndex = 0;

        int pageOffset = pageIndex * pageSize;
        int itemsOnPage = Math.max(0, Math.min(pageSize, totalItems - pageOffset));

        if (itemsOnPage <= 0) {
            return;
        }

        // Clear body zone
        for (int slot : zone.slots) {
            top.setItem(slot, null);
        }
        bodyHandlersPerIndex.clear();

        Map<Integer, Integer> map = inv.fillStrategy().assignSlots(
                zone, pageIndex, pageSize, pageOffset, itemsOnPage, align
        );

        // Place fillers
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            int slot = e.getKey();
            int bodyIndexInPage = e.getValue();
            int modelAbsIndex = pageOffset + bodyIndexInPage;

            Object model = bodyModels.get(modelAbsIndex);
            ItemStack stack = renderBodyModel(model);
            if (stack != null) {
                top.setItem(slot, stack);

                NexOnClick handler = null;
                if (bodyHandlersByModelIndex != null
                        && modelAbsIndex >= 0
                        && modelAbsIndex < bodyHandlersByModelIndex.size()) {
                    handler = bodyHandlersByModelIndex.get(modelAbsIndex);
                }
                if (handler == null) handler = bodyClickHandler;

                if (handler != null) {
                    bodyHandlersPerIndex.put(bodyIndexInPage, handler);
                }
            }
        }

        // Fill empty slots in body zone back with decoration (if enabled)
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

        stack = applyLanguageForPlayer(stack, cfg, inv.inventoryId(), player.getUniqueId());

        int newRank = rankForNamespace(namespace);

        for (Integer s1b : cfg.slots1b) {
            int s0 = Math.max(0, s1b - 1);
            if (s0 >= inv.size()) continue;

            Integer existingRank = staticPriorities.get(s0);
            // WICHTIG: Nur höhere Ranks blocken, gleicher Rank darf überschreiben
            if (existingRank != null && existingRank > newRank) {
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
        return -1;
    }

    public void setExtraItem(int slot1b, ItemStack stack, NexOnClick handler) {
        if (stack == null) return;

        int s0 = Math.max(0, slot1b - 1);
        if (s0 >= inv.size()) return;

        top.setItem(s0, stack);

        if (handler != null) {
            staticClickHandlers.put(s0, handler);
            staticNamespaces.put(s0, "extra");
            staticPriorities.put(s0, rankForNamespace("extra"));
        } else {
            // Kein Handler gewünscht -> ggf. alten "extra"-Handler entfernen
            if ("extra".equals(staticNamespaces.get(s0))) {
                staticClickHandlers.remove(s0);
                staticNamespaces.remove(s0);
                staticPriorities.remove(s0);
            }
        }
    }

    public void rerenderRequiredItem(String id, TagResolver extraResolver) {
        if (id == null) return;

        // passendes Config-Item suchen
        NexItemConfig cfg = null;
        for (NexItemConfig c : inv.required()) {
            if (c != null && id.equalsIgnoreCase(c.id)) {
                cfg = c;
                break;
            }
        }
        if (cfg == null) return;

        // Basis-Stack + Sprache erneut anwenden
        ItemStack base = inv.renderer().renderStatic(cfg, inv.inventoryId());
        if (base == null) return;

        ItemStack rendered = applyLanguageForPlayer(
                base, cfg, inv.inventoryId(), player.getUniqueId(), extraResolver
        );

        // In alle Slots dieses Required-Items setzen
        for (Integer s1b : cfg.slots1b) {
            int s0 = Math.max(0, s1b - 1);
            if (s0 < 0 || s0 >= inv.size()) continue;

            top.setItem(s0, rendered);
            // Handler/Namespaces/Prioritäten bleiben wie sie sind
        }
    }

    public void updateSlotItem(int slot1b, UnaryOperator<ItemStack> transformer) {
        if (transformer == null) return;

        int s0 = Math.max(0, slot1b - 1);
        if (s0 >= inv.size()) return;

        ItemStack current = top.getItem(s0);
        if (current == null) return;

        ItemStack updated = transformer.apply(current);
        if (updated == null) return;

        top.setItem(s0, updated);
    }

    public void bindRequired(String idOrNull, NexOnClick handler) {
        if (handler == null) return;
        if (idOrNull == null) { requiredGlobal = handler; }
        else { requiredHandlers.put(idOrNull, handler); }
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

        // Normalize range
        int start1 = Math.max(1, Math.min(startSlot1b, endSlot1b));
        int end1   = Math.max(start1, Math.max(startSlot1b, endSlot1b));

        int start0 = start1 - 1;
        int end0   = end1 - 1;

        int startRow = start0 / 9;
        int endRow   = end0   / 9;

        int leftCol  = start0 % 9;
        int rightCol = end0   % 9;

        if (rightCol < leftCol) {
            int tmp = leftCol;
            leftCol = rightCol;
            rightCol = tmp;
        }

        List<Integer> slots0b = new ArrayList<>();
        for (int row = startRow; row <= endRow; row++) {
            for (int col = leftCol; col <= rightCol; col++) {
                int s0 = row * 9 + col;
                if (s0 >= 0 && s0 < inv.size()) {
                    slots0b.add(s0);
                }
            }
        }

        this.overrideZone = new InvFillStrategy.BodyZone(
                Math.max(1, endRow - startRow + 1),
                Math.max(1, rightCol - leftCol + 1),
                Collections.unmodifiableList(slots0b)
        );
        this.overrideAlignment = alignment;

        this.bodyModels = new ArrayList<>(items);
        this.bodyClickHandler = clickHandler;
        this.bodyHandlersByModelIndex = null;

        renderBodyOnly();
        renderNavigation();
    }

    // Per-entry filler population with per-model handlers.
    public void populateFillerEntries(List<ItemStack> items, List<NexOnClick> handlers,
                                      int startSlot1b, int endSlot1b, InvAlignment alignment) {
        if (items == null) items = List.of();
        if (handlers == null) handlers = Collections.nCopies(items.size(), null);
        if (handlers.size() != items.size()) {
            throw new IllegalArgumentException("handlers.size() must match items.size()");
        }

        populateFillerStacks(items, startSlot1b, endSlot1b, alignment, null);

        this.bodyHandlersByModelIndex = new ArrayList<>(handlers);

        renderBodyOnly();
        renderNavigation();
    }

    // Update a visible body item by its page-local index.
    public void updateBodyItemAtVisibleIndex(int bodyIndexInPage, java.util.function.UnaryOperator<ItemStack> transformer) {
        if (transformer == null) return;

        InvFillStrategy.BodyZone zone = (overrideZone != null ? overrideZone : inv.bodyZone());
        InvAlignment align = (overrideAlignment != null ? overrideAlignment : inv.bodyAlignment());

        int pageSize = Math.max(1, zone.slots.size());
        int totalItems = bodyModels.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        if (pageIndex >= totalPages) pageIndex = totalPages - 1;
        if (pageIndex < 0) pageIndex = 0;

        int pageOffset = pageIndex * pageSize;
        int itemsOnPage = Math.max(0, Math.min(pageSize, totalItems - pageOffset));
        if (bodyIndexInPage < 0 || bodyIndexInPage >= itemsOnPage) return;

        Map<Integer, Integer> map = inv.fillStrategy().assignSlots(
                zone, pageIndex, pageSize, pageOffset, itemsOnPage, align
        );

        Integer targetSlot = null;
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            if (e.getValue() == bodyIndexInPage) {
                targetSlot = e.getKey();
                break;
            }
        }
        if (targetSlot == null) return;

        ItemStack current = top.getItem(targetSlot);
        ItemStack updated = transformer.apply(current);
        if (updated == null) return;

        top.setItem(targetSlot, updated);

        int absIndex = pageOffset + bodyIndexInPage;
        if (absIndex >= 0 && absIndex < bodyModels.size()) {
            bodyModels.set(absIndex, updated);
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

    private ItemStack applyLanguageForPlayer(ItemStack stack, NexItemConfig cfg, String inventoryId, UUID playerId) {
        // Standard: kein zusätzlicher Resolver
        return applyLanguageForPlayer(stack, cfg, inventoryId, playerId, null);
    }

    private ItemStack applyLanguageForPlayer(ItemStack stack,
                                             NexItemConfig cfg,
                                             String inventoryId,
                                             UUID playerId,
                                             TagResolver extraResolver) {
        try {
            if (cfg == null) return stack;

            String spec = (cfg.itemSpec == null) ? "" : cfg.itemSpec.trim().toLowerCase(Locale.ROOT);
            boolean isVanilla = spec.startsWith("minecraft:") || spec.startsWith("vanilla:") || !spec.contains(":");
            if (!isVanilla) {
                return stack;
            }

            String normalized = normalizeMinecraftSpec(spec);
            Material mat = Material.matchMaterial(normalized, false);
            if (mat == null) {
                String enumName = normalized.substring(normalized.indexOf(':') + 1).toUpperCase(Locale.ROOT);
                try { mat = Material.valueOf(enumName); } catch (IllegalArgumentException ignored) {}
            }
            if (mat == null) mat = Material.STONE;

            Component nameComp = null;
            if (cfg.name != null && cfg.name.startsWith("#language:")) {
                String path = languagePath(cfg.name, inventoryId);
                if (extraResolver != null) {
                    nameComp = nexusLanguage.getTranslation(playerId, path, false, extraResolver);
                } else {
                    nameComp = nexusLanguage.getTranslation(playerId, path, false);
                }
            }

            List<Component> loreComp = null;
            if (cfg.lore != null) {
                if (cfg.lore instanceof String s && s.startsWith("#language:")) {
                    String path = languagePath(s, inventoryId);
                    if (extraResolver != null) {
                        loreComp = nexusLanguage.getTranslationList(playerId, path, false, extraResolver);
                    } else {
                        loreComp = nexusLanguage.getTranslationList(playerId, path, false);
                    }
                } else if (cfg.lore instanceof List<?> list) {
                    List<Component> out = new ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof String ls && ls.startsWith("#language:")) {
                            String path = languagePath(ls, inventoryId);
                            List<Component> parts;
                            if (extraResolver != null) {
                                parts = nexusLanguage.getTranslationList(playerId, path, false, extraResolver);
                            } else {
                                parts = nexusLanguage.getTranslationList(playerId, path, false);
                            }
                            if (parts != null && !parts.isEmpty()) out.addAll(parts);
                        }
                    }
                    if (!out.isEmpty()) loreComp = out;
                }
            }

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

            Map<Enchantment, Integer> enchants = parseEnchantments(cfg.enchantments);
            if (enchants != null && !enchants.isEmpty()) b.enchantments(enchants);

            Set<ItemHideFlag> flags = parseHideFlags(cfg);
            if (flags != null && !flags.isEmpty()) b.hideFlags(flags);

            return b.build();
        } catch (Exception ignored) {
            return stack;
        }
    }

    private static String normalizeMinecraftSpec(String spec) {
        if (spec == null || spec.isBlank()) return "minecraft:stone";
        if (!spec.contains(":")) return "minecraft:" + spec.toLowerCase(Locale.ROOT);
        if (spec.startsWith("vanilla:")) return "minecraft:" + spec.substring("vanilla:".length());
        return spec.toLowerCase(Locale.ROOT);
    }

    private static NamespacedKey parseKey(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(v);
        if (key == null) key = NamespacedKey.minecraft(v);
        return key;
    }

    private static Map<Enchantment, Integer> parseEnchantments(List<Map<String, Object>> raw) {
        if (raw == null || raw.isEmpty()) return null;
        Map<Enchantment, Integer> out = new HashMap<>();
        for (Map<String, Object> e : raw) {
            if (e == null) continue;
            Object idObj = e.get("id");
            Object valObj = e.get("value");
            if (idObj == null) continue;

            String id = String.valueOf(idObj).toLowerCase(Locale.ROOT).trim();
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

    private static Set<ItemHideFlag> parseHideFlags(NexItemConfig cfx) {
        List<String> list = cfx.hideFlags;
        if (list == null || list.isEmpty()) return null;
        Set<ItemHideFlag> out = EnumSet.noneOf(ItemHideFlag.class);
        for (String f : list) {
            if (f == null) continue;
            String key = f.toLowerCase(Locale.ROOT).trim();
            ItemHideFlag hiddenFlag = ItemHideFlag.fromString(key);
            if (hiddenFlag != null) out.add(hiddenFlag);
        }
        return out.isEmpty() ? null : out;
    }

    private static String languagePath(String raw, String invId) {
        if (raw == null) return null;
        String s = raw;

        if (s.startsWith("#language:")) {
            s = s.substring("#language:".length());
        }
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

    // Listener dispatch

    public void handleClick(InventoryClickEvent event, int topSlot) {
        NexOnClick staticHandler = staticClickHandlers.get(topSlot);
        if (staticHandler != null) {
            String ns = staticNamespaces.getOrDefault(topSlot, "static");
            staticHandler.onClick(event, makeCtx(event, ns, topSlot, null));
            return;
        }

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
                h.onClick(event, makeCtx(event, "body", topSlot, bodyIdx));
            }
        }
    }


    private NexClickContext makeCtx(InventoryClickEvent event, String namespace, int slot, Integer bodyIndex) {
        final boolean nav = namespace.startsWith("navigation:");
        final boolean req = namespace.startsWith("required:");
        final boolean cus = namespace.startsWith("custom:");
        final boolean body = "body".equals(namespace);

        final ClickType ct = event.getClick();
        final InventoryAction act = event.getAction();
        final boolean shift = event.isShiftClick();
        final int hotbar = event.getHotbarButton(); // -1, wenn nicht NUMBER_KEY
        final boolean left = (ct == ClickType.LEFT || ct == ClickType.SHIFT_LEFT);
        final boolean right = (ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT);
        final boolean middle = (ct == ClickType.MIDDLE);
        final boolean dbl = (ct == ClickType.DOUBLE_CLICK);
        final boolean keyboard = (ct == ClickType.NUMBER_KEY || ct == ClickType.DROP || ct == ClickType.CONTROL_DROP);

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
            @Override public org.bukkit.configuration.ConfigurationSection extraSettings() { return inv.extraSettings(); }

            @Override public boolean isLeftClick() { return left; }
            @Override public boolean isRightClick() { return right; }
            @Override public boolean isMiddleClick() { return middle; }
            @Override public boolean isShiftClick() { return shift; }
            @Override public boolean isDoubleClick() { return dbl; }
            @Override public boolean isKeyboardClick() { return keyboard; }
            @Override public int hotbarKey() { return (ct == ClickType.NUMBER_KEY && hotbar >= 0) ? (hotbar + 1) : -1; }
            @Override public ClickType clickType() { return ct; }
            @Override public InventoryAction action() { return act; }
        };
    }

}