package io.nexstudios.nexus.bukkit.entities;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.List;

public interface MobBuilder {

    MobBuilder entity(EntityType entityType);
    MobBuilder name(Component name);
    MobBuilder scale(double scale);
    MobBuilder health(double health);
    MobBuilder damage(double damage);
    MobBuilder speed(double speed);
    MobBuilder armor(double armor);
    MobBuilder armorToughness(double armorToughness);
    MobBuilder knockbackResistance(double knockbackResistance);
    MobBuilder noDamageTicks(int noDamageTicks);
    MobBuilder aggressive(boolean aggressive);
    MobBuilder baby(boolean baby);
    MobBuilder disableDrops(boolean disableDrops);

    MobBuilder main(ItemStack main);
    MobBuilder off(ItemStack main);
    MobBuilder helm(ItemStack main);
    MobBuilder chest(ItemStack main);
    MobBuilder legs(ItemStack main);
    MobBuilder boots(ItemStack main);

    MobBuilder hologramEnabled(boolean hologramEnabled);
    MobBuilder holoBillboard(String billboard);
    MobBuilder holoBackgroundColor(String backgroundColor);
    MobBuilder holoSize(Vector size);
    MobBuilder holoViewRange(int viewRange);
    MobBuilder holoSeeThrough(boolean holoSeeThrough);
    MobBuilder holoLineWidth(double holoLineWidth);
    MobBuilder holoLines(List<Component> lines);

    LivingEntity spawn(Location location, @Nullable Player player);

    static MobBuilder create() {
        return MobBuilders.create();
    }

}
