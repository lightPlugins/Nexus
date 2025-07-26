package io.nexstudios.nexus.bukkit.droptable.fanydrop;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public interface FancyDropBuilder {

    FancyDropBuilder setItemStack(ItemStack itemStack);
    FancyDropBuilder setVelocity(Vector velocity);
    FancyDropBuilder setDropLocation(Location location);
    FancyDropBuilder setTrailCount(int count);
    FancyDropBuilder setTrailSpeed(int ticks);
    FancyDropBuilder setTrailColor(String hexColor);
    FancyDropBuilder setGlowColor(NamedTextColor color);
    FancyDropBuilder build();
    Item excetute(Player player);



}
