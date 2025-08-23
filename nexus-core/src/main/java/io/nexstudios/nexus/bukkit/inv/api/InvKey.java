// Java
package io.nexstudios.nexus.bukkit.inv.api;

import java.util.Objects;

public final class InvKey {
    private final String namespace; // z. B. "meinplugin"
    private final String key;       // Dateiname ohne .yml, z. B. "test-inv"

    public InvKey(String namespace, String key) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.key = Objects.requireNonNull(key, "key");
    }

    public String namespace() { return namespace; }
    public String key() { return key; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvKey k)) return false;
        return namespace.equalsIgnoreCase(k.namespace) && key.equalsIgnoreCase(k.key);
    }
    @Override public int hashCode() {
        return (namespace.toLowerCase() + ":" + key.toLowerCase()).hashCode();
    }
    @Override public String toString() {
        return namespace + ":" + key;
    }
}