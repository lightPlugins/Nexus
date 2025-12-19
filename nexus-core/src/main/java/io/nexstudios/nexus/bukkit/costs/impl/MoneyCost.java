package io.nexstudios.nexus.bukkit.costs.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.costs.CostData;
import io.nexstudios.nexus.bukkit.costs.CostResult;
import io.nexstudios.nexus.bukkit.costs.NexusCost;
import io.nexstudios.nexus.bukkit.costs.NexusCostContext;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "money" cost implementation using Vault.
 *
 * Config example:
 *
 * costs:
 *   - id: money
 *     amount: 15000
 *     placeholder: "<needed-amount> Coins / <current-amount> Coins"
 *     send-message: true
 *     message: "<red>You need <dark_red><needed-amount> <red>to buy this"
 *     success-actions: []
 *     fail-actions: []
 */
public class MoneyCost implements NexusCost {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public CostResult resolveSync(NexusCostContext context) {

        CostData data = context.data();
        Player player = context.player();

        if (player == null) {
            NexusPlugin.nexusLogger.error("Money cost requires an online player.");
            return CostResult.fail();
        }

        if (NexusPlugin.getInstance().vaultHook == null) {
            NexusPlugin.nexusLogger.error("Money cost requires Vault, but Vault hook is null.");
            return CostResult.fail();
        }

        Economy economy = NexusPlugin.getInstance().vaultHook.getEconomy();
        if (economy == null) {
            NexusPlugin.nexusLogger.error("Money cost could not get Vault Economy instance.");
            return CostResult.fail();
        }

        Object amountObj = data.getData().get("amount");
        if (amountObj == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Missing 'amount' parameter for money cost.",
                    "Please define 'amount' in your cost configuration."
            ));
            return CostResult.fail();
        }

        double requiredAmount = parseAmount(amountObj, context);
        if (requiredAmount <= 0) {
            // Non-positive amounts are considered free
            return CostResult.success();
        }

        double currentBalance = economy.getBalance(player);

        // Prepare placeholder params
        Map<String, String> params = new HashMap<>();
        params.put("needed-amount", String.valueOf(requiredAmount));
        params.put("current-amount", String.valueOf(currentBalance));
        params.put("currency", economy.currencyNamePlural());

        if (currentBalance < requiredAmount) {
            // Not enough money, do not withdraw
            return CostResult.fail(params);
        }

        // Withdraw the required amount
        var response = economy.withdrawPlayer(player, requiredAmount);
        if (!response.transactionSuccess()) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Failed to withdraw money from player for money cost.",
                    "Reason: " + response.errorMessage
            ));
            return CostResult.fail(params);
        }

        return CostResult.success(params);
    }

    /**
     * Preview without any state change.
     * Uses the same amount/balance logic as resolveSync, but does NOT withdraw money.
     */
    @Override
    public CostResult previewSync(NexusCostContext context) {

        CostData data = context.data();
        Player player = context.player();

        if (player == null) {
            return CostResult.fail();
        }

        if (NexusPlugin.getInstance().vaultHook == null) {
            return CostResult.fail();
        }

        Economy economy = NexusPlugin.getInstance().vaultHook.getEconomy();
        if (economy == null) {
            return CostResult.fail();
        }

        Object amountObj = data.getData().get("amount");
        if (amountObj == null) {
            return CostResult.fail();
        }

        double requiredAmount = parseAmount(amountObj, context);
        if (requiredAmount <= 0) {
            // Free cost, but we still return placeholders
            double currentBalance = economy.getBalance(player);
            Map<String, String> params = new HashMap<>();
            params.put("needed-amount", String.valueOf(requiredAmount));
            params.put("current-amount", String.valueOf(currentBalance));
            params.put("currency", economy.currencyNamePlural());
            return CostResult.success(params);
        }

        double currentBalance = economy.getBalance(player);

        Map<String, String> params = new HashMap<>();
        params.put("needed-amount", String.valueOf(requiredAmount));
        params.put("current-amount", String.valueOf(currentBalance));
        params.put("currency", economy.currencyNamePlural());

        // We encode "can afford" into success/fail, but still no withdraw
        if (currentBalance < requiredAmount) {
            return CostResult.fail(params);
        }
        return CostResult.success(params);
    }

    /**
     * Parses the "amount" field.
     *
     * - If it's a Number, use its double value.
     * - If it's a String, optionally apply PlaceholderAPI and then parse as double.
     */
    private double parseAmount(Object amountObj, NexusCostContext context) {

        if (amountObj instanceof Number n) {
            return n.doubleValue();
        }

        String raw = String.valueOf(amountObj);

        // Optional PlaceholderAPI support
        Player player = context.player();
        if (player != null) {
            raw = StringUtils.parsePlaceholderAPI(player, raw);
        }

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Invalid 'amount' value for money cost: " + raw,
                    "Expected a numeric value."
            ));
            e.printStackTrace();
            return 0.0;
        }
    }
}