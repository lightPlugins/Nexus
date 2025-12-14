package io.nexstudios.internal.nms.v1_21_11.items;

import io.nexstudios.nexus.bukkit.items.*;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.*;
import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class PaperItemBuilder implements ItemBuilder, ItemBuilderFactory {

    private ItemStack baseStack;
    private Material material = Material.AIR;
    private int amount = 1;
    private int stackSize = 64;

    private Component displayName;
    private final List<Component> lore = new ArrayList<>();

    private Map<Enchantment, Integer> enchants;

    private final List<Attr> attributes = new ArrayList<>();

    private Set<ItemHideFlag> hideFlags;

    private Integer customModelData;
    private NamespacedKey tooltipStyleKey;
    private NamespacedKey itemModelKey;

    private Boolean unbreakable;

    private record Attr(String name, NamespacedKey attributeKey, double amount, AttributeOperation operation, Set<EquipmentSlot> slots) {}

    @Override
    public ItemBuilder create() {
        return new PaperItemBuilder();
    }

    @Override
    public ItemBuilder itemStack(ItemStack stack) {
        this.baseStack = stack;
        return this;
    }

    @Override
    public ItemBuilder material(Material material) {
        this.material = Objects.requireNonNull(material, "ItemBuilder -> Material cant be null!");
        return this;
    }

    @Override
    public ItemBuilder amount(int amount) {
        this.amount = Math.max(1, amount);
        return this;
    }

    @Override
    public ItemBuilder stackSize(int stackSize) {
        int maxStackSize = 99;
        this.stackSize = Math.max(1, Math.min(stackSize, maxStackSize));
        return this;
    }

    @Override
    public ItemBuilder displayName(Component name) {
        this.displayName = name.decoration(TextDecoration.ITALIC, false);
        return this;
    }

    @Override
    public ItemBuilder lore(List<Component> lines) {
        for(Component line : lines) {
            if(line == null) continue;
            this.lore.add(line.decoration(TextDecoration.ITALIC, false));
        }
        return this;
    }

    @Override
    public ItemBuilder enchantments(Map<Enchantment, Integer> enchants) {
        this.enchants = (enchants == null || enchants.isEmpty()) ? null : Map.copyOf(enchants);
        return this;
    }

    @Override
    public ItemBuilder attribute(String name, NamespacedKey attributeKey, double amount, AttributeOperation operation, EquipmentSlot... slots) {
        Set<EquipmentSlot> slotSet = null;
        if (slots != null && slots.length > 0) {
            slotSet = EnumSet.noneOf(EquipmentSlot.class);
            Collections.addAll(slotSet, slots);
        }
        attributes.add(new Attr(name == null ? "" : name, Objects.requireNonNull(attributeKey), amount, Objects.requireNonNull(operation), slotSet));
        return this;
    }

    @Override
    public ItemBuilder attributes(ItemAttributeSpec... specs) {
        if (specs == null) return this;
        for (ItemAttributeSpec s : specs) {
            if (s == null) continue;
            attributes.add(new Attr(
                    s.name() == null ? "" : s.name(),
                    Objects.requireNonNull(s.attributeKey(), "attributeKey"),
                    s.amount(),
                    Objects.requireNonNull(s.operation(), "operation"),
                    (s.slots() == null || s.slots().isEmpty()) ? null : EnumSet.copyOf(s.slots())
            ));
        }
        return this;
    }

    @Override
    public ItemBuilder attributes(Collection<ItemAttributeSpec> specs) {
        if (specs == null || specs.isEmpty()) return this;
        for (ItemAttributeSpec s : specs) {
            if (s == null) continue;
            attributes.add(new Attr(
                    s.name() == null ? "" : s.name(),
                    Objects.requireNonNull(s.attributeKey(), "attributeKey"),
                    s.amount(),
                    Objects.requireNonNull(s.operation(), "operation"),
                    (s.slots() == null || s.slots().isEmpty()) ? null : EnumSet.copyOf(s.slots())
            ));
        }
        return this;
    }

    @Override
    public ItemBuilder hideFlags(Set<ItemHideFlag> flags) {
        this.hideFlags = (flags == null || flags.isEmpty()) ? null : EnumSet.copyOf(flags);
        return this;
    }

    @Override
    public ItemBuilder modelData(Integer modelData) {
        this.customModelData = modelData;
        return this;
    }

    @Override
    public ItemBuilder tooltipStyle(NamespacedKey styleKey) {
        this.tooltipStyleKey = styleKey;
        return this;
    }

    @Override
    public ItemBuilder itemModel(NamespacedKey modelKey) {
        this.itemModelKey = modelKey;
        return this;
    }

    @Override
    public ItemBuilder unbreakable(Boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public ItemStack build() {
        ItemStack stack = new ItemStack(material, amount);

        if(baseStack != null) {
            stack = baseStack;
        }

        stack.setData(DataComponentTypes.MAX_STACK_SIZE, stackSize);

        if (displayName != null) {
            stack.setData(DataComponentTypes.CUSTOM_NAME, displayName);
        }

        ItemLore.Builder lb = ItemLore.lore();
        for (Component line : lore) lb.addLine(line == null ? Component.empty() : line);
        stack.setData(DataComponentTypes.LORE, lb.build());

        if (unbreakable != null && unbreakable) {
            stack.setData(DataComponentTypes.UNBREAKABLE);
        }

        if (customModelData != null) {
            CustomModelData.Builder cmd = CustomModelData.customModelData().addFloat(customModelData);
            stack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, cmd.build());
        }

        if (tooltipStyleKey != null) {
            stack.setData(DataComponentTypes.TOOLTIP_STYLE, tooltipStyleKey);
        }

        if (itemModelKey != null) {
            stack.setData(DataComponentTypes.ITEM_MODEL, itemModelKey);
        }

        if (enchants != null && !enchants.isEmpty()) {
            ItemEnchantments.Builder eb = ItemEnchantments.itemEnchantments();
            for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
                int lvl = Math.max(1, e.getValue() == null ? 1 : e.getValue());
                eb.add(e.getKey(), lvl);
            }
            stack.setData(DataComponentTypes.ENCHANTMENTS, eb.build());
        }

        // Attribute Modifiers
        if (!attributes.isEmpty()) {
            ItemAttributeModifiers.Builder ab = ItemAttributeModifiers.itemAttributes();

            for (Attr a : attributes) {
                Attribute bukkitAttr = resolveBukkitAttribute(a.attributeKey());
                if (bukkitAttr == null) continue;

                AttributeModifier.Operation op = switch (a.operation()) {
                    case ADD_SCALAR -> AttributeModifier.Operation.ADD_SCALAR;
                    case MULTIPLY_SCALAR_1 -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                    default -> AttributeModifier.Operation.ADD_NUMBER;
                };

                String baseId = (a.attributeKey().getKey() + "_" + (a.name() == null ? "" : a.name())).toLowerCase(Locale.ROOT);
                String safeBaseId = baseId.replaceAll("[^a-z0-9._-]", "_");

                if (a.slots() == null || a.slots().isEmpty()) {
                    NamespacedKey key = new NamespacedKey(a.attributeKey().getNamespace(), safeBaseId);
                    AttributeModifier mod = new AttributeModifier(key, a.amount(), op);
                    ab.addModifier(bukkitAttr, mod);
                } else {
                    for (EquipmentSlot slot : a.slots()) {
                        if (slot == null) continue;
                        String perSlotId = safeBaseId + "_" + slot.name().toLowerCase(Locale.ROOT);
                        NamespacedKey key = new NamespacedKey(a.attributeKey().getNamespace(), perSlotId);
                        AttributeModifier mod = new AttributeModifier(key, a.amount(), op);
                        ab.addModifier(bukkitAttr, mod, slot.getGroup());
                    }
                }
            }

            stack.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ab.build());
        }

        if (hideFlags != null && hideFlags.contains(ItemHideFlag.HIDE_ATTRIBUTES)) {
            ItemAttributeModifiers current = stack.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (current != null) {
                ItemAttributeModifiers.Builder hidden = ItemAttributeModifiers.itemAttributes();
                for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
                    hidden.addModifier(
                            entry.attribute(),
                            entry.modifier(),
                            entry.getGroup(),
                            AttributeModifierDisplay.hidden()
                    );
                }
                stack.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, hidden.build());
            }
        }


        // Hide Enchantments (Tooltip)
        if (hideFlags != null && hideFlags.contains(ItemHideFlag.HIDE_ENCHANTS) && stack.hasData(DataComponentTypes.ENCHANTMENTS)) {
            stack.setData(
                    DataComponentTypes.TOOLTIP_DISPLAY,
                    TooltipDisplay.tooltipDisplay()
                            .addHiddenComponents(DataComponentTypes.ENCHANTMENTS)
                            .build()
            );
        }

        return stack;
    }

    private static @NotNull AttributeModifier getAttributeModifier(Attr a) {
        AttributeModifier.Operation op = switch (a.operation()) {
            case ADD_SCALAR -> AttributeModifier.Operation.ADD_SCALAR;
            case MULTIPLY_SCALAR_1 -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> AttributeModifier.Operation.ADD_NUMBER;
        };

        String rawId = (a.attributeKey().getKey() + "_" + (a.name() == null ? "" : a.name())).toLowerCase(Locale.ROOT);
        String safeId = rawId.replaceAll("[^a-z0-9._-]", "_");
        NamespacedKey modifierKey = new NamespacedKey(a.attributeKey().getNamespace(), safeId);

        return new AttributeModifier(modifierKey, a.amount(), op);
    }

    private Attribute resolveBukkitAttribute(NamespacedKey key) {
        if (key == null) return null;
        String full = (key.getNamespace() + ":" + key.getKey()).toLowerCase(Locale.ROOT);

        if (full.endsWith("attack_damage")) return Attribute.ATTACK_DAMAGE;
        if (full.endsWith("attack_speed")) return Attribute.ATTACK_SPEED;
        if (full.endsWith("armor")) return Attribute.ARMOR;
        if (full.endsWith("armor_toughness")) return Attribute.ARMOR_TOUGHNESS;
        if (full.endsWith("max_health")) return Attribute.MAX_HEALTH;
        if (full.endsWith("movement_speed")) return Attribute.MOVEMENT_SPEED;
        if (full.endsWith("knockback_resistance")) return Attribute.KNOCKBACK_RESISTANCE;
        if (full.endsWith("luck")) return Attribute.LUCK;
        if (full.endsWith("attack_knockback")) return Attribute.ATTACK_KNOCKBACK;
        if (full.endsWith("step_height")) return Attribute.STEP_HEIGHT;
        if (full.endsWith("fall_damage_multiplier")) return Attribute.FALL_DAMAGE_MULTIPLIER;
        if (full.endsWith("jump_strength")) return Attribute.JUMP_STRENGTH;
        if (full.endsWith("gravity")) return Attribute.GRAVITY;
        if (full.endsWith("spawn_reinforcements")) return Attribute.SPAWN_REINFORCEMENTS;

        return null;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public boolean hasCustomName() {
        if(displayName == null) return false;
        if(baseStack != null) {
            Component name = baseStack.getData(DataComponentTypes.CUSTOM_NAME);
            return name != null;
        }
        return true;
    }
}