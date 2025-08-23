package io.nexstudios.nexus.bukkit.inv.fill;

import java.util.*;

public class RowMajorFillStrategy implements InvFillStrategy {

    @Override
    public Map<Integer, Integer> assignSlots(BodyZone zone, int pageIndex, int pageSize, int pageOffset, int itemsOnPage, InvAlignment alignment) {
        Map<Integer, Integer> assignment = new HashMap<>();
        if (itemsOnPage <= 0) return assignment;

        // Row-aware: Slots nach Zeilen gruppieren, Reihenfolge beibehalten
        Map<Integer, List<Integer>> rows = new TreeMap<>(); // Key = Zeilennummer (slot/9)
        for (Integer s0 : zone.slots) {
            int row = s0 / 9;
            rows.computeIfAbsent(row, k -> new ArrayList<>()).add(s0);
        }
        // innerhalb jeder Zeile Slots aufsteigend
        for (List<Integer> line : rows.values()) {
            line.sort(Comparator.naturalOrder());
        }

        int placed = 0;
        for (List<Integer> line : rows.values()) {
            if (placed >= itemsOnPage) break;

            int capacity = line.size();
            int remaining = Math.min(itemsOnPage - placed, capacity);

            int padLeft = switch (alignment) {
                case LEFT   -> 0;
                case RIGHT  -> Math.max(0, capacity - remaining);
                case CENTER -> Math.max(0, (capacity - remaining) / 2);
            };

            // place remaining items in this row starting at padLeft
            for (int i = 0; i < remaining && placed < itemsOnPage; i++) {
                int idx = padLeft + i;
                if (idx < 0 || idx >= capacity) continue;
                int slot = line.get(idx);
                assignment.put(slot, placed); // Itemindex innerhalb der Seite
                placed++;
            }
        }
        return assignment;
    }
}

