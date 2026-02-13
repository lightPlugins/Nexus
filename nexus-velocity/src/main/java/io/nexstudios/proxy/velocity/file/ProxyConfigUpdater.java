/*
 * This file is part of Nexus, licensed under the MIT License.
 *
 *  Copyright (c) light (NexStudios)
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package io.nexstudios.proxy.velocity.file;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Comment-safe config updater: does NOT re-emit YAML.
 * <p>
 * Policy:
 * - Add missing top-level blocks by copying them from defaults (including comments).
 * - Add missing scalar keys (String/Number/Boolean/null) inside existing sections.
 * - Never add / restore map or list entries (prevents re-adding user-deleted complex config parts).
 * <p>
 * Additionally:
 * - When inserting a missing scalar key, also copy contiguous leading comment lines for that key
 *   from the defaults file (if present).
 */
public final class ProxyConfigUpdater {

    private ProxyConfigUpdater() {}

    public static int updateKeepingComments(Path targetFile,
                                            ClassLoader resourceLoader,
                                            String defaultsResourceName) {
        int blocks = appendMissingTopLevelBlocksWithComments(targetFile, resourceLoader, defaultsResourceName);
        int scalars = appendMissingScalarKeys(targetFile, resourceLoader, defaultsResourceName);
        return blocks + scalars;
    }

    public static int appendMissingTopLevelBlocksWithComments(Path targetFile,
                                                              ClassLoader resourceLoader,
                                                              String defaultsResourceName) {
        Objects.requireNonNull(targetFile, "targetFile");
        Objects.requireNonNull(resourceLoader, "resourceLoader");
        Objects.requireNonNull(defaultsResourceName, "defaultsResourceName");

        String defaultsText = readResourceUtf8(resourceLoader, defaultsResourceName);
        if (defaultsText == null) {
            throw new IllegalStateException("Defaults resource not found: " + defaultsResourceName);
        }

        ConfigurationNode current = loadYaml(targetFile);
        ConfigurationNode defaults = loadYamlFromString(defaultsText);

        Map<Object, ? extends ConfigurationNode> defChildren = defaults.childrenMap();
        if (defChildren.isEmpty()) return 0;

        List<String> blocksToAppend = new ArrayList<>();

        for (Object k : defChildren.keySet()) {
            String key = String.valueOf(k);
            if (key.isBlank()) continue;

            if (!current.node(key).virtual()) continue; // already exists

            String block = extractTopLevelBlockWithLeadingComments(defaultsText, key);
            if (block != null && !block.isBlank()) {
                blocksToAppend.add(block);
            }
        }

        if (blocksToAppend.isEmpty()) return 0;

        try {
            String existing = Files.exists(targetFile)
                    ? Files.readString(targetFile, StandardCharsets.UTF_8)
                    : "";

            StringBuilder out = new StringBuilder(existing);

            // Ensure exactly one newline at EOF before appending.
            while (!out.isEmpty() && out.charAt(out.length() - 1) == '\n') {
                out.setLength(out.length() - 1);
            }
            if (!out.isEmpty()) out.append('\n');

            for (String block : blocksToAppend) {
                String cleaned = trimTrailingBlankLines(block);
                if (cleaned.isBlank()) continue;

                // Separate blocks by exactly one blank line.
                out.append('\n');
                out.append(cleaned);
                if (!cleaned.endsWith("\n")) out.append('\n');
            }

            Files.writeString(targetFile, out.toString(), StandardCharsets.UTF_8);
            return blocksToAppend.size();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to patch config file: " + targetFile, ex);
        }
    }

    /**
     * Appends missing scalar keys inside existing sections.
     * Does NOT add missing sections (those are handled by appendMissingTopLevelBlocksWithComments).
     * <p>
     * Also copies leading comments for newly inserted scalars from defaults.
     */
    public static int appendMissingScalarKeys(Path targetFile,
                                              ClassLoader resourceLoader,
                                              String defaultsResourceName) {
        Objects.requireNonNull(targetFile, "targetFile");
        Objects.requireNonNull(resourceLoader, "resourceLoader");
        Objects.requireNonNull(defaultsResourceName, "defaultsResourceName");

        String defaultsText = readResourceUtf8(resourceLoader, defaultsResourceName);
        if (defaultsText == null) {
            throw new IllegalStateException("Defaults resource not found: " + defaultsResourceName);
        }

        String existingText;
        try {
            existingText = Files.exists(targetFile)
                    ? Files.readString(targetFile, StandardCharsets.UTF_8)
                    : "";
        } catch (IOException e) {
            throw new RuntimeException("Could not read config file: " + targetFile, e);
        }

        ConfigurationNode current = loadYaml(targetFile);
        ConfigurationNode defaults = loadYamlFromString(defaultsText);

        List<ScalarInsert> inserts = new ArrayList<>();
        collectMissingScalars("", defaults, current, defaultsText, inserts);

        // Only insert where the parent section exists in the current file (unless top-level scalar)
        inserts.removeIf(ins -> {
            if (ins.parentPath().isBlank()) return false;
            return currentNode(current, ins.parentPath()).virtual();
        });

        if (inserts.isEmpty()) return 0;

        // Stable ordering
        inserts.sort(Comparator
                .comparingInt((ScalarInsert s) -> s.path().split("\\.").length)
                .thenComparing(ScalarInsert::path));

        String patched = existingText;
        int applied = 0;

        for (ScalarInsert ins : inserts) {
            String next = insertScalarIntoYamlText(patched, ins);
            if (!next.equals(patched)) {
                patched = next;
                applied++;
            }
        }

        if (applied > 0) {
            try {
                Files.writeString(targetFile, patched, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write patched config: " + targetFile, e);
            }
        }

        return applied;
    }

    private record ScalarInsert(String path,
                                String parentPath,
                                String key,
                                Object value,
                                String leadingCommentBlock) {}

    private static void collectMissingScalars(String prefix,
                                              ConfigurationNode defaults,
                                              ConfigurationNode current,
                                              String defaultsText,
                                              List<ScalarInsert> out) {
        if (defaults == null || defaults.virtual()) return;

        Map<Object, ? extends ConfigurationNode> children = defaults.childrenMap();
        if (children.isEmpty()) return;

        for (Map.Entry<Object, ? extends ConfigurationNode> e : children.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (key.isBlank()) continue;

            String path = prefix.isEmpty() ? key : (prefix + "." + key);
            ConfigurationNode defNode = e.getValue();
            ConfigurationNode curNode = current.node(key);

            Object defRaw = defNode.raw();

            // Never restore/patch complex values as scalars.
            if (defRaw instanceof Map<?, ?> || defRaw instanceof List<?>) {
                continue;
            }

            if (curNode.virtual()) {

                int keyIndent = prefix.isBlank()
                        ? 0
                        : estimateIndentInDefaults(defaultsText, prefix) + 2;

                String commentBlock = extractLeadingCommentBlockForKey(defaultsText, prefix, key, keyIndent);

                out.add(new ScalarInsert(path, prefix, key, defRaw, commentBlock));
            }
        }

        // Safe recursion only into maps that already exist in current (policy).
        for (Map.Entry<Object, ? extends ConfigurationNode> e : children.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (key.isBlank()) continue;

            ConfigurationNode defNode = e.getValue();
            Object defRaw = defNode.raw();
            if (!(defRaw instanceof Map<?, ?>)) continue;

            ConfigurationNode curNode = current.node(key);
            if (curNode.virtual()) continue;

            String nextPrefix = prefix.isEmpty() ? key : (prefix + "." + key);
            collectMissingScalars(nextPrefix, defNode, curNode, defaultsText, out);
        }
    }

    private static String insertScalarIntoYamlText(String yaml, ScalarInsert ins) {
        List<String> lines = new ArrayList<>(Arrays.asList(yaml.split("\n", -1)));
        String renderedValue = renderScalar(ins.value());

        // Top-level scalar: append at end.
        if (ins.parentPath().isBlank()) {
            if (!yaml.endsWith("\n") && !yaml.isEmpty()) lines.add("");

            if (ins.leadingCommentBlock != null && !ins.leadingCommentBlock.isBlank()) {
                for (String c : ins.leadingCommentBlock.split("\n", -1)) {
                    if (!c.isBlank()) lines.add(c);
                }
            }

            lines.add(ins.key() + ": " + renderedValue);
            return String.join("\n", lines);
        }

        SectionPos sec = findSection(lines, ins.parentPath());
        if (sec == null) return yaml;

        String keyIndentPrefix = " ".repeat(sec.childIndent);
        String insertLine = keyIndentPrefix + ins.key() + ": " + renderedValue;

        // Prevent duplicates if text already contains it.
        for (int i = sec.startLine + 1; i < sec.endLineExclusive; i++) {
            String line = lines.get(i);
            if (line.startsWith(keyIndentPrefix + ins.key() + ":")) {
                return yaml;
            }
        }

        int insertAt = sec.endLineExclusive;

        // Insert comments directly above the new key (same indentation).
        if (ins.leadingCommentBlock != null && !ins.leadingCommentBlock.isBlank()) {
            for (String c : ins.leadingCommentBlock.split("\n", -1)) {
                if (c.isBlank()) continue;
                lines.add(insertAt, c);
                insertAt++;
            }
        }

        lines.add(insertAt, insertLine);
        return String.join("\n", lines);
    }

    private record SectionPos(int startLine, int endLineExclusive, int headerIndent, int childIndent) {}

    /**
     * Finds a YAML section by a dotted path, assuming block style indentation (2 spaces).
     */
    private static SectionPos findSection(List<String> lines, String dottedPath) {
        String[] parts = dottedPath.split("\\.");
        int searchFrom = 0;
        int currentHeaderLine = -1;
        int currentIndent = 0;

        for (String part : parts) {
            int foundLine = -1;
            int foundIndent = -1;

            for (int i = searchFrom; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int indent = countLeadingSpaces(line);

                // Must be within current section (if we already matched a parent).
                if (currentHeaderLine != -1 && indent <= currentIndent) {
                    break;
                }

                if (trimmed.equals(part + ":") || trimmed.startsWith(part + ": ")) {
                    foundLine = i;
                    foundIndent = indent;
                    break;
                }
            }

            if (foundLine == -1) return null;

            currentHeaderLine = foundLine;
            currentIndent = foundIndent;
            searchFrom = foundLine + 1;
        }

        int end = currentHeaderLine + 1;
        for (; end < lines.size(); end++) {
            String line = lines.get(end);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            int indent = countLeadingSpaces(line);
            if (indent <= currentIndent) {
                break;
            }
        }

        return new SectionPos(currentHeaderLine, end, currentIndent, currentIndent + 2);
    }

    private static int estimateIndentInDefaults(String defaultsText, String parentPath) {
        if (parentPath == null || parentPath.isBlank()) return 0;

        List<String> lines = Arrays.asList(defaultsText.split("\n", -1));
        SectionPos pos = findSection(lines, parentPath);
        return pos == null ? 0 : pos.headerIndent;
    }

    /**
     * Extracts contiguous comment lines directly above the key in defaults.
     * Only includes comment lines with the same indentation as the key.
     */
    private static String extractLeadingCommentBlockForKey(String defaultsText,
                                                           String parentPath,
                                                           String key,
                                                           int keyIndent) {
        List<String> lines = Arrays.asList(defaultsText.split("\n", -1));

        int startSearch = 0;
        int endSearch = lines.size();

        if (parentPath != null && !parentPath.isBlank()) {
            SectionPos sec = findSection(lines, parentPath);
            if (sec == null) return "";
            startSearch = sec.startLine + 1;
            endSearch = sec.endLineExclusive;
        }

        String indentPrefix = " ".repeat(Math.max(0, keyIndent));
        String keyPrefix = indentPrefix + key + ":";

        int keyLine = -1;
        for (int i = startSearch; i < endSearch; i++) {
            String line = lines.get(i);
            if (line.startsWith(keyPrefix)) {
                keyLine = i;
                break;
            }
        }
        if (keyLine == -1) return "";

        int from = keyLine - 1;
        List<String> collected = new ArrayList<>();

        while (from >= startSearch) {
            String line = lines.get(from);
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                from--;
                continue;
            }

            if (line.startsWith(indentPrefix + "#")) {
                collected.add(line);
                from--;
                continue;
            }

            break;
        }

        if (collected.isEmpty()) return "";

        Collections.reverse(collected);
        return String.join("\n", collected);
    }

    private static ConfigurationNode currentNode(ConfigurationNode root, String path) {
        ConfigurationNode n = root;
        for (String p : path.split("\\.")) {
            if (p.isEmpty()) continue;
            n = n.node(p);
        }
        return n;
    }

    private static int countLeadingSpaces(String s) {
        int c = 0;
        while (c < s.length() && s.charAt(c) == ' ') c++;
        return c;
    }

    private static ConfigurationNode loadYaml(Path path) {
        try {
            return YamlConfigurationLoader.builder()
                    .path(path)
                    .build()
                    .load();
        } catch (IOException e) {
            throw new RuntimeException("Could not load yaml: " + path, e);
        }
    }

    private static ConfigurationNode loadYamlFromString(String yaml) {
        try {
            Path tmp = Files.createTempFile("nexus-defaults-", ".yml");
            Files.writeString(tmp, yaml, StandardCharsets.UTF_8);

            return YamlConfigurationLoader.builder()
                    .path(tmp)
                    .build()
                    .load();
        } catch (IOException e) {
            throw new RuntimeException("Could not load yaml from string", e);
        }
    }

    private static String readResourceUtf8(ClassLoader cl, String name) {
        try (InputStream in = cl.getResourceAsStream(name)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource: " + name, e);
        }
    }

    private static String extractTopLevelBlockWithLeadingComments(String defaultsText, String topLevelKey) {
        List<String> lines = Arrays.asList(defaultsText.split("\n", -1));

        int keyLine = -1;
        String needle = topLevelKey + ":";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(needle)) {
                keyLine = i;
                break;
            }
        }
        if (keyLine == -1) return null;

        int start = keyLine;
        while (start - 1 >= 0) {
            String prev = lines.get(start - 1);
            String trimmed = prev.trim();
            boolean isBlank = trimmed.isEmpty();
            boolean isComment = trimmed.startsWith("#");
            if (isBlank || isComment) {
                start--;
                continue;
            }
            break;
        }

        int end = keyLine + 1;
        while (end < lines.size()) {
            String line = lines.get(end);
            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0))) {
                break;
            }
            end++;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(lines.get(i)).append('\n');
        }
        return sb.toString();
    }

    private static String trimTrailingBlankLines(String s) {
        int i = s.length();
        while (i > 0) {
            int lineStart = s.lastIndexOf('\n', i - 1) + 1;
            String line = s.substring(lineStart, i).replace("\r", "");
            if (!line.trim().isEmpty()) break;
            i = lineStart - 1;
        }
        return s.substring(0, Math.max(0, i + 1));
    }

    private static String renderScalar(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);

        String s = String.valueOf(v);
        if (s.isEmpty()) return "\"\"";

        boolean needsQuotes =
                s.startsWith(" ") || s.endsWith(" ")
                        || s.contains(":") || s.contains("#")
                        || s.contains("\"") || s.contains("'")
                        || s.contains("\n") || s.contains("\r")
                        || s.equalsIgnoreCase("null")
                        || s.equalsIgnoreCase("true")
                        || s.equalsIgnoreCase("false");

        if (!needsQuotes) return s;

        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}