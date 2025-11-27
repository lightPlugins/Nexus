package io.nexstudios.nexus.bukkit.dialog;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * High-Level vordefinierte Dialog-Presets.
 *
 * Die konkrete Implementierung sitzt im NMS-Modul und verwendet intern
 * {@link NexDialogBuilder} + die Paper/NMS Dialog API.
 */
public interface NexDialogPresets {

    /**
     * Einfacher "Phrase"-Dialog:
     * <ul>
     *   <li>Titel</li>
     *   <li>Frage/Body (MiniMessage)</li>
     *   <li>Text-Eingabefeld</li>
     *   <li>"Abschicken"-Button</li>
     *   <li>"Abbrechen"-Button</li>
     * </ul>
     *
     * Ergebnis wird per Callback geliefert.
     */
    void openPhrase(
            Player player,
            Component title,
            String questionMiniMessage,
            DialogStringResult onSubmit,
            @Nullable Consumer<Player> onCancel
    );

    /**
     * Asynchrone Variante des Phrase-Dialogs.
     *
     * <ul>
     *   <li>Bei erfolgreicher Eingabe: Future mit String</li>
     *   <li>Bei Abbrechen: Future mit {@code null}</li>
     * </ul>
     */
    CompletableFuture<String> openPhraseAsync(
            Player player,
            Component title,
            String questionMiniMessage
    );

    /*
     * Platz für weitere Presets in Zukunft, z.B.:
     *
     * CompletableFuture<Boolean> openConfirmAsync(...);
     * <T> CompletableFuture<T> openChoiceAsync(...);
     * CompletableFuture<Double> openNumberAsync(...);
     */

    void openCenterMessage(
            Player player,
            Component title,
            Component body,
            Component button
    );
}