package io.nexstudios.nexus.bukkit.droptable.events;

import com.willfp.eco.core.items.Items;
import com.willfp.ecoskills.api.EcoSkillsAPI;
import com.willfp.ecoskills.api.modifiers.StatModifier;
import com.willfp.ecoskills.stats.Stat;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.droptable.fanydrop.FancyDropBuilder;
import io.nexstudios.nexus.bukkit.droptable.fanydrop.FancyDropFactory;
import io.nexstudios.nexus.bukkit.droptable.models.DropTable;
import io.nexstudios.nexus.bukkit.hooks.EcoSkillsHook;
import io.nexstudios.nexus.bukkit.utils.MiniMessageUtil;
import io.nexstudios.nexus.bukkit.utils.NexusStringMath;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CheckDeathEntity implements Listener {

    private final HashMap<LivingEntity, UUID> threadTable = new HashMap<>(); // Spieler pro Mob
    private final HashMap<LivingEntity, BukkitRunnable> taskMap = new HashMap<>(); // Timer pro Mob
    private final HashMap<Item, UUID> virtualItems = new HashMap<>();

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

            handleKillConditions(killCondition, mythicMob, player, tableID, dropTable, event);
        });
    }

    private void handleKillConditions(DropTable.DropConditions.KillCondition killCondition, MythicMob mythicMob,
                                      Player player, String tableID, DropTable dropTable, MythicMobDeathEvent event) {
        killCondition.getEntityTypes().forEach(singleType -> {
            String[] split = singleType.split(":");

            if (!mythicMob.getInternalName().equalsIgnoreCase(split[1])) {
                return;
            }

            List<LivingEntity> entitiesToRemove = new ArrayList<>();
            processThreadTable(player, tableID, dropTable, event, entitiesToRemove);
            cleanUpEntities(entitiesToRemove);
        });
    }

    private void processDrop(DropTable.Drop drop, Player player, LivingEntity entity, DropTable dropTable,
                             MythicMobDeathEvent event) {
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

        ItemStack is = getItemStackByType(drop, dropTable);

        int dropMultiplier = dropMultiplier(drop, player);
        int newDropAmount = getFixedAmount(drop.getAmount()) * dropMultiplier;
        is.setAmount(newDropAmount);

        handleFancyDrop(drop, player, entity, is, dropTable, event, newDropAmount);
    }

    private int getFixedAmount(Object amountObject) {
        int amount;
        if (amountObject instanceof Integer) {
            amount = (Integer) amountObject;
        } else if (amountObject instanceof String amountString) {
            if (amountString.contains("-")) {
                String[] rangeParts = amountString.split("-");
                try {
                    int minAmount = Integer.parseInt(rangeParts[0].trim());
                    int maxAmount = Integer.parseInt(rangeParts[1].trim());

                    if (minAmount > maxAmount) {
                        NexusPlugin.nexusLogger.warning("Invalid amount-range: " + amountString + ". Swapping min and max.");
                        int temp = minAmount;
                        minAmount = maxAmount;
                        maxAmount = temp;
                    }

                    amount = (int) (Math.random() * (maxAmount - minAmount + 1)) + minAmount;
                } catch (NumberFormatException e) {
                    NexusPlugin.nexusLogger.error("Invalid number format in amount-range: " + amountString);
                    e.printStackTrace();
                    amount = 1; // Fallback auf Standardwert
                }
            } else {
                try {
                    amount = Integer.parseInt(amountString); // Einzelwert aus String übernehmen
                } catch (NumberFormatException e) {
                    NexusPlugin.nexusLogger.warning("Could not parse " + amountString + " as an integer. Falling back to 1.");
                    amount = 1; // Fallback auf Standardwert
                }
            }
        } else {
            NexusPlugin.nexusLogger.warning("Input" + amountObject + "is not an integer or a range. Falling back to 1. -> " + amountObject.getClass().getName());
            amount = 1; // Fallback auf Standardwert
        }
        return amount;
    }


    private ItemStack getItemStackByType(DropTable.Drop drop, DropTable dropTable) {
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

        return is;
    }

    private void handleFancyDrop(DropTable.Drop drop, Player player, LivingEntity entity, ItemStack is,
                                 DropTable dropTable, MythicMobDeathEvent event, int newDropAmount) {

        TagResolver mainResolver2 = TagResolver.builder()
                .tag("owner", Tag.inserting(Component.text(player.getName())))
                .tag("amount", Tag.inserting(Component.text(String.valueOf(newDropAmount))))
                .tag("mob_type", Tag.inserting(Component.text(event.getEntity().getType().toString())))
                .build();

        Component translatedItemName = MiniMessageUtil.replace(
                drop.getSettings().getItemName(), mainResolver2, player
        );

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
        item.customName(translatedItemName);
        item.setCustomNameVisible(true);
        item.setGlowing(true);
        player.showEntity(NexusPlugin.getInstance(), item);

        if(drop.getSettings().isVirtualItem()) {
            virtualItems.put(item, player.getUniqueId());
        }

        drop.executeActions(player);
    }

    @EventHandler
    public void onItemCollect(EntityPickupItemEvent event) {

        if(event.getEntity() instanceof Player player) {

            Item item = event.getItem();

            if(virtualItems.containsKey(item) && virtualItems.get(item).equals(player.getUniqueId())) {
                event.setCancelled(true);
                event.getItem().remove();
                virtualItems.remove(item);
            }
        }
    }

    private void cleanUpEntities(List<LivingEntity> entitiesToRemove) {
        entitiesToRemove.forEach(entity -> {
            threadTable.remove(entity);
            taskMap.remove(entity);
        });
    }

    private void processThreadTable(Player player, String tableID, DropTable dropTable, MythicMobDeathEvent event,
                                    List<LivingEntity> entitiesToRemove) {
        threadTable.forEach((entity, uuid) -> {
            if (!entity.getUniqueId().equals(event.getEntity().getUniqueId())) {
                NexusPlugin.nexusLogger.error("Entity is not in threadTable for table " + tableID + "!");
                return;
            }

            dropTable.getDrops().forEach(drop -> processDrop(drop, player, entity, dropTable, event));
            entitiesToRemove.add(entity);
        });
    }

    private boolean checkChance(double chance) {
        Random random = new java.util.Random();
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException("Chance must be between 0 and 100, inclusive. You gave " + chance);
        }
        return random.nextDouble() * 100 < chance;
    }

    private int dropMultiplier(DropTable.Drop drop, Player player) {

        DropTable.Drop.DropSettings dropSettings = drop.getSettings();
        String expression = dropSettings.getDropMultiplierExpression();

        if (expression == null || expression.isEmpty()) {
            return 1;
        }

        if (expression.contains("ecoskills:")) {
            expression = processEcoSkillsExpression(expression, player);
            if (expression == null) {
                return 1;
            }
        }

        double result = NexusStringMath.evaluateExpression(expression);

        if(result == 0) {
            return 1;
        }

        return calculateDropMultiplier(result);
    }


    private String processEcoSkillsExpression(String expression, Player player) {
        EcoSkillsHook ecoSkillsHook = NexusPlugin.getInstance().getEcoSkillsHook();

        if (ecoSkillsHook == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "You are trying to use ecoskills placeholder",
                    "while EcoSkills is not installed!",
                    "Failed Expression: " + expression
            ));
            return "1 + 0";
        }

        String[] split = expression.split(":");
        if (split.length != 2) {
            NexusPlugin.nexusLogger.error("EcoSkills stat expression is invalid: " + expression);
            return "1 + 0";
        }

        String statName = split[1].split(" ")[0].replace("'", "").trim();
        Stat stat = ecoSkillsHook.getStatByName(statName, player);

        if (stat == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "You are trying to use EcoSkills placeholder",
                    "while the provided stat does not exist!",
                    "Failed stat name: " + statName
            ));
            return null;
        }

        int currentStatLevel = stat.getActualLevel$core_plugin(player);

        // Ersetze den ecoskills-Teil in der ursprünglichen expression durch den aktuellen Level-Wert
        return expression.replace("ecoskills:" + statName, String.valueOf(currentStatLevel));
    }


    private int calculateDropMultiplier(double result) {
        // Der Ganzzahlanteil aus dem Ergebnis (z. B. 2 bei 2.6)
        int baseMultiplier = (int) result;

        // Der Dezimalanteil als Wahrscheinlichkeit (z. B. 0.6 bei 2.6)
        double fractionalChance = result - baseMultiplier;

        // Verwende checkChance, um zu entscheiden, ob der Dezimalanteil eine zusätzliche Multiplikation gibt
        if (checkChance(fractionalChance * 100)) {
            // Drop zusätzlich um 1 erhöhen
            baseMultiplier++;
        }

        return baseMultiplier; // Gesamter Multiplikator
    }
}
