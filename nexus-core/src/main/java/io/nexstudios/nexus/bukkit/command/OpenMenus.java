package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.bukkit.inventory.NexusInventory;
import io.nexstudios.nexus.bukkit.inventory.models.InventoryData;
import io.nexstudios.nexus.bukkit.inventory.models.MenuItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
        FileConfiguration config = Nexus.getInstance().getInventoryFileByName(inventoryName);
        if(config == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Inventory '" + inventoryName + "' not found!</red>"));
            return;
        }
        InventoryData inv = new InventoryData(config, Nexus.getInstance().getNexusLanguage(), inventoryName);
        inv.updateLanguage(uuid);
        NexusInventory nexusInventory = getNexusInventory(inv);
        nexusInventory.open((Player) sender);

    }

    @NotNull
    private static NexusInventory getNexusInventory(InventoryData inventoryData) {
        NexusInventory nexusInventory = new NexusInventory(Nexus.getInstance(), inventoryData);
        List<MenuItem> testItems = new ArrayList<>();
        for(int i = 0; i < 54; i++) {
            int finalI = i;
            MenuItem item = new MenuItem(new ItemStack(Material.ACACIA_BOAT), (event, menuItem) -> {
                event.getWhoClicked().sendMessage(MiniMessage.miniMessage().deserialize("<green>Test Item " + finalI + "</green>"));
            });
            testItems.add(item);
        }
        nexusInventory.setItemList(testItems, 10, 16);
        return nexusInventory;
    }
}
