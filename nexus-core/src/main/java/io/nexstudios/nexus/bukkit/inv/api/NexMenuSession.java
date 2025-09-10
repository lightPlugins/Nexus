package io.nexstudios.nexus.bukkit.inv.api;

import io.nexstudios.nexus.bukkit.inv.NexInventoryView;
import io.nexstudios.nexus.bukkit.inv.NexOnClick;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Fluente Session für Menü-Nutzung
public final class NexMenuSession {

    private final InvService.InvHandleImpl handle;
    private Player player;

    // Konfiguration vor open()
    private final List<Runnable> preOpenTasks = new ArrayList<>();

    private NexInventoryView view; // nach open()

    private NexMenuSession(InvService.InvHandleImpl handle) {
        this.handle = handle;
    }

    public static NexMenuSession forHandle(InvService.InvHandleImpl handle) {
        return new NexMenuSession(handle);
    }

    public static NexMenuSession empty() {
        return new NexMenuSession(null);
    }

    public NexMenuSession forPlayer(Player p) {
        this.player = p;
        return this;
    }

    // extra-settings section from the content path
    public ConfigurationSection extraSettings() {
        if (handle == null) return null;
        return handle.inventory().extraSettings();
    }


    // Filler-Bindung
    public FillerBinding populateFiller(List<ItemStack> items, int startSlot1b, int endSlot1b, InvAlignment alignment) {
        Objects.requireNonNull(items, "items");
        preOpenTasks.add(() -> {
            ensureView();
            view.populateFillerStacks(items, startSlot1b, endSlot1b, alignment, null);
        });
        return new FillerBinding(this, items, startSlot1b, endSlot1b, alignment);
    }

    // Extra-Item-Bindung
    public ExtraBinding addItem(ItemStack stack, int[] slots1b) {
        Objects.requireNonNull(stack, "ItemStack");
        Objects.requireNonNull(slots1b, "slots");
        preOpenTasks.add(() -> {
            ensureView();
            view.addExtraItem(stack, slots1b, null);
        });
        return new ExtraBinding(this, stack, slots1b);
    }

    // Required-Handler global
    public NexMenuSession onRequireClick(NexOnClick handler) {
        preOpenTasks.add(() -> {
            ensureView();
            view.bindRequired(null, handler); // global
        });
        return this;
    }

    // Required-Handler gezielt
    public NexMenuSession onRequireClick(String id, NexOnClick handler) {
        preOpenTasks.add(() -> {
            ensureView();
            view.bindRequired(id, handler);
        });
        return this;
    }

    // Custom-Handler global/gezielt
    public NexMenuSession onCustomClick(NexOnClick handler) {
        preOpenTasks.add(() -> {
            ensureView();
            view.bindCustom(null, handler);
        });
        return this;
    }

    public NexMenuSession onCustomClick(String id, NexOnClick handler) {
        preOpenTasks.add(() -> {
            ensureView();
            view.bindCustom(id, handler);
        });
        return this;
    }

    // Navigation-Handler global/gezielt (zusätzlich zur Standardaktion)
    public NexMenuSession onNavigationClick(NexOnClick handler) {
        preOpenTasks.add(() -> {
            ensureView();
            view.bindNavigation(null, handler);
        });
        return this;
    }

    public NexMenuSession onNavigationClick(String id, NexOnClick handler) {
        preOpenTasks.add(() -> {
            ensureView();
            view.bindNavigation(id, handler);
        });
        return this;
    }

    // Öffnen
    public NexInventoryView open() {
        ensureView();
        // Nach dem Öffnen alle vorab definierten Konfigurationen anwenden (Filler/Extras/Handler)
        for (Runnable r : preOpenTasks) r.run();
        preOpenTasks.clear();
        return view;
    }

    private void ensureView() {
        if (view != null) return;
        if (handle == null) throw new IllegalStateException("No inventory handle available");
        if (player == null) throw new IllegalStateException("Player not set. Call forPlayer(player) first.");
        view = handle.open(player);
    }

    // Binding-Typen

    public static final class FillerBinding {
        private final NexMenuSession session;
        private final List<ItemStack> items;
        private final int start1b, end1b;
        private final InvAlignment alignment;

        FillerBinding(NexMenuSession session, List<ItemStack> items, int start1b, int end1b, InvAlignment alignment) {
            this.session = session;
            this.items = items;
            this.start1b = start1b;
            this.end1b = end1b;
            this.alignment = alignment;
        }

        public NexMenuSession onClick(NexOnClick handler) {
            session.preOpenTasks.add(() -> {
                session.ensureView();
                session.view.populateFillerStacks(items, start1b, end1b, alignment, handler);
            });
            return session;
        }
    }

    public static final class ExtraBinding {
        private final NexMenuSession session;
        private final ItemStack stack;
        private final int[] slots1b;

        ExtraBinding(NexMenuSession session, ItemStack stack, int[] slots1b) {
            this.session = session;
            this.stack = stack;
            this.slots1b = slots1b;
        }

        public NexMenuSession onClick(NexOnClick handler) {
            session.preOpenTasks.add(() -> {
                session.ensureView();
                session.view.addExtraItem(stack, slots1b, handler);
            });
            return session;
        }
    }
}