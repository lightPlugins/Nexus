package io.nexstudios.nexus.bukkit.costs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of resolving a cost.
 *
 * - success: whether the cost has been paid successfully.
 * - params: additional key/value parameters that can be used as placeholders
 *           in messages or actions (e.g. "needed-amount", "current-amount").
 */
public final class CostResult {

    private final boolean success;
    private final Map<String, String> params;

    private CostResult(boolean success, Map<String, String> params) {
        this.success = success;
        this.params = Collections.unmodifiableMap(params);
    }

    public static CostResult success() {
        return new CostResult(true, Map.of());
    }

    public static CostResult success(Map<String, String> params) {
        return new CostResult(true, params == null ? Map.of() : new HashMap<>(params));
    }

    public static CostResult fail() {
        return new CostResult(false, Map.of());
    }

    public static CostResult fail(Map<String, String> params) {
        return new CostResult(false, params == null ? Map.of() : new HashMap<>(params));
    }

    public boolean succeeded() {
        return success;
    }

    public boolean failed() {
        return !success;
    }

    public Map<String, String> params() {
        return params;
    }

    public CostResult with(String key, String value) {
        Map<String, String> copy = new HashMap<>(params);
        copy.put(key, value);
        return new CostResult(success, copy);
    }
}