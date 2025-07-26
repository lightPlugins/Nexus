package io.nexstudios.nexus.bukkit.droptable.fanydrop;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.items.ItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.UUID;


public class FancyDrop implements Listener {


    @EventHandler
    public void onEntityDeath(MythicMobDeathEvent event) {

        if(!event.getMobType().getInternalName().equalsIgnoreCase("guard")) {
            return;
        }

        if(event.getKiller() instanceof Player player) {
            Key tooltipStyle = Key.key("minecraft", "rare");



            for (int i = 0; i < 10; i++) {
                ItemBuilder drop = ItemBuilderFactory.getItemBuilder(Material.COAL)
                        .setAmount(1)
                        .setDisplayName(MiniMessage.miniMessage().deserialize("<red>Super Coal Key A " + i))
                        .setTooltipStyle(tooltipStyle)
                        .build();
                dropVelocityItemWithParticles(drop.getItemStack().clone(), event.getEntity().getLocation(), player);
            }
        }
    }


    private void dropVelocityItemWithParticles(ItemStack itemStack, Location location, Player player) {
        Item item = location.getWorld().dropItem(location, itemStack);

        String teamName = "glow_" + UUID.randomUUID();
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.color(NamedTextColor.RED);
        }


        // Zufällige Velocity für den Drop festlegen
        double randomX = (Math.random() * 0.4) - 0.2;
        double randomZ = (Math.random() * 0.4) - 0.2;
        double randomY = 0.5 + (Math.random() * (0.8 - 0.5)); // Bereich 0.1 bis 0.3

        item.setVelocity(new Vector(randomX, randomY, randomZ));
        item.setOwner(player.getUniqueId());
        item.setGlowing(true);
        item.setVisibleByDefault(false);

        team.addEntry(item.getUniqueId().toString());

        player.showEntity(NexusPlugin.getInstance(), item);

        // Partikel-Trail hinzufügen
        new BukkitRunnable() {
            @Override
            public void run() {
                // Beenden, wenn das Item ungültig oder entfernt wurde
                if (!item.isValid() || item.isDead()) {
                    this.cancel();
                    return;
                }
                // Partikel an der aktuellen Position des Items spawnen
                player.spawnParticle(Particle.FLAME, item.getLocation(), 5, 0, 0, 0, 0.01); // Beispielsweise Partikel FLAME
            }
        }.runTaskTimer(NexusPlugin.getInstance(), 0L, 1L); // Timer startet sofort und läuft alle 2 Ticks
    }


}
