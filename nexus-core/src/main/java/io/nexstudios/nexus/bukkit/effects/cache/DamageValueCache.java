package io.nexstudios.nexus.bukkit.effects.cache;

import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariables;
import io.nexstudios.nexus.bukkit.utils.NexusStringMath;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageValueCache {

    private DamageValueCache() {}

    private static final Map<Key, Entry> CACHE = new ConcurrentHashMap<>();

    private record Key(UUID playerId, String expr, String context, long version) {
        Key(UUID playerId, String expr, String context, long version) {
            this.playerId = playerId;
            this.expr = expr;
            this.context = context == null ? "" : context;
            this.version = version;
        }
    }

    private record Entry(double value) {}

    // Bestehender Overload bleibt bestehen (backward compatible)
    public static double getOrCompute(Player player, String expression, PlayerVariableResolver resolver) {
        return getOrCompute(player, expression, resolver, null);
    }

    // kontextsensitiver Cache (z. B. pro Stat "stat:<statId>")
    public static double getOrCompute(Player player, String expression, PlayerVariableResolver resolver, String contextKey) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(expression, "expression");

        long version = PlayerVariables.version(player.getUniqueId());
        Key key = new Key(player.getUniqueId(), expression, contextKey, version);

        Entry cached = CACHE.get(key);
        if (cached != null) return cached.value();

        // Variablen auflösen und Ausdruck rechnen
        Map<String, String> vars = resolver != null ? resolver.resolve(player) : Map.of();
        String exprResolved = expression;
        for (var e : vars.entrySet()) {
            // Ersetzt #key# durch den Wert.
            exprResolved = exprResolved.replace("#" + e.getKey() + "#", e.getValue());
        }

        double result = 0.0;
        try {
            result = NexusStringMath.evaluateExpression(exprResolved);
        } catch (Exception ignored) { }

        CACHE.put(key, new Entry(result));
        return result;
    }

    public static void clearAll() {
        CACHE.clear();
    }
}