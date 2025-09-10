package io.nexstudios.nexus.bukkit.placeholder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Placeholder resolver facade.
 *
 * Supports:
 * - Tokens in the form "#<namespace>:<key>#"
 * - Multiple ":" inside key are allowed.
 * - Escaping: "##" becomes a literal "#".
 * - Recursive resolution up to MAX_DEPTH (default 3).
 * - String and Component variants.
 *
 * Behavior for unknown placeholders:
 * - If provider has a fallback for the key -> use fallback.
 * - Otherwise keep original token unchanged.
 *
 * Notes:
 * - All methods are thread-safe and side-effect free.
 * - All comments are in English.
 */
public final class NexusPlaceholders {

    private NexusPlaceholders() {}

    private static final int MAX_DEPTH = 3;

    // Regex to find tokens: "#" then any non-# chars (lazily) then "#"
    private static final Pattern TOKEN_PATTERN = Pattern.compile("#([^#]+?)#");
    // "##" escaping gets mapped to a sentinel char first
    private static final Pattern ESCAPED_HASH = Pattern.compile("##");
    private static final String HASH_SENTINEL = "\u0007"; // bell char, unlikely to occur

    public static String resolve(String text) {
        return resolveInternal(text, null, 0);
    }

    public static String resolveWithPlayer(String text, Player player) {
        return resolveInternal(text, player, 0);
    }

    public static Component resolve(Component component) {
        return resolveInternal(component, null, 0);
    }

    public static Component resolveWithPlayer(Component component, Player player) {
        return resolveInternal(component, player, 0);
    }

    public static Optional<String> resolveSingle(String namespace, String key, @Nullable Player player) {
        if (namespace == null || key == null) return Optional.empty();
        String ns = namespace.toLowerCase(Locale.ROOT).trim();
        String k = key.toLowerCase(Locale.ROOT).trim();

        var regOpt = NexusPlaceholderRegistry.getRegistration(ns);
        if (regOpt.isEmpty()) return Optional.empty();
        var reg = regOpt.get();

        long now = System.currentTimeMillis();
        var cachedVal = NexusPlaceholderRegistry.getCached(ns, k, player == null ? null : player.getUniqueId(), now);
        if (cachedVal != null) {
            if (cachedVal.stringValue() != null) return Optional.of(cachedVal.stringValue());
            if (cachedVal.componentValue() != null) {
                return Optional.of(PlainTextComponentSerializer.plainText().serialize(cachedVal.componentValue()));
            }
        }

        PlaceholderValue val = player == null ? reg.provider().resolve(k) : reg.provider().resolve(player, k);
        if (val == null) {
            String fb = reg.provider().fallback(player, k);
            if (fb != null) return Optional.of(fb);
            return Optional.empty();
        }

        // Determine TTL and cacheability
        boolean cacheable = val.cacheable() && !reg.cachePolicy().isNonCacheable(k) && reg.provider().isCacheable(k);
        long ttl = Optional.ofNullable(val.ttlMillisOverride())
                .or(() -> Optional.ofNullable(reg.provider().ttlMillis(k)))
                .or(() -> Optional.ofNullable(reg.cachePolicy().perKeyTtlMillis(k)))
                .orElse(reg.cachePolicy().defaultTtlMillis());

        if (cacheable && ttl > 0) {
            NexusPlaceholderRegistry.putCached(ns, k, player == null ? null : player.getUniqueId(),
                    val.stringValue(), val.componentValue(), ttl, now);
        }

        if (val.stringValue() != null) return Optional.of(val.stringValue());
        if (val.componentValue() != null) {
            return Optional.of(PlainTextComponentSerializer.plainText().serialize(val.componentValue()));
        }
        return Optional.empty();
    }

    // ---- String resolver ----

    private static String resolveInternal(String input, @Nullable Player player, int depth) {
        if (input == null || input.isEmpty()) return input;
        if (depth >= MAX_DEPTH) return unescapeHashes(input);

        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            // Escaped '#': "##" -> literal '#'
            if (c == '#' && i + 1 < input.length() && input.charAt(i + 1) == '#') {
                out.append('#');
                i += 2;
                continue;
            }

            if (c == '#') {
                int end = findClosingHash(input, i + 1);
                if (end > i + 1) {
                    String token = input.substring(i + 1, end);
                    String replacement = resolveTokenToString(token, player);
                    if (replacement != null) {
                        out.append(replacement);
                    } else {
                        out.append('#').append(token).append('#');
                    }
                    i = end + 1;
                    continue;
                }
            }

            out.append(c);
            i++;
        }

        String result = out.toString();
        if (containsUnescapedHash(result) && depth + 1 < MAX_DEPTH) {
            // recursive pass if more tokens remain
            return resolveInternal(result, player, depth + 1);
        }
        return result;
    }

    // ---- Component resolver (style-preserving via TextReplacementConfig) ----

    private static Component resolveInternal(Component component, @Nullable Player player, int depth) {
        if (component == null) return Component.empty();
        if (depth >= MAX_DEPTH) return component;

        // 1) Temporarily replace "##" with a sentinel to protect escaped hashes
        Component step1 = component.replaceText(TextReplacementConfig.builder()
                .match(ESCAPED_HASH)
                .replacement(HASH_SENTINEL)
                .build());

        // 2) Replace placeholder tokens "#...#" with resolved Components/Strings
        Component step2 = step1.replaceText(TextReplacementConfig.builder()
                .match(TOKEN_PATTERN)
                .replacement((match, builder) -> {
                    String token = match.group(1); // content without surrounding '#'
                    PlaceholderValue resolved = resolveToken(token, player);
                    if (resolved == null) {
                        // unknown -> keep original literal token
                        return Component.text("#" + token + "#");
                    }
                    if (resolved.componentValue() != null) {
                        return resolved.componentValue();
                    }
                    if (resolved.stringValue() != null) {
                        return Component.text(resolved.stringValue());
                    }
                    return Component.text("#" + token + "#");
                })
                .build());

        // 3) Restore sentinel back to "#" (literal hash)
        Component step3 = step2.replaceText(TextReplacementConfig.builder()
                .match(Pattern.quote(HASH_SENTINEL))
                .replacement("#")
                .build());

        // 4) Optional recursive pass if placeholders remain
        if (depth + 1 < MAX_DEPTH && containsUnescapedHash(PlainTextComponentSerializer.plainText().serialize(step3))) {
            return resolveInternal(step3, player, depth + 1);
        }
        return step3;
    }

    private static boolean containsUnescapedHash(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '#') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '#') {
                    i++; // skip escaped '##'
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private static int findClosingHash(String s, int start) {
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '#') {
                // if this is an escaped '##', skip one and continue
                if (i + 1 < s.length() && s.charAt(i + 1) == '#') {
                    i += 2;
                    continue;
                }
                return i;
            }
            i++;
        }
        return -1;
    }

    private static @Nullable String resolveTokenToString(String token, @Nullable Player player) {
        PlaceholderValue val = resolveToken(token, player);
        if (val == null) return null;
        if (val.stringValue() != null) return val.stringValue();
        if (val.componentValue() != null) {
            return PlainTextComponentSerializer.plainText().serialize(val.componentValue());
        }
        return null;
    }

    private static @Nullable PlaceholderValue resolveToken(String token, @Nullable Player player) {
        if (token == null || token.isEmpty()) return null;
        String t = token.toLowerCase(Locale.ROOT).trim();
        int idx = t.indexOf(':');
        if (idx <= 0) return null; // missing namespace
        String namespace = t.substring(0, idx).trim();
        String key = t.substring(idx + 1).trim();
        if (namespace.isEmpty() || key.isEmpty()) return null;

        var regOpt = NexusPlaceholderRegistry.getRegistration(namespace);
        if (regOpt.isEmpty()) return null;
        var reg = regOpt.get();

        long now = System.currentTimeMillis();
        var cachedVal = NexusPlaceholderRegistry.getCached(namespace, key, player == null ? null : player.getUniqueId(), now);
        if (cachedVal != null) {
            return cachedVal;
        }

        PlaceholderValue val = (player == null) ? reg.provider().resolve(key) : reg.provider().resolve(player, key);
        if (val == null) {
            String fb = reg.provider().fallback(player, key);
            if (fb != null) {
                return PlaceholderValue.ofString(fb).cacheable(true);
            }
            return null;
        }

        // Determine TTL and cacheability
        boolean cacheable = val.cacheable() && !reg.cachePolicy().isNonCacheable(key) && reg.provider().isCacheable(key);
        long ttl = Optional.ofNullable(val.ttlMillisOverride())
                .or(() -> Optional.ofNullable(reg.provider().ttlMillis(key)))
                .or(() -> Optional.ofNullable(reg.cachePolicy().perKeyTtlMillis(key)))
                .orElse(reg.cachePolicy().defaultTtlMillis());

        if (cacheable && ttl > 0) {
            NexusPlaceholderRegistry.putCached(namespace, key, player == null ? null : player.getUniqueId(),
                    val.stringValue(), val.componentValue(), ttl, now);
        }

        return val;
    }

    private static String unescapeHashes(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '#' && i + 1 < s.length() && s.charAt(i + 1) == '#') {
                out.append('#');
                i += 2;
            } else {
                out.append(s.charAt(i));
                i++;
            }
        }
        return out.toString();
    }
}