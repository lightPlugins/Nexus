// Java
package io.nexstudios.nexus.bukkit.inv.api;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.files.NexusFileReader;
import io.nexstudios.nexus.bukkit.inv.NexInventory;
import io.nexstudios.nexus.bukkit.inv.NexInventoryManager;
import io.nexstudios.nexus.bukkit.inv.NexInventoryView;
import io.nexstudios.nexus.bukkit.inv.NexOnClick;
import io.nexstudios.nexus.bukkit.inv.config.NexInventoryConfig;
import io.nexstudios.nexus.bukkit.inv.config.NexItemConfig;
import io.nexstudios.nexus.bukkit.inv.fill.InvAlignment;
import io.nexstudios.nexus.bukkit.inv.fill.InvFillStrategy;
import io.nexstudios.nexus.bukkit.inv.fill.RowMajorFillStrategy;
import io.nexstudios.nexus.bukkit.inv.renderer.NexItemRenderer;
import io.nexstudios.nexus.bukkit.language.NexusLanguage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class InvService {

    private final JavaPlugin plugin;
    private final NexItemRenderer renderer;
    private final Map<InvKey, InvHandleImpl> registry = new ConcurrentHashMap<>();
    private final Map<String, NexusFileReader> namespaces = new ConcurrentHashMap<>();
    private final NexusLanguage nexusLanguage;

    public InvService(JavaPlugin plugin, NexItemRenderer renderer, NexusLanguage nexusLanguage) {
        this.plugin = plugin;
        this.renderer = renderer;
        this.nexusLanguage = nexusLanguage;
    }

    // Namespace registrieren und initial laden (preload aller inv yml)
    public void registerNamespace(String namespace, NexusFileReader fileReader) {
        namespaces.put(namespace.toLowerCase(Locale.ROOT), fileReader);
        preloadNamespace(namespace);
    }

    // Preload: alle Dateien des Namespace einlesen und Inventare bauen
    public void preloadNamespace(String namespace) {
        NexusFileReader fr = namespaces.get(namespace.toLowerCase(Locale.ROOT));
        if (fr == null) return;
        Map<String, FileConfiguration> map = getBukkitFiles(fr);
        for (Map.Entry<String, FileConfiguration> e : map.entrySet()) {
            String key = e.getKey(); // Dateiname ohne .yml
            FileConfiguration cfg = e.getValue();
            InvKey invKey = new InvKey(namespace, key);
            NexInventoryConfig nic = InvConfigLoader.load(key, cfg);
            NexInventory inv = build(nic);
            registry.put(invKey, new InvHandleImpl(invKey, inv));
        }
    }

    public void reloadWithNamespace(String namespace, NexusFileReader fileReader) {
        if (namespace == null) return;
        String nsLower = namespace.toLowerCase(Locale.ROOT);

        // Falls ein externer Reader übergeben wird, im Namespace registrieren/ersetzen
        if (fileReader != null) {
            namespaces.put(nsLower, fileReader);
            fileReader.reload();
        } else {
            // Fallback: vorhandenen Reader verwenden
            NexusFileReader fr = namespaces.get(nsLower);
            if (fr == null) return;
            fr.reload();
        }

        // Alle Registry-Einträge dieses Namespaces entfernen
        registry.keySet().removeIf(k -> k != null && nsLower.equalsIgnoreCase(k.namespace()));

        // Nur diesen Namespace erneut preloaden (verwendet den Reader aus 'namespaces')
        preloadNamespace(namespace);

        // Optionales Logging
        NexusPlugin.nexusLogger.info("Reloaded inventories with namespace: " + namespace);
    }



    // Reload: Reader neu einlesen; alte Views schließen; Registry neu aufbauen
    public void reload() {
        // Zuerst alle offenen Views schließen
        NexInventoryManager.get().closeAndReopenAll();

        // Dann Namespaces neu laden
        namespaces.values().forEach(NexusFileReader::reload);

        // Registry leeren und für alle Namespaces neu aufbauen
        registry.clear();
        namespaces.keySet().forEach(this::preloadNamespace);
    }


    public Optional<InvHandle> handle(String namespace, String key) {
        InvKey k = new InvKey(namespace, key);
        return Optional.ofNullable(registry.get(k));
    }

    public NexInventoryView open(String namespace, String key, Player player) {
        return handle(namespace, key)
                .map(h -> h.open(player))
                .orElse(null);
    }

    public NexMenuSession menu(String namespace, String key) {
        InvKey k = new InvKey(namespace, key);
        InvHandleImpl h = registry.get(k);
        if (h == null) return NexMenuSession.empty(); // liefert No-Op Session
        return NexMenuSession.forHandle(h);
    }

    // Optional: nach dem Config-Load noch Items hinzufügen (Required/Custom), Body-Items setzen etc.
    public InvHandle augment(String namespace, String key, Consumer<NexInventory> fn) {
        InvKey k = new InvKey(namespace, key);
        InvHandleImpl h = registry.get(k);
        if (h != null) {
            fn.accept(h.inventory());
        }
        return h;
    }

    // Build aus NexInventoryConfig (setzt Navigation/Required/Custom/Deko/Body/Alignment/Strategy)
    private NexInventory build(NexInventoryConfig nic) {
        // Body-Slots vorbereiten: Entweder explizite slots oder grid
        List<Integer> bodySlots0b = new ArrayList<>();
        if (nic.bodySlots1b != null && !nic.bodySlots1b.isEmpty()) {
            for (Integer s1b : nic.bodySlots1b) {
                if (s1b != null && s1b > 0) bodySlots0b.add(s1b - 1);
            }
        }
        InvFillStrategy strategy = new RowMajorFillStrategy();

        Map<String, NexItemConfig> navigation = nic.navigation;
        List<NexItemConfig> required = nic.required;
        List<NexItemConfig> custom = nic.custom;

        return NexInventory.builder()
                .inventoryId(nic.inventoryId)
                .rows(nic.rows)
                .titleProvider(p -> nic.title)
                .updateIntervalTicks(nic.updateIntervalTicks)
                .decoration(nic.decorationEnabled, nic.decorationItemSpec)
                .navigation(navigation)
                .required(required)
                .custom(custom)
                .bodyAlignment(nic.bodyAlignment != null ? nic.bodyAlignment : InvAlignment.LEFT)
                .bodyGrid(nic.bodyRows, nic.bodyCols)
                .bodySlots0b(bodySlots0b)
                .fillStrategy(strategy)
                .itemRenderer(renderer)
                .extraSettings(nic.extraSettings)
                .nexusLanguage(nexusLanguage)
                .build();
    }

    private Map<String, FileConfiguration> getBukkitFiles(NexusFileReader fr) {
        // Verwende die vom Reader gepflegte Map: Dateiname ohne .yml -> FileConfiguration
        Map<String, FileConfiguration> map = fr.getBukkitFileMap();
        return (map != null) ? new HashMap<>(map) : java.util.Collections.emptyMap();
    }


    // Interne Handle-Implementierung
    public final class InvHandleImpl implements InvHandle {
        private final InvKey key;
        private final NexInventory inv;
        private Consumer<NexInventory> augment = (i) -> {};

        InvHandleImpl(InvKey key, NexInventory inv) {
            this.key = key;
            this.inv = inv;
        }
        @Override public InvKey key() { return key; }
        @Override public NexInventoryView open(Player player) { return inv.openFor(player); }
        @Override public InvHandle setBodyItems(List<?> models, NexOnClick clickHandler) { return this; }
        @Override public NexInventory inventory() { return inv; }
        @Override public InvHandle onPostLoad(Consumer<NexInventory> a) { this.augment = a; a.accept(inv); return this; }
    }

}