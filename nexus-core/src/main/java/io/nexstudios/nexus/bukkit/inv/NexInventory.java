package io.nexstudios.nexus.bukkit.inv;

import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import io.nexstudios.nexus.bukkit.inv.fill.InvFillStrategy;
import io.nexstudios.nexus.bukkit.inv.fill.RowMajorFillStrategy;
import io.nexstudios.nexus.bukkit.inv.pagination.NexPageSource;
import io.nexstudios.nexus.bukkit.inv.renderer.NexItemRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Function;

public class NexInventory implements InventoryHolder {

    private final String inventoryId;
    private final int rows;
    private final int size;
    private final Function<Player, String> titleSupplier;
    private final int updateIntervalTicks;

    private final boolean decorationEnabled;
    private final String decorationItemSpec;

    private final Map<String, NexItemConfig> navigation;
    private final List<NexItemConfig> required;
    private final List<NexItemConfig> custom;

    private final InvAlignment bodyAlignment;
    private final InvFillStrategy.BodyZone bodyZone;
    private final InvFillStrategy fillStrategy;

    private final NexItemRenderer itemRenderer;

    private final Inventory inventory;

    private NexInventory(Builder b) {
        this.inventoryId = b.inventoryId;
        this.rows = b.rows;
        this.size = rows * 9;
        this.titleSupplier = b.titleSupplier != null ? b.titleSupplier : (p -> "Inventory");
        this.updateIntervalTicks = b.updateIntervalTicks;

        this.decorationEnabled = b.decorationEnabled;
        this.decorationItemSpec = b.decorationItemSpec;

        this.navigation = Objects.requireNonNullElseGet(b.navigation, HashMap::new);
        this.required = Objects.requireNonNullElseGet(b.required, ArrayList::new);
        this.custom = Objects.requireNonNullElseGet(b.custom, ArrayList::new);

        this.bodyAlignment = b.bodyAlignment != null ? b.bodyAlignment : InvAlignment.LEFT;
        List<Integer> slots0b = new ArrayList<>();
        if (b.bodySlots0b != null && !b.bodySlots0b.isEmpty()) {
            slots0b.addAll(b.bodySlots0b);
        } else if (b.bodyRows > 0 && b.bodyCols > 0) {
            for (int r = 0; r < b.bodyRows; r++) {
                for (int c = 0; c < b.bodyCols; c++) {
                    int row = 1 + r;
                    int col = 1 + c;
                    int slot = row * 9 + col;
                    if (slot >= 0 && slot < size) slots0b.add(slot);
                }
            }
        }
        int bodyRows = b.bodyRows > 0 ? b.bodyRows : Math.max(1, slots0b.size() / 9);
        int bodyCols = b.bodyCols > 0 ? b.bodyCols : Math.min(9, slots0b.size());
        this.bodyZone = new InvFillStrategy.BodyZone(bodyRows, bodyCols, Collections.unmodifiableList(slots0b));
        this.fillStrategy = b.fillStrategy != null ? b.fillStrategy : new RowMajorFillStrategy();

        this.itemRenderer = Objects.requireNonNull(b.itemRenderer, "NexItemRenderer required");
        // Paper-konform: Holder ist this (NexInventory)
        this.inventory = Bukkit.createInventory(this, size, titleSupplier.apply(null));
    }


    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public NexInventoryView openFor(Player player) {
        NexInventoryView view = new NexInventoryView(this, player);
        view.open();
        return view;
    }

    public String inventoryId() { return inventoryId; }
    public int rows() { return rows; }
    public int size() { return size; }
    public int updateIntervalTicks() { return updateIntervalTicks; }
    public boolean decorationEnabled() { return decorationEnabled; }
    public String decorationItemSpec() { return decorationItemSpec; }
    public Map<String, NexItemConfig> navigation() { return navigation; }
    public List<NexItemConfig> required() { return required; }
    public List<NexItemConfig> custom() { return custom; }
    public InvAlignment bodyAlignment() { return bodyAlignment; }
    public InvFillStrategy.BodyZone bodyZone() { return bodyZone; }
    public InvFillStrategy fillStrategy() { return fillStrategy; }
    public NexItemRenderer renderer() { return itemRenderer; }
    public Function<Player, String> titleSupplier() { return titleSupplier; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String inventoryId;
        private int rows = 6;
        private Function<Player, String> titleSupplier;
        private int updateIntervalTicks = 40;

        private boolean decorationEnabled = false;
        private String decorationItemSpec;

        private Map<String, NexItemConfig> navigation;
        private List<NexItemConfig> required;
        private List<NexItemConfig> custom;

        private InvAlignment bodyAlignment = InvAlignment.LEFT;
        private int bodyRows = 3;
        private int bodyCols = 7;
        private List<Integer> bodySlots0b;
        private InvFillStrategy fillStrategy;

        private NexItemRenderer itemRenderer;

        public Builder inventoryId(String id) { this.inventoryId = id; return this; }
        public Builder rows(int rows) { this.rows = rows; return this; }
        public Builder titleProvider(Function<Player, String> titleSupplier) { this.titleSupplier = titleSupplier; return this; }
        public Builder updateIntervalTicks(int ticks) { this.updateIntervalTicks = ticks; return this; }

        public Builder decoration(boolean enabled, String itemSpec) { this.decorationEnabled = enabled; this.decorationItemSpec = itemSpec; return this; }

        public Builder navigation(Map<String, NexItemConfig> nav) { this.navigation = nav; return this; }
        public Builder required(List<NexItemConfig> req) { this.required = req; return this; }
        public Builder custom(List<NexItemConfig> cus) { this.custom = cus; return this; }

        public Builder bodyAlignment(InvAlignment a) { this.bodyAlignment = a; return this; }
        public Builder bodyGrid(int rows, int cols) { this.bodyRows = rows; this.bodyCols = cols; return this; }
        public Builder bodySlots0b(List<Integer> slots) { this.bodySlots0b = slots; return this; }
        public Builder fillStrategy(InvFillStrategy s) { this.fillStrategy = s; return this; }

        public Builder itemRenderer(NexItemRenderer r) { this.itemRenderer = r; return this; }

        public NexInventory build() { return new NexInventory(this); }
    }

    // Hilfsfunktionen (intern)
    NexPageSource pageSourceFor(int totalBodyItems) {
        int pageSize = Math.max(1, bodyZone.slots.size());
        return new NexPageSource(totalBodyItems, pageSize);
    }
}
