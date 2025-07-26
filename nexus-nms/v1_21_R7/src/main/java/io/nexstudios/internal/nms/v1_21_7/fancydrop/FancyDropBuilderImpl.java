package io.nexstudios.internal.nms.v1_21_7.fancydrop;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.droptable.fanydrop.FancyDropBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.UUID;

public class FancyDropBuilderImpl implements FancyDropBuilder {


    private ItemStack itemStack;
    private Vector velocity;
    private Location dropLocation;
    private int trailCount;
    private int trailSpeed;
    private String trailColor;
    private NamedTextColor glowColor;


    @Override
    public FancyDropBuilder setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        return this;
    }

    @Override
    public FancyDropBuilder setVelocity(Vector velocity) {
        this.velocity = velocity;
        return this;
    }

    @Override
    public FancyDropBuilder setDropLocation(Location location) {
        this.dropLocation = location;
        return this;
    }

    @Override
    public FancyDropBuilder setTrailCount(int count) {
        this.trailCount = count;
        return this;
    }

    @Override
    public FancyDropBuilder setTrailSpeed(int ticks) {
        this.trailSpeed = ticks;
        return this;
    }

    @Override
    public FancyDropBuilder setTrailColor(String hexColor) {
        this.trailColor = hexColor;
        return this;
    }

    @Override
    public FancyDropBuilder setGlowColor(NamedTextColor color) {
        this.glowColor = color;
        return this;
    }

    @Nullable
    @Override
    public FancyDropBuilder build() {

        if(itemStack == null) {
            NexusPlugin.getInstance().getNexusLogger().error("FancyDropBuilder: itemStack is null");
            return null;
        }
        if(velocity == null) {
            NexusPlugin.getInstance().getNexusLogger().error("FancyDropBuilder: velocity is null");
            return null;
        }
        if(dropLocation == null) {
            NexusPlugin.getInstance().getNexusLogger().error("FancyDropBuilder: dropLocation is null");
            return null;
        }
        if(trailCount <= 0) {
            NexusPlugin.getInstance().getNexusLogger().error("FancyDropBuilder: trailCount must be greater than 0");
            return null;
        }
        if(trailSpeed <= 0) {
            NexusPlugin.getInstance().getNexusLogger().error("FancyDropBuilder: trailSpeed must be greater than 0");
            return null;
        }
        if(trailColor == null) {
            NexusPlugin.getInstance().getNexusLogger().error("FancyDropBuilder: trailColor is null");
            return null;
        }

        return this;
    }

    @Override
    public Item excetute(Player player) {

        Item item = dropLocation.getWorld().dropItem(dropLocation, itemStack);

        item.setVelocity(velocity);

        if(glowColor != null) {
            String teamName = "glow_" + UUID.randomUUID();
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(teamName);

            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                team.color(glowColor);
            }

            team.addEntry(item.getUniqueId().toString());
        }

        java.awt.Color color = java.awt.Color.decode(trailColor);
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();


        Particle.DustOptions dustOptions = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(red, green, blue), // Bukkit-Farbwert
                1.0f // Größe des Partikels
        );
        // Partikel-Trail hinzufügen
        new BukkitRunnable() {
            @Override
            public void run() {
                // Beenden, wenn das Item ungültig oder entfernt wurde
                if (!item.isValid() || item.isDead()) {
                    this.cancel();
                    return;
                }
                player.spawnParticle(Particle.DUST, item.getLocation(), trailCount, 0, 0, 0, 0.01, dustOptions);
            }
        }.runTaskTimer(NexusPlugin.getInstance(), 0L, trailSpeed);

        return item;
    }
}
