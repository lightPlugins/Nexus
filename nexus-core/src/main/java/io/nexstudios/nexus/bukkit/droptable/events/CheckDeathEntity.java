package io.nexstudios.nexus.bukkit.droptable.events;

import com.willfp.eco.core.items.Items;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.droptable.fanydrop.FancyDropBuilder;
import io.nexstudios.nexus.bukkit.droptable.fanydrop.FancyDropFactory;
import io.nexstudios.nexus.bukkit.droptable.models.DropTable;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CheckDeathEntity implements Listener {

    private final HashMap<LivingEntity, UUID> threadTable = new HashMap<>(); // Spieler pro Mob
    private final HashMap<LivingEntity, BukkitRunnable> taskMap = new HashMap<>(); // Timer pro Mob

    @EventHandler
    public void onEntityAttack(EntityDamageByEntityEvent event) {
        // Prüfen, ob der Angreifer ein Spieler ist
        if (event.getDamager() instanceof Player player) {
            LivingEntity entity = (LivingEntity) event.getEntity();

            // Mob und Spieler in die threadTable einfügen/aktualisieren
            threadTable.put(entity, player.getUniqueId());

            // Vorherigen Timer stoppen, falls einer existiert
            if (taskMap.containsKey(entity)) {
                taskMap.get(entity).cancel(); // Storniere den laufenden Timer
            }

            // Erstelle neuen Timer, der nach 10 Sekunden den Spieler entfernt
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    // Überprüfen, ob noch der gleiche Spieler für den Mob gespeichert ist
                    if (threadTable.containsKey(entity) && threadTable.get(entity).equals(player.getUniqueId())) {
                        threadTable.remove(entity); // Spieler entfernen
                        taskMap.remove(entity); // Timer entfernen
                    }
                }
            };

            // Timer starten und speichern
            runnable.runTaskLater(NexusPlugin.getInstance(), 20 * 10); // 10 Sekunden Verzögerung
            taskMap.put(entity, runnable); // Timer in taskMap speichern
        }
    }



    @EventHandler
    public void onDeathEntity(MythicMobDeathEvent event) {

        Player player = (Player) event.getKiller();
        MythicMob mythicMob = event.getMobType();

        if (player == null) {
            return;
        }

        NexusPlugin.getInstance().getDropTableReader().getDropTables().forEach((tableID, dropTable) -> {

            DropTable.DropConditions.KillCondition killCondition = dropTable.getConditions().getKillCondition();

            if (killCondition == null) {
                return;
            }

            killCondition.getEntityTypes().forEach(singleType -> {

                String[] split = singleType.split(":");

                if (!mythicMob.getInternalName().equalsIgnoreCase(split[1])) {
                    return;
                }

                List<LivingEntity> entitiesToRemove = new ArrayList<>();

                threadTable.forEach((entity, uuid) -> {
                    if (!entity.getUniqueId().equals(event.getEntity().getUniqueId())) {
                        return;
                    }

                    dropTable.getDrops().forEach(drop -> {

                        try {
                            double chance = Double.parseDouble(drop.getChance());
                            if (!checkChance(chance)) {
                                return;
                            }
                        } catch (NumberFormatException e) {
                            NexusPlugin.nexusLogger.error(List.of(
                                    "chance is not a number!",
                                    "Please check your " + dropTable.getId() + " drop table",
                                    "for the correct chance!"
                            ));
                            return;
                        }

                        String[] itemSplit = drop.getItem().split(":");
                        ItemStack is;

                        switch (itemSplit[0]) {
                            case "ecoitems": {
                                is = Items.lookup("ecoitems:" + itemSplit[1]).getItem();
                                break;
                            }
                            case "vanilla": {
                                is = ItemStack.of(Material.valueOf(itemSplit[1].toUpperCase()));
                                break;
                            }
                            default: {
                                NexusPlugin.nexusLogger.error(List.of(
                                        "Failed to get item with ID: " + itemSplit[1],
                                        "Please check your " + dropTable.getId() + " drop table",
                                        "for the correct item ID!"
                                ));
                                is = ItemStack.of(Material.DEEPSLATE);
                                break;
                            }
                        }

                        FancyDropBuilder fancyDropBuilder = FancyDropFactory.getFancyDropBuilder();
                        fancyDropBuilder
                                .setDropLocation(entity.getLocation())
                                .setVelocity(NexusPlugin.getInstance().dropTableReader.refreshVelocity(dropTable.getConfiguration(), dropTable))
                                .setTrailCount(10)
                                .setTrailSpeed(1)
                                .setItemStack(is)
                                .setGlowColor(drop.getSettings().getGlowColor())
                                .setTrailColor(drop.getSettings().getTrailColor())
                                .build();
                        Item item = fancyDropBuilder.excetute(player);
                        item.setOwner(player.getUniqueId());
                        item.setVisibleByDefault(false);
                        item.customName(drop.getSettings().getItemName());
                        item.setCustomNameVisible(true);
                        item.setGlowing(true);
                        player.showEntity(NexusPlugin.getInstance(), item);

                    });

                    entitiesToRemove.add(entity);
                });

                // Entferne die Entities außerhalb der Iteration über threadTable
                entitiesToRemove.forEach(entity -> {
                    threadTable.remove(entity);
                    taskMap.remove(entity);
                });
            });
        });
    }


    private boolean checkChance(double chance) {
        Random random = new java.util.Random();
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException("Chance must be between 0 and 100, inclusive. You gave " + chance);
        }
        return random.nextDouble() * 100 < chance;
    }

}
