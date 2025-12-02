package io.nexstudios.nexus.bukkit.redis;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a generic Redis message exchanged via NexusRedis.
 *
 * This is intentionally minimal and implementation-agnostic:
 * - "type" is a logical message type (e.g. "PLAYER_LEVEL_UPDATE")
 * - "origin" can be used to tag the sending server/instance
 * - "payload" holds arbitrary key/value data
 */
public final class NexusRedisMessage {

    private final String type;
    private final String origin;
    private final Map<String, Object> payload;

    public NexusRedisMessage(String type, String origin, Map<String, Object> payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.origin = origin; // may be null if you don't care about origin
        this.payload = (payload == null)
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(payload);
    }

    /**
     * Logical message type (e.g. "PLAYER_LEVEL_UPDATE").
     */
    public String getType() {
        return type;
    }

    /**
     * Optional origin identifier (e.g. "survival-1", "proxy-1").
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Arbitrary payload data, typically simple JSON-like key/value pairs.
     */
    public Map<String, Object> getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "NexusRedisMessage{" +
                "type='" + type + '\'' +
                ", origin='" + origin + '\'' +
                ", payload=" + payload +
                '}';
    }
}