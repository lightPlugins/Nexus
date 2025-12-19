package io.nexstudios.nexus.bukkit.costs;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * A single cost implementation (e.g. "money", "item", ...).
 *
 * Implementations should only perform the actual cost logic (checking & paying),
 * but must NOT send messages or run actions. This is handled by the factory.
 */
public interface NexusCost {

    JavaPlugin getPlugin();

    /**
     * Synchronous cost resolution.
     * This method must always be implemented by cost implementations.
     */
    CostResult resolveSync(NexusCostContext context);

    /**
     * Synchronous preview of a cost, used for display (e.g. in item lore).
     *
     * Implementations should:
     * - NOT modify any state (no money withdraw, no item removal, etc.)
     * - fill CostResult.params() with all placeholder values needed
     *   for the "placeholder" string in the config.
     *
     * Default implementation returns an empty success result.
     * Implementations are strongly encouraged to override this.
     */
    default CostResult previewSync(NexusCostContext context) {
        return CostResult.success();
    }

    /**
     * Asynchronous cost resolution.
     * Default implementation calls the synchronous variant and wraps the result.
     */
    default CompletableFuture<CostResult> resolveAsync(NexusCostContext context) {
        CostResult result = resolveSync(context);
        return CompletableFuture.completedFuture(result);
    }
}