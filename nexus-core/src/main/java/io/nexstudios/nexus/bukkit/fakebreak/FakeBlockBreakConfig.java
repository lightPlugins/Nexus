package io.nexstudios.nexus.bukkit.fakebreak;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.time.Duration;

/**
 * Konfiguration, wie ein Fake-Block-Abbau ablaufen soll.
 */
public final class FakeBlockBreakConfig {

    private final Duration breakDuration;
    private final Material resultMaterial;
    private final boolean resetCracksAfterFinish;

    private FakeBlockBreakConfig(Builder builder) {
        this.breakDuration = builder.breakDuration;
        this.resultMaterial = builder.resultMaterial;
        this.resetCracksAfterFinish = builder.resetCracksAfterFinish;
    }

    /**
     * Gesamtdauer des Fake-Abbaus.
     */
    public Duration breakDuration() {
        return breakDuration;
    }

    /**
     * Welcher Block für den Spieler am Ende angezeigt wird.
     * Typischerweise Material.AIR.
     */
    public Material resultMaterial() {
        return resultMaterial;
    }

    /**
     * Ob nach dem Abschluss noch einmal ein "stage = -1" / Reset
     * geschickt werden soll.
     */
    public boolean resetCracksAfterFinish() {
        return resetCracksAfterFinish;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Duration breakDuration = Duration.ofMillis(500); // default
        private Material resultMaterial = Material.AIR;
        private boolean resetCracksAfterFinish = true;

        public Builder breakDuration(Duration breakDuration) {
            this.breakDuration = breakDuration;
            return this;
        }

        public Builder resultMaterial(Material resultMaterial) {
            this.resultMaterial = resultMaterial;
            return this;
        }

        public Builder resetCracksAfterFinish(boolean reset) {
            this.resetCracksAfterFinish = reset;
            return this;
        }

        public FakeBlockBreakConfig build() {
            return new FakeBlockBreakConfig(this);
        }
    }

    public static FakeBlockBreakConfig fromBlockHardness(Block block, double breakSpeedAttribute, double baseHardness) {
        // Hier könntest du später eine genauere Formel einbauen.
        // Placeholder: je höher breakSpeedAttribute, desto kürzer die Dauer.
        double speed = Math.max(0.1, breakSpeedAttribute);
        long millis = (long) ((baseHardness * 1500L) / speed);
        return builder()
                .breakDuration(Duration.ofMillis(millis))
                .resultMaterial(Material.AIR)
                .resetCracksAfterFinish(true)
                .build();
    }
}