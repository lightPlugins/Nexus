package io.nexstudios.nexus.bukkit.redis;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple Jedis-based implementation of {@link NexusRedisService}.
 *
 * Responsibilities:
 * - Manage a single shared Jedis connection for PUB (short-lived)
 *   and a dedicated SUB connection running on its own thread.
 * - Maintain in-memory subscriptions (UUID -> listener) and dispatch
 *   messages to them when Redis delivers a payload.
 * - Perform network I/O off the Bukkit main thread.
 *
 * This implementation is intentionally minimal and can be extended
 * later with better JSON handling (Jackson/Gson), connection pooling,
 * authentication, etc.
 */
public class JedisNexusRedisService implements NexusRedisService, Closeable {

    private final NexusPlugin plugin;
    private final String host;
    private final int port;
    private final String password; // may be null/empty for no auth
    private final int database;    // usually 0 if you don't care

    private final ExecutorService publishExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Jedis subscriberJedis;
    private Thread subscriberThread;
    private final InternalPubSub pubSub;

    // subscriptionId -> (channel, listener)
    private final ConcurrentMap<UUID, Subscription> subscriptions = new ConcurrentHashMap<>();

    private static final class Subscription {
        final String channel;
        final NexusRedisListener listener;

        Subscription(String channel, NexusRedisListener listener) {
            this.channel = channel;
            this.listener = listener;
        }
    }

    /**
     * Internal JedisPubSub that dispatches incoming messages
     * to the registered NexusRedisListeners.
     */
    private final class InternalPubSub extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            // Decode wire format into NexusRedisMessage and dispatch
            NexusRedisMessage decoded;
            try {
                decoded = decodeMessage(message);
            } catch (Exception e) {
                NexusPlugin.nexusLogger.error("[NexusRedis] Failed to decode message on channel " + channel + ": " + e.getMessage());
                return;
            }

            for (Subscription sub : subscriptions.values()) {
                if (!sub.channel.equals(channel)) continue;
                try {
                    sub.listener.onMessage(channel, decoded);
                } catch (Throwable t) {
                    NexusPlugin.nexusLogger.error("[NexusRedis] Listener threw exception on channel " + channel + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        }
    }

    public JedisNexusRedisService(NexusPlugin plugin,
                                  String host,
                                  int port,
                                  String password,
                                  int database) {
        this.plugin = plugin;
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
        this.password = password;
        this.database = database;

        this.publishExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                r -> {
                    Thread t = new Thread(r, "NexusRedis-Publish");
                    t.setDaemon(true);
                    return t;
                }
        );
        this.pubSub = new InternalPubSub();
    }

    /**
     * Starts the Jedis subscriber thread and establishes the subscription connection.
     * Should be called once during plugin startup (onEnable).
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        subscriberThread = new Thread(this::runSubscriberLoop, "NexusRedis-Subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    private void runSubscriberLoop() {
        while (running.get()) {
            try (Jedis jedis = new Jedis(host, port)) {
                if (password != null && !password.isEmpty()) {
                    jedis.auth(password);
                }
                if (database != 0) {
                    jedis.select(database);
                }

                this.subscriberJedis = jedis;

                Set<String> channels = collectChannelsSnapshot();
                if (channels.isEmpty()) {
                    // No subscriptions yet; sleep a bit and retry
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }

                String[] channelArray = channels.toArray(new String[0]);
                NexusPlugin.nexusLogger.info("[NexusRedis] Subscribing to channels: " + channels);
                jedis.subscribe(pubSub, channelArray);

            } catch (Exception e) {
                if (!running.get()) {
                    return;
                }
                NexusPlugin.nexusLogger.error("[NexusRedis] Subscriber loop error: " + e.getMessage());
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException ignored) {
                }
            } finally {
                this.subscriberJedis = null;
            }
        }
    }

    private Set<String> collectChannelsSnapshot() {
        Set<String> out = new HashSet<>();
        for (Subscription s : subscriptions.values()) {
            out.add(s.channel);
        }
        return out;
    }

    @Override
    public CompletableFuture<Long> publish(String channel, NexusRedisMessage message) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(message, "message");

        CompletableFuture<Long> future = new CompletableFuture<>();
        if (!running.get()) {
            future.completeExceptionally(new IllegalStateException("NexusRedisService is not running"));
            return future;
        }

        publishExecutor.submit(() -> {
            try (Jedis jedis = new Jedis(host, port)) {
                if (password != null && !password.isEmpty()) {
                    jedis.auth(password);
                }
                if (database != 0) {
                    jedis.select(database);
                }

                String payload = encodeMessage(message);
                Long receivers = jedis.publish(channel, payload);
                future.complete(receivers);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    @Override
    public UUID subscribe(String channel, NexusRedisListener listener) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(listener, "listener");

        UUID id = UUID.randomUUID();
        subscriptions.put(id, new Subscription(channel, listener));

        // If subscriber is already running, we need to resubscribe with the new channel set.
        // Simplest approach: unsubscribe all and let the loop restart.
        Jedis sj = this.subscriberJedis;
        if (sj != null) {
            try {
                // WICHTIG: unsubscriben macht man auf dem JedisPubSub, nicht auf dem Jedis.
                pubSub.unsubscribe();
            } catch (Exception ignored) {
            }
        }

        return id;
    }

    @Override
    public void unsubscribe(UUID subscriptionId) {
        if (subscriptionId == null) return;
        subscriptions.remove(subscriptionId);
        Jedis sj = this.subscriberJedis;
        if (sj != null) {
            try {
                // ebenfalls: auf dem PubSub unsubscriben, damit subscribe() im Loop zurückkehrt
                pubSub.unsubscribe();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (!running.get()) return false;
        try (Jedis jedis = new Jedis(host, port)) {
            if (password != null && !password.isEmpty()) {
                jedis.auth(password);
            }
            if (database != 0) {
                jedis.select(database);
            }
            String pong = jedis.ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        // stop new publishes
        publishExecutor.shutdown();

        // stop subscriber
        if (subscriberJedis != null) {
            try {
                subscriberJedis.close();
            } catch (Exception ignored) {
            }
        }
        if (subscriberThread != null) {
            try {
                subscriberThread.interrupt();
            } catch (Exception ignored) {
            }
        }

        subscriptions.clear();
    }

    @Override
    public void close() throws IOException {
        shutdown();
    }

    /**
     * Encodes a NexusRedisMessage into a wire format string.
     * Currently a very simple JSON-like format; replace with
     * a proper JSON library (Jackson/Gson) if needed.
     */
    private String encodeMessage(NexusRedisMessage msg) {
        // Very simple manual JSON; you probably want Jackson in production.
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"").append(escape(msg.getType())).append("\"");
        if (msg.getOrigin() != null) {
            sb.append(",\"origin\":\"").append(escape(msg.getOrigin())).append("\"");
        }
        sb.append(",\"payload\":{");
        boolean first = true;
        for (Map.Entry<String, Object> e : msg.getPayload().entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v.toString());
            } else {
                sb.append("\"").append(escape(String.valueOf(v))).append("\"");
            }
        }
        sb.append("}}");
        return sb.toString();
    }

    /**
     * Decodes a wire format string into NexusRedisMessage.
     * This expects the format produced by encodeMessage.
     * Für eine robuste API solltest du später auf eine echte JSON-Library wechseln.
     */
    private NexusRedisMessage decodeMessage(String raw) {
        String type = "unknown";
        String origin = null;
        Map<String, Object> payload = Collections.emptyMap();

        try {
            int typeIdx = raw.indexOf("\"type\"");
            if (typeIdx >= 0) {
                int colon = raw.indexOf(":", typeIdx);
                int start = raw.indexOf("\"", colon + 1) + 1;
                int end = raw.indexOf("\"", start);
                if (start > 0 && end > start) {
                    type = unescape(raw.substring(start, end));
                }
            }
            int originIdx = raw.indexOf("\"origin\"");
            if (originIdx >= 0) {
                int colon = raw.indexOf(":", originIdx);
                int start = raw.indexOf("\"", colon + 1) + 1;
                int end = raw.indexOf("\"", start);
                if (start > 0 && end > start) {
                    origin = unescape(raw.substring(start, end));
                }
            }

            // payload parsen (erwartet {...} wie in encodeMessage)
            int payloadIdx = raw.indexOf("\"payload\"");
            if (payloadIdx >= 0) {
                int colon = raw.indexOf(":", payloadIdx);
                int startObj = raw.indexOf("{", colon + 1);
                if (startObj >= 0) {
                    // Ende des Payload-Objekts finden: passende schließende Klammer
                    int depth = 0;
                    int endObj = -1;
                    for (int i = startObj; i < raw.length(); i++) {
                        char c = raw.charAt(i);
                        if (c == '{') {
                            depth++;
                        } else if (c == '}') {
                            depth--;
                            if (depth == 0) {
                                endObj = i;
                                break;
                            }
                        }
                    }

                    if (endObj > startObj) {
                        String inner = raw.substring(startObj + 1, endObj).trim();
                        payload = parsePayload(inner);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new NexusRedisMessage(type, origin, payload);
    }

    /**
     * Mini-Parser für das Payload-Object, das encodeMessage() produziert.
     * Unterstützt:
     * - Strings: "text"
     * - Zahlen: 1, 1.23
     * - Boolean: true/false
     * - null
     * KEINE verschachtelten Objekte/Arrays.
     */
    private Map<String, Object> parsePayload(String inner) {
        Map<String, Object> result = new HashMap<>();
        if (inner.isEmpty()) {
            return result;
        }

        List<String> entries = splitTopLevel(inner);
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            int colonIdx = trimmed.indexOf(':');
            if (colonIdx <= 0) continue;

            String keyPart = trimmed.substring(0, colonIdx).trim();
            String valuePart = trimmed.substring(colonIdx + 1).trim();

            if (!keyPart.startsWith("\"") || !keyPart.endsWith("\"")) {
                continue;
            }

            String key = unescape(keyPart.substring(1, keyPart.length() - 1));
            Object value = parseJsonValue(valuePart);
            result.put(key, value);
        }
        return result;
    }

    /**
     * Splittet ein einfaches JSON-Objekt am Top-Level nach Kommas.
     * Kommas innerhalb von Strings werden ignoriert.
     */
    private List<String> splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }

            if (c == '\\') {
                current.append(c);
                escaping = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }

            if (c == ',' && !inString) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            parts.add(current.toString());
        }

        return parts;
    }

    /**
     * Parsen eines sehr kleinen JSON-Werte-Sets: String, Number, Boolean, null.
     */
    private Object parseJsonValue(String valuePart) {
        String v = valuePart.trim();
        if (v.isEmpty()) return null;

        // String
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            String inner = v.substring(1, v.length() - 1);
            return unescape(inner);
        }

        // null
        if ("null".equalsIgnoreCase(v)) {
            return null;
        }

        // boolean
        if ("true".equalsIgnoreCase(v)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(v)) {
            return Boolean.FALSE;
        }

        // number (try long, then double)
        try {
            if (v.contains(".") || v.contains("e") || v.contains("E")) {
                return Double.parseDouble(v);
            } else {
                return Long.parseLong(v);
            }
        } catch (NumberFormatException ignored) {
        }

        // fallback: raw String
        return v;
    }

    private String escape(String in) {
        return in.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String in) {
        return in.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}