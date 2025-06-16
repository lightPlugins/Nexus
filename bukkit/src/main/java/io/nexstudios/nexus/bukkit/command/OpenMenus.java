package io.nexstudios.nexus.bukkit.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import io.nexstudios.nexus.bukkit.Nexus;
import io.nexstudios.nexus.bukkit.inventory.NexusInventory;
import io.nexstudios.nexus.bukkit.inventory.models.InventoryData;
import io.nexstudios.nexus.bukkit.inventory.models.MenuItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
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
    public void onReload(CommandSender sender, String inventoryName) {

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        InventoryData inventoryData = Nexus.getInstance().getNexusInventoryData().get(inventoryName);
        boolean isDefaultLanguage = Nexus.getInstance().getNexusLanguage().hasPlayerDefaultLanguage(uuid);
        if(!isDefaultLanguage) {
            // update the language for the player if they have a custom language set
            Nexus.nexusLogger.debug("Updating inventory language for player " + player.getName() + " with UUID: " + uuid, 3);
            inventoryData.updateLanguage(uuid);
        }
        NexusInventory nexusInventory = getNexusInventory(inventoryData);
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
