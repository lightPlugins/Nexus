package io.nexstudios.nexus.bukkit.hooks;

import com.willfp.eco.core.items.Items;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class EcoItemsHook {

    public ItemStack getEcoItem(String id) {
        ItemStack is = Items.lookup("ecoitems:" + id).getItem();

        if(is != null) {
            return is;
        }
        return ItemStack.of(Material.DEEPSLATE);
    }

}
