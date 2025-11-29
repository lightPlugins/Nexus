package io.nexstudios.nexus.bukkit.indicator;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.hologram.NexHologram;
import io.nexstudios.nexus.bukkit.hologram.HoloBuilder;
import io.nexstudios.nexus.bukkit.hooks.EcoSkillsHook;
import io.nexstudios.nexus.bukkit.platform.NexServices;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DamageIndicator implements Listener {

    private final NexusPlugin plugin;
    private final EcoSkillsHook ecoSkillsHook;

    private boolean enabled;
    private List<Map<String, Object>> conditions;
    private int fractionDigits;
    private int lifeTimeTicks;
    private String normalFormat;
    private String critFormat;
    private Vector offset;
    private VelocityRange velocityRange;
    private final Map<Character, String> normalNumbers = new HashMap<>();
    private final Map<Character, String> critNumbers = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private boolean papiAvailable;

    // Trackt alle von diesem Indicator erzeugten ArmorStands
    private final Set<ArmorStand> activeStands = new HashSet<>();

    public DamageIndicator(NexusPlugin plugin, EcoSkillsHook ecoSkillsHook) {
        this.plugin = plugin;
        this.ecoSkillsHook = ecoSkillsHook;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        ConfigurationSection root = plugin.getSettingsFile().getConfigurationSection("damage-indicator");
        if (root == null) {
            this.enabled = false;
            this.fractionDigits = 2;
            this.lifeTimeTicks = 10;
            this.normalFormat = "<red>#damage#";
            this.critFormat = "<rainbow>#damage#";
            this.offset = new Vector(0, 1.5, 0);
            this.velocityRange = VelocityRange.defaultRange();
            this.papiAvailable = plugin.getPapiHook() != null;
            return;
        }

        this.enabled = root.getBoolean("enable", true);

        List<Map<String, Object>> enableConditions = new ArrayList<>();

        this.conditions = (List<Map<String, Object>>) (List<?>) root.getMapList("enable-conditions");

        this.lifeTimeTicks = root.getInt("life-time-ticks", 10);
        this.fractionDigits = root.getInt("fraction-digits", 2);

        ConfigurationSection fmtSection = root.getConfigurationSection("format");
        this.normalFormat = fmtSection != null ? fmtSection.getString("normal", "<red>#damage#") : "<red>#damage#";
        this.critFormat = fmtSection != null ? fmtSection.getString("crit", "<rainbow>#damage#") : "<rainbow>#damage#";

        ConfigurationSection offSec = root.getConfigurationSection("offset");
        double ox = offSec != null ? offSec.getDouble("x", 0.0) : 0.0;
        double oy = offSec != null ? offSec.getDouble("y", 1.5) : 1.5;
        double oz = offSec != null ? offSec.getDouble("z", 0.0) : 0.0;
        this.offset = new Vector(ox, oy, oz);

        this.velocityRange = VelocityRange.fromConfig(root.getConfigurationSection("velocity"));

        // Zahlen-Mapping laden
        ConfigurationSection numbersSec = root.getConfigurationSection("numbers");
        if (numbersSec != null) {
            ConfigurationSection normalSec = numbersSec.getConfigurationSection("normal");
            ConfigurationSection critSec = numbersSec.getConfigurationSection("crit");
            for (char c = '0'; c <= '9'; c++) {
                String key = String.valueOf(c);
                if (normalSec != null) {
                    normalNumbers.put(c, normalSec.getString(key, key));
                } else {
                    normalNumbers.put(c, key);
                }
                if (critSec != null) {
                    critNumbers.put(c, critSec.getString(key, key));
                } else {
                    critNumbers.put(c, key);
                }
            }
        } else {
            for (char c = '0'; c <= '9'; c++) {
                normalNumbers.put(c, String.valueOf(c));
                critNumbers.put(c, String.valueOf(c));
            }
        }

        this.papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void reloadFromConfig() {
        // vorhandene Mappings leeren
        normalNumbers.clear();
        critNumbers.clear();

        ConfigurationSection root = plugin.getSettingsFile().getConfigurationSection("damage-indicator");
        if (root == null) {
            this.enabled = false;
            this.conditions = List.of();
            this.fractionDigits = 2;
            this.lifeTimeTicks = 10;
            this.normalFormat = "<red>#damage#";
            this.critFormat = "<rainbow>#damage#";
            this.offset = new Vector(0, 1.5, 0);
            this.velocityRange = VelocityRange.defaultRange();
            this.papiAvailable = plugin.getPapiHook() != null;
            return;
        }

        this.enabled = root.getBoolean("enable", true);
        this.conditions = (List<Map<String, Object>>) (List<?>) root.getMapList("enable-conditions");
        this.lifeTimeTicks = root.getInt("life-time-ticks", 10);
        this.fractionDigits = root.getInt("fraction-digits", 2);

        ConfigurationSection fmtSection = root.getConfigurationSection("format");
        this.normalFormat = fmtSection != null ? fmtSection.getString("normal", "<red>#damage#") : "<red>#damage#";
        this.critFormat = fmtSection != null ? fmtSection.getString("crit", "<rainbow>#damage#") : "<rainbow>#damage#";

        ConfigurationSection offSec = root.getConfigurationSection("offset");
        double ox = offSec != null ? offSec.getDouble("x", 0.0) : 0.0;
        double oy = offSec != null ? offSec.getDouble("y", 1.5) : 1.5;
        double oz = offSec != null ? offSec.getDouble("z", 0.0) : 0.0;
        this.offset = new Vector(ox, oy, oz);

        this.velocityRange = VelocityRange.fromConfig(root.getConfigurationSection("velocity"));

        // Zahlen-Mapping laden
        ConfigurationSection numbersSec = root.getConfigurationSection("numbers");
        if (numbersSec != null) {
            ConfigurationSection normalSec = numbersSec.getConfigurationSection("normal");
            ConfigurationSection critSec = numbersSec.getConfigurationSection("crit");
            for (char c = '0'; c <= '9'; c++) {
                String key = String.valueOf(c);
                if (normalSec != null) {
                    normalNumbers.put(c, normalSec.getString(key, key));
                } else {
                    normalNumbers.put(c, key);
                }
                if (critSec != null) {
                    critNumbers.put(c, critSec.getString(key, key));
                } else {
                    critNumbers.put(c, key);
                }
            }
        } else {
            for (char c = '0'; c <= '9'; c++) {
                normalNumbers.put(c, String.valueOf(c));
                critNumbers.put(c, String.valueOf(c));
            }
        }

        this.papiAvailable = NexusPlugin.getInstance().getPapiHook() != null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        Player player = event.getDamager() instanceof Player ? (Player) event.getDamager() : null;

        if (!enabled) return;
        if(player != null) {
            if(!NexusPlugin.getInstance().getConditionFactory().checkConditions(player, event.getEntity().getLocation(), conditions)) {
                return;
            }
        }

        Entity target = event.getEntity();
        Location base = target.getLocation().add(offset);

        boolean crit = ecoSkillsHook != null && ecoSkillsHook.isCrit(event);
        double damageValue = event.getFinalDamage();

        String damageString = formatDamage(damageValue);
        String mappedDigits = mapDigits(damageString, crit);

        // PlaceholderAPI auch auf die Zahlen anwenden (für ItemsAdder etc.)
        if (papiAvailable) {
            mappedDigits = PlaceholderAPI.setPlaceholders(player, mappedDigits);
        }

        String formatTemplate = crit ? critFormat : normalFormat;
        String formatted = formatTemplate.replace("#damage#", mappedDigits);

        if (papiAvailable) {
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }

        Component line = miniMessage.deserialize(formatted);

        spawnFlyingHologram(player, base, line);
    }

    private String formatDamage(double damage) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        StringBuilder pattern = new StringBuilder("0");
        if (fractionDigits > 0) {
            pattern.append('.');
            for (int i = 0; i < fractionDigits; i++) {
                pattern.append('0');
            }
        }
        DecimalFormat df = new DecimalFormat(pattern.toString(), symbols);
        df.setMaximumFractionDigits(fractionDigits);
        df.setMinimumFractionDigits(fractionDigits);
        return df.format(damage);
    }

    private String mapDigits(String input, boolean crit) {
        Map<Character, String> map = crit ? critNumbers : normalNumbers;
        StringBuilder out = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                String repl = map.get(c);
                out.append(repl != null ? repl : c);
            } else {
                // z.B. Dezimalpunkt
                out.append(c);
            }
        }
        return out.toString();
    }

    private void spawnFlyingHologram(Player viewer, Location start, Component line) {
        if (start.getWorld() == null) return;

        // Wie weit oberhalb des ArmorStands das Hologramm gerendert wird.
        double passengerYOffset = 1.0;

        Location holoBase = start.clone();
        Location standLocation = start.clone().subtract(0, passengerYOffset, 0);

        // Velocity VOR dem Spawn berechnen

        // 1) Unsichtbaren ArmorStand spawnen und direkt mit Velocity ausstatten
        Vector finalVelocity = velocityRange.randomVector();
        ArmorStand stand = start.getWorld().spawn(standLocation, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(false);
            as.setGravity(true);        // keine Vanilla-Gravitation
            as.setSmall(true);
            as.setBasePlate(false);
            as.setArms(false);
            as.setInvulnerable(true);
            as.setSilent(true);
            as.setCustomNameVisible(false);
            as.setVelocity(finalVelocity);    // WICHTIG: direkt beim Spawn setzen
        });

        synchronized (activeStands) {
            activeStands.add(stand);
        }

        // 2) Packet-Hologram
        HoloBuilder builder = NexServices.newHoloBuilder()
                .location(holoBase)
                .lines(List.of(line))
                .viewerOnly(viewer)
                .billboard("center")
                .lineWidth(200)
                .backgroundColor(0x00000000)
                .useTextDisplay(true)
                .scale(new Vector(2.5, 2.5, 2.5))
                .attachToEntity(stand);

        NexHologram holo = builder.build();


        // 3) Timer zum löschen
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            holo.destroy();
            synchronized (activeStands) {
                activeStands.remove(stand);
            }
            stand.remove();
        }, lifeTimeTicks);
    }

    /**
     * Wird z.B. in onDisable() des Plugins aufgerufen.
     * Entfernt alle noch existierenden ArmorStands sofort.
     */
    public void shutdown() {
        synchronized (activeStands) {
            for (ArmorStand stand : activeStands) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
            activeStands.clear();
        }
    }

    // ----------------- Helpers -----------------

    private static final class VelocityRange {
        final double inXMin, inXMax;
        final double inYMin, inYMax;
        final double inZMin, inZMax;

        private VelocityRange(double inXMin, double inXMax,
                              double inYMin, double inYMax,
                              double inZMin, double inZMax) {
            this.inXMin = inXMin;
            this.inXMax = inXMax;
            this.inYMin = inYMin;
            this.inYMax = inYMax;
            this.inZMin = inZMin;
            this.inZMax = inZMax;
        }

        static VelocityRange defaultRange() {
            return new VelocityRange(0.2, 0.5, 0.5, 0.9, 0.2, 0.5);
        }

        static VelocityRange fromConfig(ConfigurationSection velocitySec) {
            if (velocitySec == null) return defaultRange();

            ConfigurationSection xSec = velocitySec.getConfigurationSection("in-x");
            ConfigurationSection ySec = velocitySec.getConfigurationSection("in-y");
            ConfigurationSection zSec = velocitySec.getConfigurationSection("in-z");

            double xMin = xSec != null ? xSec.getDouble("min", 0.2) : 0.2;
            double xMax = xSec != null ? xSec.getDouble("max", 0.5) : 0.5;
            double yMin = ySec != null ? ySec.getDouble("min", 0.5) : 0.5;
            double yMax = ySec != null ? ySec.getDouble("max", 0.9) : 0.9;
            double zMin = zSec != null ? zSec.getDouble("min", 0.2) : 0.2;
            double zMax = zSec != null ? zSec.getDouble("max", 0.5) : 0.5;

            return new VelocityRange(xMin, xMax, yMin, yMax, zMin, zMax);
        }

        Vector randomVector() {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            double vx = randomBetween(rnd, inXMin, inXMax) * (rnd.nextBoolean() ? 1 : -1);
            double vy = randomBetween(rnd, inYMin, inYMax);
            double vz = randomBetween(rnd, inZMin, inZMax) * (rnd.nextBoolean() ? 1 : -1);
            return new Vector(vx, vy, vz);
        }

        private double randomBetween(ThreadLocalRandom rnd, double min, double max) {
            if (max <= min) return min;
            return min + rnd.nextDouble() * (max - min);
        }
    }
}