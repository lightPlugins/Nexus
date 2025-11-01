package io.nexstudios.nexus.bukkit.hooks.auroracollections;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.collections.api.AuroraCollectionsProvider;
import gg.auroramc.collections.collection.CollectionManager;
import gg.auroramc.collections.collection.Trigger;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public class AuroraCollectionsHook {

    private CollectionManager collectionManager() {
        return AuroraCollectionsProvider.getCollectionManager();
    }

    public void addCollection(Player player, ItemStack itemStack, int amount) {
        TypeId typeId = AuroraAPI.getItemManager().resolveId(itemStack);
        collectionManager().progressCollections(player, typeId, amount, Trigger.BLOCK_LOOT);
    }
}
