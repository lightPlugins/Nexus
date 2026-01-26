package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConditionHasAge implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    /**
     * Synchrone Prüfung:
     * - nutzt nur targetLocation + ConditionData
     */
    @Override
    public boolean checkSync(NexusConditionContext context) {
        ConditionData data = context.data();
        Location targetLocation = context.location();

        if (targetLocation == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Could not check provided condition",
                    "Condition 'has_age' needs a target location to work"
            ));
            return false;
        }

        Block blockAtLocation = targetLocation.getBlock();

        Object onlyBlockObj = data.getData().get("block");
        if (onlyBlockObj != null) {
            String onlyBlockId = String.valueOf(onlyBlockObj).trim();
            if (!onlyBlockId.isEmpty()) {
                // Uses StringUtils prefix-based block matcher (minecraft/vanilla/itemsadder)
                if (!StringUtils.matchesBlock(blockAtLocation, onlyBlockId)) {
                    // Not the configured block -> fail silently (no log)
                    return false;
                }
            }
        }

        Object ageObj = data.getData().get("age");
        if (ageObj == null) {
            NexusPlugin.nexusLogger.error("Missing 'age' parameter for condition has-age");
            return false;
        }

        int requiredAge;
        if (ageObj instanceof Number n) {
            requiredAge = n.intValue();
        } else {
            try {
                requiredAge = Integer.parseInt(String.valueOf(ageObj));
            } catch (NumberFormatException e) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid age value for condition has-age: " + ageObj,
                        "Expected an integer age value."
                ));
                return false;
            }
        }

        String modeRaw = String.valueOf(data.getData().getOrDefault("mode", "=="));
        Mode mode = Mode.fromConfig(modeRaw);

        boolean clampToRange = (boolean) data.getData().getOrDefault("clamp-to-range", false);

        if (!(blockAtLocation.getBlockData() instanceof Ageable ageable)) {
            // Not ageable -> fail silently to avoid console spam
            return false;
        }

        int maxAge = ageable.getMaximumAge();

        if (requiredAge < 0 || requiredAge > maxAge) {
            if (!clampToRange) {
                return false;
            }
            requiredAge = Math.max(0, Math.min(requiredAge, maxAge));
        }

        int currentAge = ageable.getAge();

        return switch (mode) {
            case EQ -> currentAge == requiredAge;
            case GT -> currentAge > requiredAge;
            case GTE -> currentAge >= requiredAge;
            case LT -> currentAge < requiredAge;
            case LTE -> currentAge <= requiredAge;
        };
    }

    private enum Mode {
        EQ, GT, GTE, LT, LTE;

        static Mode fromConfig(String raw) {
            if (raw == null) return EQ;
            String v = raw.trim().toLowerCase();

            return switch (v) {
                case ">", "gt", "greater", "greater-than" -> GT;
                case ">=", "gte" -> GTE;
                case "<", "lt", "less", "less-than" -> LT;
                case "<=", "lte" -> LTE;
                case "=", "==", "eq", "exact" -> EQ;
                default -> EQ;
            };
        }
    }

    @Override
    public void sendMessage(NexusConditionContext context) {
        ConditionData data = context.data();
        Location targetLocation = context.location();

        // Silent abort: no location, wrong block-filter, or block isn't ageable -> no player message
        if (targetLocation == null) return;

        Block blockAtLocation = targetLocation.getBlock();

        Object onlyBlockObj = data.getData().get("block");
        if (onlyBlockObj != null) {
            String onlyBlockId = String.valueOf(onlyBlockObj).trim();
            if (!onlyBlockId.isEmpty() && !StringUtils.matchesBlock(blockAtLocation, onlyBlockId)) {
                return;
            }
        }

        if (!(blockAtLocation.getBlockData() instanceof Ageable)) {
            return;
        }

        boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);
        boolean asActionBar = (boolean) data.getData().getOrDefault("as-actionbar", false);

        if (!sendMessage) return;

        Player player = context.player();
        if (player == null) {
            return;
        }

        if (asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}