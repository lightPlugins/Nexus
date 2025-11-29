package io.nexstudios.nexus.bukkit.fakebreak;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.time.Instant;

/**
 * Repräsentiert einen laufenden Fake-Block-Abbau.
 */
public interface FakeBlockBreakSession {

    Player player();

    Block block();

    FakeBlockBreakConfig config();

    Instant startTime();

    /**
     * Bricht den Fake-Abbau aktiv ab. Setzt Cracks zurück
     * und zeigt dem Spieler wieder den echten Block an.
     */
    void cancel();

    /**
     * @return true, wenn die Session bereits beendet/abgebrochen wurde.
     */
    boolean isFinished();
}