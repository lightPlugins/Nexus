package io.nexstudios.nexus.bukkit.items;

import org.bukkit.attribute.AttributeModifier;

/**
 * Abbildung:
 * 0 -> ADD_NUMBER
 * 1 -> ADD_SCALAR
 * 2 -> MULTIPLY_SCALAR_1
 *
 * - Konstanten und simple switch-Zweige sind JIT-freundlich und haben keinen messbaren Overhead.
 */
public enum AttributeOperation {
    ADD_NUMBER(0, AttributeModifier.Operation.ADD_NUMBER),
    ADD_SCALAR(1, AttributeModifier.Operation.ADD_SCALAR),
    MULTIPLY_SCALAR_1(2, AttributeModifier.Operation.MULTIPLY_SCALAR_1);

    private final int id;
    private final AttributeModifier.Operation bukkit;

    AttributeOperation(int id, AttributeModifier.Operation bukkit) {
        this.id = id;
        this.bukkit = bukkit;
    }

    public int toInt() {
        return id;
    }

    public AttributeModifier.Operation toBukkit() {
        return bukkit;
    }

    public static AttributeOperation fromInt(int value) {
        return switch (value) {
            case 0 -> ADD_NUMBER;
            case 1 -> ADD_SCALAR;
            case 2 -> MULTIPLY_SCALAR_1;
            default -> ADD_NUMBER; // Fallback
        };
    }
}