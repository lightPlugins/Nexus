package io.nexstudios.nexus.bukkit.inv;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NexInventoryManager {

    private static final NexInventoryManager INSTANCE = new NexInventoryManager();
    public static NexInventoryManager get() { return INSTANCE; }

    private final Map<UUID, NexInventoryView> views = new ConcurrentHashMap<>();

    public void register(UUID playerId, NexInventoryView view) {
        views.put(playerId, view);
    }

    public void unregister(UUID playerId) {
        views.remove(playerId);
    }

    public NexInventoryView viewOf(UUID playerId) {
        return views.get(playerId);
    }

    public void closeAndReopenAll() {
        // Schließt alle derzeit offenen Views. Ein erneutes Öffnen mit neuer Config
        // erfolgt außerhalb (z. B. durch deinen Reload-Flow).
        for (Map.Entry<UUID, NexInventoryView> e : views.entrySet()) {
            NexInventoryView v = e.getValue();
            if (v != null) {
                Player p = v.player();
                if (p != null && p.isOnline()) {
                    p.closeInventory();
                }
            }
        }
        views.clear();
    }
}
