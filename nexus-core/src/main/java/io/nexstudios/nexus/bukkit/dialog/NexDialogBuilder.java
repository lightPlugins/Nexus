package io.nexstudios.nexus.bukkit.dialog;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Version-agnostic builder interface for a dialog screen.
 *
 * Implementations live in the NMS / Paper modules and adapt this
 * interface to the underlying API.
 */
public interface NexDialogBuilder {

    enum DialogButtonRole {
        SUBMIT,
        CANCEL,
        CUSTOM
    }

    /**
     * Callback for a completed dialog.
     * Provides Player + {@link NexDialogResult}.
     */
    @FunctionalInterface
    interface DialogSubmitHandler extends BiConsumer<Player, NexDialogResult> {
        @Override
        void accept(Player player, NexDialogResult result);
    }

    // --- basic meta ---

    NexDialogBuilder id(String id);

    NexDialogBuilder title(Component title);

    NexDialogBuilder titleMiniMessage(String miniMessage);

    NexDialogBuilder subtitle(Component subtitle);

    NexDialogBuilder subtitleMiniMessage(String miniMessage);

    // --- body ---

    NexDialogBuilder clearBody();

    NexDialogBuilder addBodyLine(Component line);

    NexDialogBuilder addBodyLineMiniMessage(String miniMessage);

    NexDialogBuilder bodyMiniMessageLines(List<String> lines);

    // --- inputs ---

    NexDialogBuilder addTextInput(String key,
                                  Component label,
                                  String prefill,
                                  int width);

    // --- buttons -----

    NexDialogBuilder addButton(DialogButtonRole role,
                               Component label,
                               Component tooltip,
                               int weight,
                               boolean closeOnClick);

    default NexDialogBuilder addSubmitButton(Component label,
                                             Component tooltip,
                                             int weight,
                                             boolean closeOnClick) {
        return addButton(DialogButtonRole.SUBMIT, label, tooltip, weight, closeOnClick);
    }

    default NexDialogBuilder addCancelButton(Component label,
                                             Component tooltip,
                                             int weight,
                                             boolean closeOnClick) {
        return addButton(DialogButtonRole.CANCEL, label, tooltip, weight, closeOnClick);
    }

    // --- options / behavior ---

    NexDialogBuilder closeOnSubmit(boolean closeOnSubmit);

    NexDialogBuilder closeOnCancel(boolean closeOnCancel);

    /**
     * Optional: which text input key should be treated as "primary".
     * Implementations may use this for convenience methods.
     */
    NexDialogBuilder primaryTextKey(String key);

    // --- config import ---

    /**
     * Populates the builder from a single dialog section (the content of "dialog:").
     */
    NexDialogBuilder fromConfig(ConfigurationSection dialogSection);

    // --- callbacks ---

    NexDialogBuilder onSubmit(DialogSubmitHandler handler);

    NexDialogBuilder onCancel(Consumer<Player> handler);

    // --- show ---

    void open(Player player);

    /**
     * Future with the complete {@link NexDialogResult}.
     * - on submit: NexDialogResult
     * - on cancel: null
     */
    default CompletableFuture<NexDialogResult> openWithFuture(Player player) {
        Objects.requireNonNull(player, "player");
        CompletableFuture<NexDialogResult> future = new CompletableFuture<>();

        this.onSubmit((p, result) -> {
            if (!future.isDone()) future.complete(result);
        });
        this.onCancel(p -> {
            if (!future.isDone()) future.complete(null);
        });

        this.open(player);
        return future;
    }
}