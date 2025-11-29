package io.nexstudios.nexus.bukkit.fakebreak;

import io.nexstudios.nexus.bukkit.platform.NexServices;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-Listener:
 *
 * - Wenn ein Spieler beginnt, einen Block abzubauen:
 *   -> "Fange an block abzubauen"
 * - Solange er aktiv weiter "abbaut" (Arm-Swings auf denselben Block),
 *   wird der Zustand aktualisiert.
 * - Wenn für eine gewisse Zeit (Timeout) keine Aktivität mehr ist,
 *   oder er sichtbar nicht mehr auf denselben Block schlägt:
 *   -> "Haut den block nicht mehr"
 */
public final class FakeBreakAllBlocksListener implements Listener {

    private final Plugin plugin;

    public FakeBreakAllBlocksListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        event.getPlayer().sendMessage("Block starting damage");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().addItem(createCustomPickaxe(plugin));
    }


    @EventHandler
    public void onBlockStopDamage(BlockDamageAbortEvent event) {
        event.getPlayer().sendMessage("Block stopped damage");
    }

    public ItemStack createCustomPickaxe(Plugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // NamespacedKey statt UUID+String
        NamespacedKey key = new NamespacedKey(plugin, "nexus_block_break_speed");

        AttributeModifier modifier = new AttributeModifier(
                key,
                -0.85,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlotGroup.MAINHAND
        );

        meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, modifier);
        item.setItemMeta(meta);

        return item;
    }

}