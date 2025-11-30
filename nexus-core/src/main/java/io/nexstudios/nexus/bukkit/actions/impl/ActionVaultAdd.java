package io.nexstudios.nexus.bukkit.actions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.actions.NexusActionContext;
import io.nexstudios.nexus.bukkit.utils.NexusStringMath;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class ActionVaultAdd implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(NexusActionContext context) {
        ActionData data = context.data();
        Player player = context.requirePlayer();

        double amount;
        String expression;

        Object multiplierObj = data.getData().get("multiplier");
        if (multiplierObj == null) {
            expression = "1";
        } else {
            expression = String.valueOf(multiplierObj);
        }

        Object amountObject = data.getData().get("amount");
        if (amountObject == null) {
            NexusPlugin.nexusLogger.warning("Missing 'amount' parameter for drop table action 'vault_add'. " +
                    "Please add it to your drop table action and try again.");
        }

        if (amountObject instanceof String amountString) {
            String[] rangeParts = amountString.split("-");

            if (amountString.contains("-")) {
                try {
                    double minAmount = Integer.parseInt(rangeParts[0].trim());
                    double maxAmount = Integer.parseInt(rangeParts[1].trim());

                    if (minAmount > maxAmount) {
                        NexusPlugin.nexusLogger.warning("Invalid amount-range: " + amountString + ". Swapping min and max.");
                        double temp = minAmount;
                        minAmount = maxAmount;
                        maxAmount = temp;
                    }

                    amount = (minAmount + (Math.random() * ((maxAmount - minAmount) + 1)));

                } catch (NumberFormatException e) {
                    NexusPlugin.nexusLogger.error("Invalid number format in amount-range: " + amountString);
                    e.printStackTrace();
                    amount = 0.0;
                }
            } else {
                try {
                    amount = Double.parseDouble(amountString);
                } catch (NumberFormatException e) {
                    NexusPlugin.nexusLogger.warning("Could not parse " + amountString + " as a number. Falling back to 0.");
                    e.printStackTrace();
                    amount = 0.0;
                }
            }

        } else if (amountObject instanceof Number n) {
            amount = n.doubleValue();
        } else {
            NexusPlugin.nexusLogger.warning("Input " + amountObject + " is not a number or range. Falling back to 1. -> " +
                    (amountObject != null ? amountObject.getClass().getName() : "null"));
            amount = 1;
        }

        double dropMultiplier = amountMultiplier(expression, player);
        amount *= dropMultiplier;

        if (NexusPlugin.getInstance().vaultHook != null) {
            Economy economy = NexusPlugin.getInstance().vaultHook.getEconomy();
            EconomyResponse response = economy.depositPlayer(player, amount);

            if (!response.transactionSuccess()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Could not add money to your account. Reason: <dark_red>" + response.errorMessage
                ));
            }
            String currency = economy.currencyNamePlural();
            NexusPlugin.nexusLogger.info("<white>Added <#ffdc73>" + amount + " <gray>" + currency +
                    " <white>to <#ffdc73>" + player.getName() + "<white>'s account via drop table action.");
        } else {
            NexusPlugin.nexusLogger.warning("You are trying to use Vault actions in drop table, but Vault was not found");
        }
    }

    private int amountMultiplier(String expression, Player player) {

        if (expression == null || expression.isEmpty()) {
            return 1;
        }

        expression = StringUtils.parsePlaceholderAPI(player, expression);

        double result = NexusStringMath.evaluateExpression(expression);

        if (result == 0) {
            return 1;
        }

        return calculateDropMultiplier(result);
    }

    private int calculateDropMultiplier(double result) {
        int baseMultiplier = (int) result;

        double fractionalChance = result - baseMultiplier;

        if (checkChance(fractionalChance * 100)) {
            baseMultiplier++;
        }

        return baseMultiplier;
    }

    private boolean checkChance(double chance) {
        Random random = new java.util.Random();
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException("Chance must be between 0 and 100, inclusive. You gave " + chance);
        }
        return random.nextDouble() * 100 < chance;
    }
}