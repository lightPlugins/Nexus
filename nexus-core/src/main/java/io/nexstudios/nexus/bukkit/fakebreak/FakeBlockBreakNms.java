package io.nexstudios.nexus.bukkit.fakebreak;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * Abstraktion für alle NMS-Packet-Operationen, die zum
 * Fake-Block-Abbau benötigt werden.
 *
 * Wird pro Version (v1_21_R8 etc.) implementiert.
 */
public interface FakeBlockBreakNms {

    /**
     * Sendet dem Spieler den aktuellen "Crack"-Status eines Blocks.
     *
     * @param player  Zielspieler
     * @param location Position des Blocks
     * @param stage   0-9 = Crack-Stufen, -1 = Reset/entfernen
     *                (je nach NMS-Version anpassbar)
     */
    void sendBlockBreakStage(Player player, Location location, int stage);

    /**
     * Sendet eine Blockänderung NUR an den angegebenen Spieler.
     * Der Serverzustand bleibt unverändert.
     *
     * @param player   Zielspieler
     * @param location Position des Blocks
     * @param data     BlockData, das der Spieler sehen soll
     */
    void sendFakeBlockChange(Player player, Location location, BlockData data);

    /**
     * Stellt sicher, dass der Spieler wieder den echten Server-Block
     * an dieser Position sieht (z.B. wenn wir den Fake-Zustand
     * zurücksetzen wollen).
     */
    void sendResetBlock(Player player, Location location);
}