package io.nexstudios.nexus.bukkit.fakebreak;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Öffentlicher Einstiegspunkt: damit kannst du Fake-Abbau
 * für einen Block bei einem Spieler starten.
 */
public interface FakeBlockBreakService {

    /**
     * Startet einen Fake-Block-Abbau bei einem Spieler.
     *
     * - Server-Block bleibt unverändert.
     * - Nur der Spieler sieht Cracks + am Ende den "neuen" Block.
     *
     * @param player der Spieler
     * @param block  der echte Block in der Welt
     * @param config Konfiguration (Dauer, Ergebnisblock, etc.)
     * @return eine Session, über die du ggf. abbrechen kannst
     */
    FakeBlockBreakSession startFakeBreak(Player player, Block block, FakeBlockBreakConfig config);

    /**
     * Bricht alle laufenden Fake-Abbau-Vorgänge für diesen Spieler
     * an genau diesem Block ab (falls vorhanden).
     *
     * @return true, wenn etwas abgebrochen wurde
     */
    boolean cancelFakeBreak(Player player, Block block);

    /**
     * Sucht eine laufende Session, falls vorhanden.
     */
    @Nullable
    FakeBlockBreakSession getSession(Player player, Block block);
}