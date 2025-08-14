package io.nexstudios.nexus.bukkit.effects.cache;

import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariables;
import io.nexstudios.nexus.bukkit.utils.NexusStringMath;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageValueCache {

    private static final class Entry {
        final long version;
        final double value;
        Entry(long version, double value) { this.version = version; this.value = value; }
    }

    // Cache-Key: pro Spieler pro Expression-String
    private static final Map<UUID, Map<String, Entry>> CACHE = new ConcurrentHashMap<>();

    private DamageValueCache() {}

    public static double getOrCompute(Player player, String expression, PlayerVariableResolver resolver) {
        UUID pid = player.getUniqueId();
        long ver = PlayerVariables.version(pid);

        Map<String, Entry> perExpr = CACHE.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
        Entry e = perExpr.get(expression);
        if (e != null && e.version == ver) {
            return e.value;
        }

        // Neu berechnen
        String prepared = prepare(expression, resolver, player);
        double val = NexusStringMath.evaluateExpression(prepared);
        perExpr.put(expression, new Entry(ver, val));
        return val;
    }

    private static String prepare(String expr, PlayerVariableResolver resolver, Player player) {
        Map<String, String> vars = resolver.resolve(player);
        String out = expr;
        for (Map.Entry<String, String> en : vars.entrySet()) {
            String plain = toPlain(en.getValue());
            out = out.replace("#" + en.getKey() + "#", plain);
        }
        // sinnvolle Defaults
        out = out.replace("#stat-level#", "0");
        return out;
    }

    private static String toPlain(String in) {
        if (in == null) return "0";
        try { return new BigDecimal(in).toPlainString(); }
        catch (NumberFormatException ex) { return in; }
    }

    public static void clearAll() {
        CACHE.clear();
    }

}
