package io.nexstudios.nexus.bukkit.redis;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Central Redis API provided by Nexus for itself and other plugins.
 *
 * This interface is meant to be registered as a Bukkit service.
 * Other plugins should ONLY depend on this interface, never on the
 * concrete implementation or underlying Redis client.
 */
public interface NexusRedisService {

    /**
     * Publishes a message asynchronously on the given channel.
     *
     * Implementations should:
     * - perform I/O off the main thread
     * - serialize the message to the chosen wire format (e.g. JSON)
     * - return a future that completes with the Redis PUBLISH result
     *
     * @param channel the Redis channel (e.g. "nexus:levels")
     * @param message the message to send
     * @return a future with the number of clients that received the message
     */
    CompletableFuture<Long> publish(String channel, NexusRedisMessage message);

    /**
     * Subscribes a listener to the given channel.
     *
     * Implementations are responsible for:
     * - mapping channel -> listener
     * - decoding incoming messages into NexusRedisMessage
     *
     * @param channel  Redis channel to subscribe to
     * @param listener callback invoked for each incoming message
     * @return a subscription ID that can later be used to unsubscribe
     */
    UUID subscribe(String channel, NexusRedisListener listener);

    /**
     * Cancels a previously registered subscription.
     *
     * Implementations should safely ignore unknown IDs.
     *
     * @param subscriptionId ID returned by {@link #subscribe(String, NexusRedisListener)}
     */
    void unsubscribe(UUID subscriptionId);

    /**
     * @return true if the underlying Redis client is currently connected
     *         and able to perform operations; false otherwise.
     */
    boolean isConnected();

    /**
     * Releases all Redis resources and closes connections.
     *
     * Called from the owning plugin's onDisable().
     * After shutdown, publish/subscribe calls should either no-op
     * or fail fast with clear exceptions.
     */
    void shutdown();
}