// Java
package io.nexstudios.nexus.bukkit.placeholder;

import java.util.Set;

/**
 * Optional introspection interface for providers that want to expose
 * the set of supported placeholder keys for counting/statistics purposes.
 *
 * Notes:
 * - Keys must be lower-case and without the namespace prefix.
 * - For dynamic providers, return the currently known/static keys (or an empty set).
 * - This interface is optional; providers that do not implement it will simply not contribute to counts.
 */
public interface PlaceholderIntrospector {
    Set<String> advertisedKeys();
}