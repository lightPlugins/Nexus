package io.nexstudios.nexus.bukkit.dialog;

import org.bukkit.entity.Player;

/**
 * Callback für einen Dialog mit String-Eingabe.
 */
@FunctionalInterface
public interface DialogStringResult {

    /**
     * Wird aufgerufen, wenn der Spieler den Dialog mit einem Text bestätigt.
     *
     * @param player Spieler, der den Dialog benutzt hat
     * @param value  vom Spieler eingegebener Text
     */
    void handle(Player player, String value);
}