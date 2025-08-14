package io.nexstudios.nexus.bukkit.effects.load;

import java.util.List;
import java.util.Map;

public final class EffectConfig {
    private final Map<String, Object> raw;
    public EffectConfig(Map<String, Object> raw) { this.raw = raw; }
    public Map<String, Object> raw() { return raw; }
    public String getString(String key, String def) {
        Object o = raw.get(key); return o != null ? String.valueOf(o) : def;
    }
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object o = raw.get(key); return (o instanceof List<?> l) ? (List<String>) l : List.of();
    }
    public double getDouble(String key, double def) {
        Object o = raw.get(key);
        if (o instanceof Number n) return n.doubleValue();
        try { return o != null ? Double.parseDouble(String.valueOf(o)) : def; }
        catch (Exception e) { return def; }
    }
}

