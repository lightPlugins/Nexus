package io.nexstudios.nexus.bukkit.inv.api;

import io.nexstudios.nexus.bukkit.inv.NexInventory;
import io.nexstudios.nexus.bukkit.inv.NexInventoryView;
import io.nexstudios.nexus.bukkit.inv.NexOnClick;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class NexMenuSession {

    private final InvHandle handle;
    NexInventoryView view;
    final List<Runnable> preOpenTasks = new ArrayList<>();
    private boolean opened = false;
    private TagResolver titleResolver;


    private NexMenuSession(InvHandle handle) {
        this.handle = handle;
    }

    public static NexMenuSession forHandle(InvHandle handle) {
        Objects.requireNonNull(handle, "handle");
        return new NexMenuSession(handle);
    }

    public static NexMenuSession empty() {
        return new NexMenuSession(new InvHandle() {
            @Override public InvKey key() { return null; }
            @Override public NexInventoryView open(Player player) { return null; }
            @Override public InvHandle setBodyItems(List<?> models, NexOnClick clickHandler) { return this; }
            @Override public NexInventory inventory() { return null; }
            @Override public InvHandle onPostLoad(Consumer<NexInventory> a) { return this; }
        }) {
            @Override public NexInventoryView openFor(Player player) { return null; }
            @Override public NexMenuSession withTitleTags(TagResolver... tags) { return this; }
            @Override public NexMenuSession onRequireClick(NexOnClick handler) { return this; }
            @Override public NexMenuSession onRequireClick(String id, NexOnClick handler) { return this; }
            @Override public NexMenuSession onCustomClick(String id, NexOnClick handler) { return this; }
            @Override public NexMenuSession onNavigationClick(String idOrNull, NexOnClick handler) { return this; }
            @Override public FillerBinding populateFiller(List<ItemStack> items, int startSlot1b, int endSlot1b, InvAlignment alignment) { return new FillerBinding(this, startSlot1b, endSlot1b, alignment); }
            @Override public NexMenuSession populateFillerEntries(List<NexFillerEntry> entries, int startSlot1b, int endSlot1b, InvAlignment alignment) { return this; }
        };
    }

    public NexInventoryView openFor(Player player) {
        if (!opened) {
            this.view = handle.open(player);

            // Den zuvor gesetzten Title-Resolver sofort auf die View anwenden
            if (titleResolver != null) {
                try {
                    view.setTitleTagResolver(titleResolver);
                } catch (Exception ignored) { /* falls View intern (noch) nicht bereit ist */ }
            }

            for (Runnable r : preOpenTasks) {
                try { r.run(); } catch (Exception ignored) { /* Pre-open task failed */ }
            }
            preOpenTasks.clear();
            opened = true;
        }
        return view;
    }

    public NexMenuSession withRawTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) return this;

        Runnable task = () -> {
            ensureView();
            view.setOverrideTitleRaw(rawTitle);
            view.applyTitleOverrideNow();
        };

        if (opened && view != null) {
            task.run();
        } else {
            preOpenTasks.add(task);
        }
        return this;
    }

    void ensureView() {
        if (view == null) {
            throw new IllegalStateException("View is not initialized. Call openFor(player) before accessing the view.");
        }
    }

    public List<Component> resolveLore(Object input, TagResolver tags) {
        return resolveLore(null, input, tags);
    }

    public List<Component> resolveLore(Player player, Object input, TagResolver tags) {
        if (view != null) return view.resolveDynamicLore(input, tags);

        // Manueller Fallback ohne View-Erzeugung (vermeidet NPE im Titel)
        NexInventory inv = handle.inventory();
        NexInventoryView tempView = new NexInventoryView(inv, player, inv.getNexusLanguage());
        return tempView.resolveDynamicLore(input, tags);
    }

    public Component resolveComponent(String input, TagResolver tags) {
        return resolveComponent(null, input, tags);
    }

    public Component resolveComponent(Player player, String input, TagResolver tags) {
        if (view != null) return view.resolveDynamicComponent(input, tags);

        NexInventory inv = handle.inventory();
        NexInventoryView tempView = new NexInventoryView(inv, player, inv.getNexusLanguage());
        return tempView.resolveDynamicComponent(input, tags);
    }

    public NexMenuSession withTitleTags(TagResolver... tags) {
        // Resolver direkt in der Session speichern (ohne View-Zugriff)
        this.titleResolver = (tags != null && tags.length > 0) ? TagResolver.resolver(tags) : null;

        Runnable task = () -> {
            ensureView();
            // Resolver auf die View setzen
            view.setTitleTagResolver(titleResolver);
            // NEU: Titel sofort mit Resolver neu anwenden
            view.applyTitleOverrideNow();
        };

        if (opened && view != null) {
            task.run();
        } else {
            // Falls noch nicht geöffnet, beim Open ausführen
            preOpenTasks.add(task);
        }
        return this;
    }



    public NexMenuSession onRequireClick(NexOnClick handler) {
        Objects.requireNonNull(handler, "handler");
        preOpenTasks.add(() -> {
            ensureView();
            view.bindRequired(null, handler);
        });
        return this;
    }

    public NexMenuSession onRequireClick(String id, NexOnClick handler) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(handler, "handler");
        preOpenTasks.add(() -> {
            ensureView();
            view.bindRequired(id, handler);
        });
        return this;
    }

    public NexMenuSession onCustomClick(String id, NexOnClick handler) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(handler, "handler");
        preOpenTasks.add(() -> {
            ensureView();
            view.bindCustom(id, handler);
        });
        return this;
    }

    public NexMenuSession onNavigationClick(String idOrNull, NexOnClick handler) {
        Objects.requireNonNull(handler, "handler");
        preOpenTasks.add(() -> {
            ensureView();
            view.bindNavigation(idOrNull, handler);
        });
        return this;
    }

    public NexMenuSession updateRequiredItem(String id, TagResolver extraResolver) {
        Objects.requireNonNull(id, "id");
        Runnable task = () -> {
            ensureView();
            view.rerenderRequiredItem(id, extraResolver);
        };

        if (opened && view != null) {
            task.run();
        } else {
            preOpenTasks.add(task);
        }
        return this;
    }


    /**
     * Steuert, ob das Spieler-Inventar (unterer Teil) bei dieser Session gelockt ist.
     * Default ist true (gelockt); hier kannst du es umstellen, bevor oder nachdem das
     * Inventar geöffnet wurde.
     */
    public NexMenuSession withPlayerInventoryLocked(boolean locked) {
        Runnable task = () -> {
            ensureView();
            view.setPlayerInventoryLocked(locked);
        };

        if (opened && view != null) {
            task.run();
        } else {
            preOpenTasks.add(task);
        }
        return this;
    }

    /**
     * Kurzform: Spieler-Inventar entsperren.
     */
    public NexMenuSession withUnlockedPlayerInventory() {
        return withPlayerInventoryLocked(false);
    }

    /**
     * Kombiniert Required-Item-Re-Render + Lore-Anpassung in EINEM Schritt.
     * Dadurch wird nur die finale Lore an den Client gesendet (kein Flackern).
     */
    public NexMenuSession updateRequiredItemLore(String id,
                                                 TagResolver extraResolver,
                                                 UnaryOperator<List<Component>> loreUpdater) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(loreUpdater, "loreUpdater");

        Runnable task = () -> {
            ensureView();
            view.rerenderRequiredItemWithLore(id, extraResolver, loreUpdater);
        };

        if (opened && view != null) {
            task.run();
        } else {
            preOpenTasks.add(task);
        }
        return this;
    }

    public NexMenuSession updateSlotItem(int slot1b, UnaryOperator<ItemStack> transformer) {
        Objects.requireNonNull(transformer, "transformer");

        Runnable task = () -> {
            ensureView();
            view.updateSlotItem(slot1b, transformer);
        };

        if (opened && view != null) {
            task.run();
        } else {
            preOpenTasks.add(task);
        }

        return this;
    }

    public NexMenuSession updateSlotDisplayName(int slot1b, UnaryOperator<Component> displayNameUpdater) {
        Objects.requireNonNull(displayNameUpdater, "displayNameUpdater");
        return updateSlotItem(slot1b, stack -> {
            if (stack == null) return null;

            var meta = stack.getItemMeta();
            if (meta == null) return stack;

            Component currentName = meta.displayName();
            Component newName = displayNameUpdater.apply(currentName);
            meta.displayName(newName);

            stack.setItemMeta(meta);
            return stack;
        });
    }

    public NexMenuSession updateSlotLore(int slot, UnaryOperator<List<Component>> loreUpdater) {
        Objects.requireNonNull(loreUpdater, "loreUpdater");
        return updateSlotItem(slot, stack -> {
            if (stack == null) return null;

            var meta = stack.getItemMeta();
            if (meta == null) return stack;

            List<Component> currentLore = meta.lore();
            // aktuelle Lore-Liste defensiv kopieren, damit der Updater sie gefahrlos verändern kann
            List<Component> loreCopy = (currentLore == null) ? new ArrayList<>() : new ArrayList<>(currentLore);

            List<Component> newLore = loreUpdater.apply(loreCopy);
            meta.lore(newLore);

            stack.setItemMeta(meta);
            return stack;
        });
    }

    public NexMenuSession setItem(int slot1b, ItemStack item, NexOnClick clickHandler) {
        Objects.requireNonNull(item, "item");
        preOpenTasks.add(() -> {
            ensureView();
            view.setExtraItem(slot1b, item, clickHandler);
        });
        return this;
    }

    public FillerBinding populateFiller(List<ItemStack> items, int startSlot1b, int endSlot1b, InvAlignment alignment) {
        Objects.requireNonNull(items, "items");
        Runnable task = () -> {
            ensureView();
            view.populateFillerStacks(items, startSlot1b, endSlot1b, alignment, null);
        };

        if (opened && view != null) {
            task.run();
        } else {
            preOpenTasks.add(task);
        }
        return new FillerBinding(this, startSlot1b, endSlot1b, alignment);
    }


    // CHANGED: return NexMenuSession for fluent chaining (lambda style)
    public NexMenuSession populateFillerEntries(List<NexFillerEntry> entries, int startSlot1b, int endSlot1b, InvAlignment alignment) {
        Objects.requireNonNull(entries, "entries");

        List<ItemStack> items = new ArrayList<>(entries.size());
        List<NexOnClick> handlers = new ArrayList<>(entries.size());
        for (NexFillerEntry e : entries) {
            items.add(e.item());
            handlers.add(e.onClick());
        }

        Runnable task = () -> {
            ensureView();
            view.populateFillerEntries(items, handlers, startSlot1b, endSlot1b, alignment);
        };

        // Wenn bereits geöffnet: sofort ausführen, sonst für openFor() vormerken
        if (opened && view != null) {
            task.run();
        } else {
            preOpenTasks.add(task);
        }

        return this;
    }

    public static final class FillerBinding {
        private final NexMenuSession session;
        private final int start1b;
        private final int end1b;
        private final InvAlignment alignment;

        FillerBinding(NexMenuSession session, int start1b, int end1b, InvAlignment alignment) {
            this.session = session;
            this.start1b = start1b;
            this.end1b = end1b;
            this.alignment = alignment;
        }

        private NexInventoryView view() {
            session.ensureView();
            return session.view;
        }

        public FillerBinding update(int bodyIndexInPage, UnaryOperator<ItemStack> transformer) {
            if (transformer == null) return this;
            view().updateBodyItemAtVisibleIndex(bodyIndexInPage, transformer);
            return this;
        }

        public FillerBinding set(int bodyIndexInPage, ItemStack newStack) {
            if (newStack == null) return this;
            return update(bodyIndexInPage, old -> newStack);
        }

        public FillerBinding toggleGlow(int bodyIndexInPage, boolean glow) {
            return update(bodyIndexInPage, current -> {
                if (current == null) return null;
                var builder = io.nexstudios.nexus.bukkit.platform.NexServices
                        .newItemBuilder()
                        .itemStack(current);
                if (glow) {
                    builder.enchantments(Map.of(Enchantment.FORTUNE, 1))
                            .hideFlags(Set.of(io.nexstudios.nexus.bukkit.items.ItemHideFlag.HIDE_ENCHANTS));
                } else {
                    builder.enchantments(Collections.emptyMap());
                }
                return builder.build();
            });
        }

        public int startSlot1b() { return start1b; }
        public int endSlot1b() { return end1b; }
        public InvAlignment alignment() { return alignment; }
    }
}