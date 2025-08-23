package io.nexstudios.nexus.bukkit.platform;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Einfache Service-Registry, die Implementierungen (oder Supplier) an Interfaces bindet.
 */
public final class VersionedServiceRegistry {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<?>> suppliers = new ConcurrentHashMap<>();

    public <T> void registerInstance(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        services.put(type, instance);
    }

    public <T> void registerSupplier(Class<T> type, Supplier<T> supplier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(supplier, "supplier");
        suppliers.put(type, supplier);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object existing = services.get(type);
        if (existing != null) return (T) existing;

        Supplier<?> sup = suppliers.get(type);
        if (sup != null) {
            T created = (T) sup.get();
            if (created != null) {
                // Cachen, damit spätere Zugriffe schneller sind (optional)
                services.put(type, created);
            }
            return created;
        }

        return null;
    }

    public boolean has(Class<?> type) {
        return services.containsKey(type) || suppliers.containsKey(type);
    }
}