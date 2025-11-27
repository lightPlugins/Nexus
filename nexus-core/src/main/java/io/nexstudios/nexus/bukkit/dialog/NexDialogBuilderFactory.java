package io.nexstudios.nexus.bukkit.dialog;

/**
 * Factory für {@link NexDialogBuilder}, wird vom NMS-Provider bereitgestellt.
 */
public interface NexDialogBuilderFactory {

    /**
     * Erstellt einen neuen, noch nicht konfigurierten Dialog-Builder.
     */
    NexDialogBuilder create();
}