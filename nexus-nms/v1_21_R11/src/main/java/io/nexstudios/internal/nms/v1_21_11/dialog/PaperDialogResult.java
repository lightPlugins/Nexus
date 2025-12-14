package io.nexstudios.internal.nms.v1_21_11.dialog;

import io.nexstudios.nexus.bukkit.dialog.NexDialogResult;
import io.papermc.paper.dialog.DialogResponseView;

import java.util.Set;

/**
 * NMS/Paper adapter that wraps Paper's DialogResponseView
 * and exposes it as a NexDialogResult for the core.
 */
public final class PaperDialogResult implements NexDialogResult {

    private final DialogResponseView delegate;

    public PaperDialogResult(DialogResponseView delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getString(String key) {
        if (delegate == null || key == null) return null;
        // For text inputs Paper stores the value as text
        return delegate.getText(key);
    }

    @Override
    public int getInt(String key, int def) {
        if (delegate == null || key == null) return def;

        // 1) Try native float getter (for numberRange / slider)
        Float f = delegate.getFloat(key);
        if (f != null) {
            return Math.round(f);
        }

        // 2) Fallback: parse text representation if present
        String s = getString(key);
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    @Override
    public double getDouble(String key, double def) {
        if (delegate == null || key == null) return def;

        // 1) Try native float getter
        Float f = delegate.getFloat(key);
        if (f != null) {
            return f;
        }

        // 2) Fallback: parse text
        String s = getString(key);
        if (s == null) return def;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        if (delegate == null || key == null) return def;

        // 1) Try native boolean getter (for bool inputs)
        Boolean b = delegate.getBoolean(key);
        if (b != null) {
            return b;
        }

        // 2) Fallback: parse text
        String s = getString(key);
        if (s == null) return def;
        String v = s.trim().toLowerCase();
        if (v.equals("true") || v.equals("yes") || v.equals("y") || v.equals("1")) return true;
        if (v.equals("false") || v.equals("no") || v.equals("n") || v.equals("0")) return false;
        return def;
    }

    @Override
    public Set<String> keys() {
        // DialogResponseView does not expose keys, so we just return an empty set.
        return Set.of();
    }

    public DialogResponseView delegate() {
        return delegate;
    }
}