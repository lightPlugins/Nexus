package io.nexstudios.nexus.bukkit.inv.fill;

import java.util.List;
import java.util.Map;

public interface InvFillStrategy {

    Map<Integer, Integer> assignSlots(
            BodyZone zone,
            int pageIndex,
            int pageSize,
            int pageOffset,      // globaler Offset in der Gesamtliste
            int itemsOnPage,     // Anzahl der Items auf dieser Seite
            InvAlignment alignment
    );

    final class BodyZone {
        public final int rows;
        public final int cols;
        public final List<Integer> slots; // intern 0-basiert, row-major

        public BodyZone(int rows, int cols, List<Integer> slots) {
            this.rows = rows;
            this.cols = cols;
            this.slots = slots;
        }
    }
}

