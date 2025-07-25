package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.inventory.NexusInventory;
import io.nexstudios.nexus.bukkit.inventory.models.InventoryData;
import io.nexstudios.nexus.bukkit.inventory.models.MenuItem;
import io.nexstudios.nexus.bukkit.items.ItemBuilder;
import io.nexstudios.nexus.bukkit.items.ItemBuilderFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CommandAlias("nexus")
public class OpenMenus extends BaseCommand {

    @Subcommand("open")
    @CommandPermission("nexus.command.admin.open")
    @Description("Opens a found inventory menu.")
    @CommandCompletion("@inventories")
    public void onOpen(CommandSender sender, String inventoryName) {

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        FileConfiguration config = NexusPlugin.getInstance().getInventoryFileByName(inventoryName);
        if(config == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Inventory '" + inventoryName + "' not found!</red>"));
            return;
        }
        InventoryData inv = new InventoryData(config, NexusPlugin.getInstance().getNexusLanguage(), inventoryName);
        inv.updateLanguage(uuid);
        NexusInventory nexusInventory = getNexusInventory(inv);
        nexusInventory.open((Player) sender);

    }

    @NotNull
    private static NexusInventory getNexusInventory(InventoryData inventoryData) {
        NexusInventory nexusInventory = new NexusInventory(NexusPlugin.getInstance(), inventoryData);
        List<MenuItem> testItems = new ArrayList<>();
        for(int i = 0; i < 54; i++) {
            int finalI = i;
            ItemBuilder itemBuilder = ItemBuilderFactory.getItemBuilder(Material.ACACIA_LEAVES)
                    .setDisplayName(Component.text("<red>Test Item " + finalI + "</red>"))
                    .addEnchantment(Enchantment.EFFICIENCY, 20)
                    .build();
            MenuItem item = new MenuItem(itemBuilder.getItemStack(), (event, menuItem) -> {
                event.getWhoClicked().sendMessage(MiniMessage.miniMessage().deserialize("<green>Test Item " + finalI + "</green>"));
            });
            testItems.add(item);
        }
        nexusInventory.setItemList(testItems, 10, 16);
        return nexusInventory;
    }
}
