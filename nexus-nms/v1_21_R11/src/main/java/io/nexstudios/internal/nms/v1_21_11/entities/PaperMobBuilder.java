package io.nexstudios.internal.nms.v1_21_11.entities;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.entities.MobBuilder;
import io.nexstudios.nexus.bukkit.entities.MobBuilderFactory;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple Paper implementation of MobBuilder.
 * Notes:
 * - build() spawns at the main world's spawn if no better context is available here.
 * - Some features (hologram*) are stored but not rendered here (no hologram subsystem in this class).
 * - Attributes are only applied if available on the spawned entity.
 */
public class PaperMobBuilder implements MobBuilder, MobBuilderFactory {

    // Core mob properties
    private EntityType entityType;
    private Component customName;
    private Double scale = 1.0;
    private Double health = 20.0;
    private Double damage = 2.0;
    private Double speed = 0.2;
    private Double armor = 0.0;
    private Double armorToughness = 0.0;
    private Double knockbackResistance = 0.0;
    private Integer noDamageTicks = 20;
    private Boolean aggressive = false;
    private Boolean baby = false;
    private Boolean disableDrops = false;
    private ItemStack main;
    private ItemStack off;
    private ItemStack helm;
    private ItemStack chest;
    private ItemStack legs;
    private ItemStack boots;

    // Hologram preview settings (not rendered here)
    private Boolean hologramEnabled;
    private String holoBillboard;
    private String holoBackgroundColor;
    private Vector holoSize;
    private Integer holoViewRange;
    private Boolean holoSeeThrough;
    private Double holoLineWidth;
    private List<Component> holoLines;

    // ... existing code ...

    @Override
    public MobBuilder entity(EntityType entityType) {
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        return this;
    }

    @Override
    public MobBuilder name(Component name) {
        this.customName = name;
        return this;
    }

    @Override
    public MobBuilder scale(double scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public MobBuilder health(double health) {
        this.health = health;
        return this;
    }

    @Override
    public MobBuilder damage(double damage) {
        this.damage = damage;
        return this;
    }

    @Override
    public MobBuilder speed(double speed) {
        this.speed = speed;
        return this;
    }

    @Override
    public MobBuilder armor(double armor) {
        this.armor = armor;
        return this;
    }

    @Override
    public MobBuilder armorToughness(double armorToughness) {
        this.armorToughness = armorToughness;
        return this;
    }

    @Override
    public MobBuilder knockbackResistance(double knockbackResistance) {
        this.knockbackResistance = knockbackResistance;
        return this;
    }

    @Override
    public MobBuilder noDamageTicks(int noDamageTicks) {
        this.noDamageTicks = noDamageTicks;
        return this;
    }

    @Override
    public MobBuilder aggressive(boolean aggressive) {
        this.aggressive = aggressive;
        return this;
    }

    @Override
    public MobBuilder baby(boolean baby) {
        this.baby = baby;
        return this;
    }

    @Override
    public MobBuilder disableDrops(boolean disableDrops) {
        this.disableDrops = disableDrops;
        return this;
    }

    @Override
    public MobBuilder main(ItemStack main) {
        this.main = main;
        return this;
    }

    @Override
    public MobBuilder off(ItemStack main) {
        this.off = main;
        return this;
    }

    @Override
    public MobBuilder helm(ItemStack main) {
        this.helm = main;
        return this;
    }

    @Override
    public MobBuilder chest(ItemStack main) {
        this.chest = main;
        return this;
    }

    @Override
    public MobBuilder legs(ItemStack main) {
        this.legs = main;
        return this;
    }

    @Override
    public MobBuilder boots(ItemStack main) {
        this.boots = main;
        return this;
    }

    @Override
    public MobBuilder hologramEnabled(boolean hologramEnabled) {
        this.hologramEnabled = hologramEnabled;
        return this;
    }

    @Override
    public MobBuilder holoBillboard(String billboard) {
        this.holoBillboard = billboard;
        return this;
    }

    @Override
    public MobBuilder holoBackgroundColor(String backgroundColor) {
        this.holoBackgroundColor = backgroundColor;
        return this;
    }

    @Override
    public MobBuilder holoSize(Vector size) {
        this.holoSize = size;
        return this;
    }

    @Override
    public MobBuilder holoViewRange(int viewRange) {
        this.holoViewRange = viewRange;
        return this;
    }

    @Override
    public MobBuilder holoSeeThrough(boolean holoSeeThrough) {
        this.holoSeeThrough = holoSeeThrough;
        return this;
    }

    @Override
    public MobBuilder holoLineWidth(double holoLineWidth) {
        this.holoLineWidth = holoLineWidth;
        return this;
    }

    @Override
    public MobBuilder holoLines(List<Component> lines) {
        this.holoLines = lines;
        return this;
    }

    @Override
    public LivingEntity spawn(Location loc, Player player) {

        if (entityType == null) {
            throw new IllegalStateException("EntityType must be set before build().");
        }

        var spawned = loc.getWorld().spawnEntity(loc, entityType);

        if (!(spawned instanceof LivingEntity livingEntity)) {
            // If not a LivingEntity, remove and fail (builder expects LivingEntity).
            spawned.remove();
            throw new IllegalStateException("Spawned entity is not a LivingEntity: " + entityType);
        }

        // Name
        if (customName != null) {
            livingEntity.customName(customName);
            livingEntity.setCustomNameVisible(true);
        }

        // Apply attributes helper
        applyAttr(livingEntity, Attribute.MAX_HEALTH, health);
        applyAttr(livingEntity, Attribute.ATTACK_DAMAGE, damage);
        applyAttr(livingEntity, Attribute.MOVEMENT_SPEED, speed);
        applyAttr(livingEntity, Attribute.ARMOR, armor);
        applyAttr(livingEntity, Attribute.ARMOR_TOUGHNESS, armorToughness);
        applyAttr(livingEntity, Attribute.KNOCKBACK_RESISTANCE, knockbackResistance);
        applyAttr(livingEntity, Attribute.SCALE, scale);

        // No-damage ticks
        if (noDamageTicks != null) {
            livingEntity.setNoDamageTicks(Math.max(0, noDamageTicks));
        }

        // Baby: try Ageable / Zombie specific APIs
        if (livingEntity instanceof Ageable ageable) {
            if(baby) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }

        if (livingEntity instanceof Lootable lootable) {
            if(disableDrops) {
                lootable.setLootTable(null);
            }
        }

        // Aggressive: for Mob types, toggle awareness
        if (aggressive && livingEntity instanceof Mob mob) {
            // setAware controls AI; aggressive=false -> passive/idle
            //mob.setAggressive(true);
            tryMakeHostileWithNmsGoals(mob);
            //mob.setAware(aggressive);

            if (player != null && player.isOnline() && player.getWorld().equals(mob.getWorld())) {
                mob.setTarget(player);
            }
        }

        // Equipment: map list to common slots [mainHand, offHand, helmet, chest, legs, boots]
        EntityEquipment eq = livingEntity.getEquipment();
        if (eq != null) {
            if(main != null) eq.setItemInMainHand(main);
            if(off != null) eq.setItemInOffHand(off);
            if(helm != null) eq.setHelmet(helm);
            if(chest != null) eq.setChestplate(chest);
            if(legs != null) eq.setLeggings(legs);
            if(boots != null) eq.setBoots(boots);

            if(disableDrops) {
                eq.setHelmetDropChance(0.0f);
                eq.setChestplateDropChance(0.0f);
                eq.setLeggingsDropChance(0.0f);
                eq.setBootsDropChance(0.0f);
                eq.setItemInMainHandDropChance(0.0f);
                eq.setItemInOffHandDropChance(0.0f);
            }
        }


        // Hologram preview settings are not rendered here.
        return livingEntity;
    }

    @Override
    public MobBuilder create() {
        return new PaperMobBuilder();
    }

    // Attempt to convert passive mobs to hostile by adjusting NMS goal selectors (guarded).
    // Robust hostile AI: minimal goal set, no leap, no random stroll/look, tuned attributes.
    private static void tryMakeHostileWithNmsGoals(Mob mob) {
        try {
            Object nms = ((CraftLivingEntity) mob).getHandle();
            if (!(nms instanceof net.minecraft.world.entity.Mob nmsMob)) return;

            //try { nmsMob.setPersistenceRequired(true); } catch (Throwable ignored) {}
            //try { mob.setRemoveWhenFarAway(false); } catch (Throwable ignored) {}
            //try { mob.setAware(true); } catch (Throwable ignored) {}

            //clearAllGoals(nmsMob.goalSelector);
            //clearAllGoals(nmsMob.targetSelector);
            nmsMob.goalSelector.removeAllGoals(w -> true);
            nmsMob.targetSelector.removeAllGoals(w -> true);

            try {
                AttributeMap map = nmsMob.getAttributes();
                var ATTACK_DAMAGE = Attributes.ATTACK_DAMAGE;
                var FOLLOW_RANGE  = Attributes.FOLLOW_RANGE;
                var KNOCKBACK_RES = Attributes.KNOCKBACK_RESISTANCE;
                var MOVE_SPEED    = Attributes.MOVEMENT_SPEED;
                var INTERACTIVE_RANGE    = Attributes.ENTITY_INTERACTION_RANGE;

                if (!map.hasAttribute(ATTACK_DAMAGE)) map.registerAttribute(ATTACK_DAMAGE);
//                if (!map.hasAttribute(FOLLOW_RANGE))  map.registerAttribute(FOLLOW_RANGE);
//                if (!map.hasAttribute(KNOCKBACK_RES)) map.registerAttribute(KNOCKBACK_RES);
//                if (!map.hasAttribute(MOVE_SPEED))    map.registerAttribute(MOVE_SPEED);
//                if (!map.hasAttribute(INTERACTIVE_RANGE))    map.registerAttribute(INTERACTIVE_RANGE);

                var dmg = map.getInstance(ATTACK_DAMAGE);
                if (dmg != null && dmg.getBaseValue() <= 0.0D) dmg.setBaseValue(1.0D);

                nmsMob.getAttributes().assignBaseValues(new AttributeMap(Zombie.createAttributes().build()));
//
//                var range = map.getInstance(INTERACTIVE_RANGE);
//                if (range != null && range.getBaseValue() <= 0.0D) range.setBaseValue(4.0D);
//
//                var follow = map.getInstance(FOLLOW_RANGE);
//                if (follow != null && follow.getBaseValue() < 32.0D) follow.setBaseValue(48.0D);
//
//                var kb = map.getInstance(KNOCKBACK_RES);
//                if (kb != null && kb.getBaseValue() < 0.6D) kb.setBaseValue(0.9D);
//
//                var sp = map.getInstance(MOVE_SPEED);
//                if (sp != null) {
//                    double v = sp.getBaseValue();
//                    if (v <= 0.0D) v = 0.30D;
//                    sp.setBaseValue(Math.max(0.20D, Math.min(v, 0.40D)));
//                }
            } catch (Throwable ignored) {}

            if (nms instanceof PathfinderMob pfm) {
                nmsMob.goalSelector.addGoal(1, new MeleeAttackGoal(pfm, 1.0D, true));
                nmsMob.goalSelector.addGoal(2, new LookAtPlayerGoal(pfm, net.minecraft.world.entity.player.Player.class, 1.0F));
                nmsMob.goalSelector.addGoal(3, new RandomLookAroundGoal(pfm));
                nmsMob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(pfm, net.minecraft.world.entity.player.Player.class, true));
            } else {
                NexusPlugin.nexusLogger.warning("Failed to convert to dummy attacker (PathfinderMob required): " + mob.getType());
                try { mob.setAggressive(true); } catch (Throwable ignored) {}
            }

        } catch (Throwable t) {
            NexusPlugin.nexusLogger.error("Failed to build nms attacker: " + mob.getType());
            t.printStackTrace();
        }
    }

    private static void clearAllGoals(GoalSelector selector) {
        try {
            var copy = new ArrayList<>(selector.getAvailableGoals());
            for (var w : copy) selector.removeGoal(w);
        } catch (Throwable ignored) { }
    }

    private static void removeGoalBySimpleName(GoalSelector selector, String simpleName) {
        try {
            var goals = selector.getAvailableGoals(); // Set<WrappedGoal>
            var toRemove = new ArrayList<WrappedGoal>();
            for (var w : goals) {
                var g = w.getGoal();
                if (g.getClass().getSimpleName().equals(simpleName)) {
                    toRemove.add(w);
                }
            }
            toRemove.forEach(selector::removeGoal);
        } catch (Throwable ignored) { }
    }

    private static void applyAttr(LivingEntity le, Attribute attr, Double value) {
        if (value == null || attr == null) return;
        AttributeInstance inst = le.getAttribute(attr);
        if (inst != null) {
            double v = value;
            inst.setBaseValue(v);
        }
    }
}