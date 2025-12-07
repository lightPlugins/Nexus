package io.nexstudios.nexus.bukkit.conditions;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public interface NexusCondition {

    JavaPlugin getPlugin();

    /**
     * Asynchrone Condition-Prüfung.
     * Default-Implementierung ruft die synchrone Variante auf und wrapped das Ergebnis.
     */
    default CompletableFuture<Boolean> checkAsync(NexusConditionContext context) {
        boolean result = checkSync(context);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Synchrone Prüfung, die von einfachen Conditions überschrieben werden kann.
     * Wird von der Default-Implementierung von checkAsync(...) aufgerufen.
     */
    default boolean checkSync(NexusConditionContext context) {
        throw new UnsupportedOperationException("checkSync not implemented for " + getClass().getName());
    }

    /**
     * Nachricht senden, wenn die Condition nicht erfüllt ist.
     */
    void sendMessage(NexusConditionContext context);
}