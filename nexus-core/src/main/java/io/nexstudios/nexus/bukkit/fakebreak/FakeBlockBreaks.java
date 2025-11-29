package io.nexstudios.nexus.bukkit.fakebreak;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Statischer Helper für den Zugriff auf den FakeBlockBreakService.
 *
 * - holt sich den FakeBlockBreakNms-Adapter aus NexServices
 * - baut daraus einmalig einen FakeBlockBreakManager
 */
public final class FakeBlockBreaks {

    private static volatile FakeBlockBreakService INSTANCE;

    private FakeBlockBreaks() {}

    public static FakeBlockBreakService service() {
        FakeBlockBreakService inst = INSTANCE;
        if (inst != null) {
            return inst;
        }
        synchronized (FakeBlockBreaks.class) {
            inst = INSTANCE;
            if (inst == null) {
                // 1) NMS-Adapter aus der Registry holen (kommt aus nexus-nms)
                FakeBlockBreakNms nms = NexServices.get(FakeBlockBreakNms.class);

                // 2) Versionunabhängigen Manager im Core bauen
                inst = new FakeBlockBreakManager(NexusPlugin.getInstance(), nms);
                INSTANCE = inst;
            }
            return inst;
        }
    }

    public static FakeBlockBreakSession start(Player player, Block block, FakeBlockBreakConfig config) {
        return service().startFakeBreak(player, block, config);
    }

    public static boolean cancel(Player player, Block block) {
        return service().cancelFakeBreak(player, block);
    }
}