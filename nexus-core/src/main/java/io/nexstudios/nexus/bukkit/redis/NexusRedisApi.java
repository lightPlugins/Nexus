package io.nexstudios.nexus.bukkit.redis;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicesManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Static convenience API for accessing NexusRedisService.
 *
 * Internally this ALWAYS resolves the service via Bukkit's ServicesManager.
 * This keeps the implementation swappable and supports clean plugin reloads.
 */
public final class NexusRedisApi {

    private NexusRedisApi() {
    }

    /**
     * Returns the currently registered NexusRedisService, if any.
     *
     * Plugins may use this to check availability before using Redis.
     */
    public static Optional<NexusRedisService> getService() {
        ServicesManager sm = Bukkit.getServer().getServicesManager();
        NexusRedisService svc = sm.load(NexusRedisService.class);
        return Optional.ofNullable(svc);
    }

    /**
     * Convenience wrapper around {@link NexusRedisService#publish(String, NexusRedisMessage)}.
     *
     * If the service is not available, the returned future completes exceptionally.
     */
    public static CompletableFuture<Long> publish(String channel, NexusRedisMessage message) {
        return getService()
                .map(svc -> svc.publish(channel, message))
                .orElseGet(() -> {
                    CompletableFuture<Long> failed = new CompletableFuture<>();
                    failed.completeExceptionally(
                            new IllegalStateException("NexusRedisService is not available")
                    );
                    return failed;
                });
    }

    /**
     * Convenience wrapper around {@link NexusRedisService#subscribe(String, NexusRedisListener)}.
     *
     * @return Optional containing the subscription ID if the service is available,
     *         or empty if the service is missing.
     */
    public static Optional<UUID> subscribe(String channel, NexusRedisListener listener) {
        return getService()
                .map(svc -> svc.subscribe(channel, listener));
    }

    /**
     * Convenience wrapper around {@link NexusRedisService#unsubscribe(UUID)}.
     *
     * If the service is not available, this call is silently ignored.
     */
    public static void unsubscribe(UUID subscriptionId) {
        getService().ifPresent(svc -> svc.unsubscribe(subscriptionId));
    }

    /**
     * @return true if a service is present and reports itself as connected.
     */
    public static boolean isConnected() {
        return getService()
                .map(NexusRedisService::isConnected)
                .orElse(false);
    }
}