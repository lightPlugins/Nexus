package io.nexstudios.nexus.bukkit.redis;

/**
 * Listener for incoming Redis messages on a subscribed channel.
 *
 * Implementations should be fast and non-blocking.
 * If you need to interact with Bukkit API that must run on the main thread,
 * schedule back to the primary thread manually (e.g. Bukkit scheduler).
 */
@FunctionalInterface
public interface NexusRedisListener {

    /**
     * Called when a message is received on a subscribed channel.
     *
     * @param channel the Redis channel name
     * @param message the decoded message object
     */
    void onMessage(String channel, NexusRedisMessage message);
}