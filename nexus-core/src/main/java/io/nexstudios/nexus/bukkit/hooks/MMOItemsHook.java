package io.nexstudios.nexus.bukkit.hooks;

import lombok.Getter;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.TypeManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter
public class MMOItemsHook {


    public MMOItems instance() {
        return MMOItems.plugin;
    }

    public TypeManager typeManager() {
        return instance().getTypes();
    }

    public ItemStack getMMOItemsStack(String category, String id) {

        ItemStack is = instance().getItem(typeManager().get(category), id);

        if(is == null) {
            is = ItemStack.of(Material.DEEPSLATE);
        }
        return is;
    }

}
