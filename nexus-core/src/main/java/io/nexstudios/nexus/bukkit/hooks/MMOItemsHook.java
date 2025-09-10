package io.nexstudios.nexus.bukkit.hooks;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.api.stat.modifier.TemporaryStatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierSource;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import lombok.Getter;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.manager.TypeManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Getter
public class MMOItemsHook {


    public MMOItems instance() {
        return MMOItems.plugin;
    }

    public TypeManager typeManager() {
        return instance().getTypes();
    }

    public ItemStack getMMOItemsStack(String category, String id) {

        MMOItem item = instance().getMMOItem(typeManager().get(category.toUpperCase(Locale.ROOT)), id.toUpperCase(Locale.ROOT));

        if(item == null) {
            NexusPlugin.nexusLogger.error("Could not find mmoitem with id: " + category + ":" + id);
            return ItemStack.of(Material.COBBLESTONE);
        }

        ItemStack is = item.newBuilder().build();

        if(is == null) {
            NexusPlugin.nexusLogger.error("Could not find itemstack by mmoitem with id: " + category + ":" + id);
            is = ItemStack.of(Material.DEEPSLATE);
        }
        return is;
    }

    /**
     * Applies a stat modifier to a player's MMO data instance using the specified parameters.
     * This method registers the stat modification under a given namespace and is processed
     * within the player's MMOPlayerData context.
     *
     * @param namespace the unique namespace used to identify the stat modifier
     * @param player the player to whom the stat modifier will be applied
     * @param stat the specific stat to modify (e.g., damage, defense)
     * @param value the value of the stat modifier to apply
     * @param modifierType the type of the stat modifier (e.g., addition, multiplier)
     */
    public void applyRuntimeStat(String namespace, Player player, String stat, double value, ModifierType modifierType) {

        MMOPlayerData data = MMOPlayerData.get(player);
        StatModifier modifier = new StatModifier(
                namespace.toLowerCase(Locale.ROOT),
                stat.toUpperCase(Locale.ROOT),
                value,
                modifierType);
        modifier.register(data);
    }

    /**
     * Applies a temporary stat modifier to a player's MMO data instance using the specified parameters.
     * This stat modifier will be automatically removed after the specified duration.
     *
     * @param namespace the unique namespace to identify the stat modifier
     * @param player the player to whom the stat modifier will be applied
     * @param stat the specific stat to modify
     * @param value the value of the stat modifier to apply
     * @param duration the duration for which the stat modifier will be active
     * @param modifierType the type of the stat modifier (e.g., addition, multiplier)
     */
    public void applyTemporaryStat(String namespace, Player player, String stat, double value, Duration duration, ModifierType modifierType) {

        long millis = duration.toMillis();
        long ticks = millis / 50;

        MMOPlayerData data = MMOPlayerData.get(player);
        TemporaryStatModifier modifier2 = new TemporaryStatModifier(
                namespace.toLowerCase(Locale.ROOT),
                stat.toUpperCase(Locale.ROOT),
                value,
                modifierType,
                EquipmentSlot.OTHER,
                ModifierSource.OTHER);
        modifier2.register(data, ticks);
    }

    public int getUpgradeLevel(String type, String id) {

       MMOItem item = instance().getMMOItem(Type.get(type), id);
       if(item == null) {
           return 0;
       }

       return item.getUpgradeLevel();
    }

    public int getUpgradeLevel(ItemStack is) {
        NBTItem nbt = NBTItem.get(is);
        if(!nbt.hasType()) {
            return -1;
        }
        String id = nbt.getString("MMOITEMS_ITEM_ID");
        String type = nbt.getType();

        MMOItem mmoitem = MMOItems.plugin.getMMOItem(MMOItems.plugin.getTypes().get(type), id);
        if(mmoitem == null) {
            return -2;
        }

        return mmoitem.getUpgradeLevel();
    }

    @Nullable
    public ItemStack getUpgradedMMOItemStack(ItemStack is, int level) {
        NBTItem nbt = NBTItem.get(is);

        if(!nbt.hasType()) {
            return null;
        }

        if(level < 0 || level == 0) {
            NexusPlugin.getInstance().getNexusLogger().error(List.of(
                    "Invalid mmoitems upgrade level: " + level,
                    "Expected level >= 1"
            ));
            return null;
        }

        String id = nbt.getString("MMOITEMS_ITEM_ID");
        String type = nbt.getType();

        MMOItem mmoitem = MMOItems.plugin.getMMOItem(MMOItems.plugin.getTypes().get(type), id);
        if(mmoitem == null) {
            return null;
        }

        mmoitem.getUpgradeTemplate().upgradeTo(mmoitem, level);

        return mmoitem.newBuilder().build();
    }

}
