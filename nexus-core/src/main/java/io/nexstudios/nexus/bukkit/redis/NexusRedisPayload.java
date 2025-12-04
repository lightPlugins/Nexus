package io.nexstudios.nexus.bukkit.redis;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public final class NexusRedisPayload {

    private final Map<String, Object> values = new HashMap<>();

    private NexusRedisPayload() {
    }

    public static NexusRedisPayload create() {
        return new NexusRedisPayload();
    }

    public static NexusRedisPayload fromMessage(NexusRedisMessage message) {
        Objects.requireNonNull(message, "message");
        NexusRedisPayload p = new NexusRedisPayload();
        if (message.getPayload() != null) {
            p.values.putAll(message.getPayload());
        }
        return p;
    }

    public static NexusRedisPayload fromMap(Map<String, Object> map) {
        NexusRedisPayload p = new NexusRedisPayload();
        if (map != null) {
            p.values.putAll(map);
        }
        return p;
    }

    public NexusRedisPayload put(String key, Object value) {
        Objects.requireNonNull(key, "key");
        values.put(key, value);
        return this;
    }

    public Map<String, Object> toMap() {
        return Map.copyOf(values);
    }

    public NexusRedisMessage toMessage(String type, String origin) {
        return new NexusRedisMessage(type, origin, toMap());
    }

    public String getString(String key) {
        Object v = values.get(key);
        return v != null ? v.toString() : null;
    }

    public String getString(String key, String defaultValue) {
        String v = getString(key);
        return v != null ? v : defaultValue;
    }

    public Long getLong(String key) {
        Object v = values.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
            }
        }
        // kein Wert oder nicht parsebar
        return null;
    }

    public long getLong(String key, long defaultValue) {
        Long v = getLong(key);
        return v != null ? v : defaultValue;
    }

    public Double getDouble(String key) {
        Object v = values.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public double getDouble(String key, double defaultValue) {
        Double v = getDouble(key);
        return v != null ? v : defaultValue;
    }

    public Integer getInt(String key) {
        Object v = values.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public int getInt(String key, int defaultValue) {
        Integer v = getInt(key);
        return v != null ? v : defaultValue;
    }

    public Boolean getBoolean(String key) {
        Object v = values.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return null;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean v = getBoolean(key);
        return v != null ? v : defaultValue;
    }

    public UUID getUuid(String key) {
        Object v = values.get(key);
        switch (v) {
            case null -> {
                return null;
            }
            case UUID uuid -> {
                return uuid;
            }
            case String s -> {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
            default -> {
            }
        }
        return null;
    }

    public UUID getUuid(String key, UUID defaultValue) {
        UUID v = getUuid(key);
        return v != null ? v : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object v = values.get(key);
        if (v == null) return null;
        if (type.isInstance(v)) {
            return (T) v;
        }
        return null;
    }

    /**
     * Speichert eine Bukkit-Location in der Payload.
     * Die Daten werden unter Schlüsseln "<key>.world", "<key>.x", ... abgelegt.
     */
    public NexusRedisPayload putLocation(String key, Location location) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location world may not be null");
        }

        String base = key.endsWith(".") ? key.substring(0, key.length() - 1) : key;

        values.put(base + ".world", location.getWorld().getName());
        values.put(base + ".x", location.getX());
        values.put(base + ".y", location.getY());
        values.put(base + ".z", location.getZ());
        values.put(base + ".yaw", location.getYaw());
        values.put(base + ".pitch", location.getPitch());
        return this;
    }

    /**
     * Liest eine Location aus der Payload, die mit {@link #putLocation(String, Location)} gespeichert wurde.
     * Gibt null zurück, wenn Welt oder Koordinaten nicht vollständig sind.
     */
    public Location getLocation(String key) {
        String base = key.endsWith(".") ? key.substring(0, key.length() - 1) : key;

        String worldName = getString(base + ".world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        Double x = getDouble(base + ".x");
        Double y = getDouble(base + ".y");
        Double z = getDouble(base + ".z");
        Double yaw = getDouble(base + ".yaw");
        Double pitch = getDouble(base + ".pitch");

        if (x == null || y == null || z == null) {
            return null;
        }

        float yawF = yaw != null ? yaw.floatValue() : 0.0f;
        float pitchF = pitch != null ? pitch.floatValue() : 0.0f;

        return new Location(world, x, y, z, yawF, pitchF);
    }

    public NexusRedisPayload putStringList(String key, List<String> list) {
        Objects.requireNonNull(key, "key");
        if (list == null) {
            values.put(key, null);
        } else {
            values.put(key, List.copyOf(list));
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object v = values.get(key);
        if (v == null) return null;
        if (v instanceof List<?> raw) {
            for (Object o : raw) {
                if (o != null && !(o instanceof String)) {
                    return null;
                }
            }
            return Collections.unmodifiableList((List<String>) raw);
        }
        return null;
    }

    public List<String> getStringList(String key, List<String> defaultValue) {
        List<String> v = getStringList(key);
        return v != null ? v : defaultValue;
    }

    @Override
    public String toString() {
        return "NexusRedisPayload{" +
                "values=" + values +
                '}';
    }
}