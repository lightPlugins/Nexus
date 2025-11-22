package io.nexstudios.nexus.bukkit.actions.handler;

import com.willfp.ecoskills.stats.Stat;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionData;
import io.nexstudios.nexus.bukkit.actions.NexusAction;
import io.nexstudios.nexus.bukkit.hooks.EcoSkillsHook;
import io.nexstudios.nexus.bukkit.utils.NexusStringMath;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ActionVaultAdd implements NexusAction {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation) {

        StatModifier modifier = new StatModifier("test", "Test Stat", 1.0);
        MMOPlayerData playerData = MMOPlayerData.get(player);
        modifier.register(playerData);

        double amount;
        String expression;

        if(data.getData().get("multiplier") == null) {
            expression = "1";
        } else {
            expression = (String) data.getData().get("multiplier");
        }

        if(data.getData().get("amount") == null) {
            NexusPlugin.nexusLogger.warning("Missing 'amount' parameter for drop table action 'vault_add'. " +
                    "Please add it to your drop table action and try again.'");
        }

        Object amountObject = data.getData().get("amount");

        if(data.validate(data.getData().get("amount"), String.class)) {

            String amountString = (String) amountObject;

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
                    amount = 0.0; // Fallback value
                }
            } else {
                try {
                    amount = Double.parseDouble(amountString); // Einzelwert aus String übernehmen
                } catch (NumberFormatException e) {
                    NexusPlugin.nexusLogger.warning("Could not parse " + amountString + " as an integer. Falling back to 1.");
                    e.printStackTrace();
                    amount = 0.0; // Fallback auf Standardwert
                }
            }

        } else if (data.validate(data.getData().get("amount"), Number.class)) {
            amount = (double) data.getData().get("amount");
        } else {
            NexusPlugin.nexusLogger.warning("Input" + amountObject + "is not an integer or a range. Falling back to 1. -> " + amountObject.getClass().getName());
            amount = 1; // Fallback auf Standardwert
        }

        double dropMultiplier = amountMultiplier(expression, player);

        amount *= dropMultiplier;

        if(NexusPlugin.getInstance().vaultHook != null) {
            Economy economy = NexusPlugin.getInstance().vaultHook.getEconomy();
            EconomyResponse response = economy.depositPlayer(player, amount);

            if(!response.transactionSuccess()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not add money to your account. " +
                        "Reason: <dark_red>" + response.errorMessage));
            }
            String currency = economy.currencyNamePlural();
            NexusPlugin.nexusLogger.info("<white>Added <#ffdc73>" + amount + " <gray>" + currency + " <white>to <#ffdc73>" + player.getName() + "<white>'s account via drop table action.");
        } else {
            NexusPlugin.nexusLogger.warning("You are trying to use Vault actions in drop table, but Vault was not found");
        }
    }

    @Override
    public void execute(Player player, ActionData data, Location targetLocation, Map<String, Object> params) {
        execute(player, data, targetLocation);
    }

    @Override
    public void execute(Player player, ActionData data, Location location, TagResolver tagResolver) {
        execute(player, data, location);
    }

    private int amountMultiplier(String expression, Player player) {

        if (expression == null || expression.isEmpty()) {
            return 1;
        }

        if (expression.contains("ecoskills:")) {
            expression = processEcoSkillsExpression(expression, player);
            if (expression == null) {
                return 1;
            }
        }

        double result = NexusStringMath.evaluateExpression(expression);

        if(result == 0) {
            return 1;
        }

        return calculateDropMultiplier(result);
    }

    private int calculateDropMultiplier(double result) {
        // Der Ganzzahlanteil aus dem Ergebnis (z. B. 2 bei 2.6)
        int baseMultiplier = (int) result;

        // Der Dezimalanteil als Wahrscheinlichkeit (z. B. 0.6 bei 2.6)
        double fractionalChance = result - baseMultiplier;

        // Verwende checkChance, um zu entscheiden, ob der Dezimalanteil eine zusätzliche Multiplikation gibt
        if (checkChance(fractionalChance * 100)) {
            // Drop zusätzlich um 1 erhöhen
            baseMultiplier++;
        }

        return baseMultiplier; // Gesamter Multiplikator
    }

    private boolean checkChance(double chance) {
        Random random = new java.util.Random();
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException("Chance must be between 0 and 100, inclusive. You gave " + chance);
        }
        return random.nextDouble() * 100 < chance;
    }

    private String processEcoSkillsExpression(String expression, Player player) {
        EcoSkillsHook ecoSkillsHook = NexusPlugin.getInstance().getEcoSkillsHook();

        if (ecoSkillsHook == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "You are trying to use ecoskills placeholder",
                    "while EcoSkills is not installed!",
                    "Failed Expression: " + expression
            ));
            return "1 + 0";
        }

        String[] split = expression.split(":");
        if (split.length != 2) {
            NexusPlugin.nexusLogger.error("EcoSkills stat expression is invalid: " + expression);
            return "1 + 0";
        }

        String statName = split[1].split(" ")[0].replace("'", "").trim();
        Stat stat = ecoSkillsHook.getStatByName(statName, player);

        if (stat == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "You are trying to use EcoSkills placeholder",
                    "while the provided stat does not exist!",
                    "Failed stat name: " + statName
            ));
            return null;
        }

        int currentStatLevel = stat.getActualLevel$core_plugin(player);

        // Ersetze den ecoskills-Teil in der ursprünglichen expression durch den aktuellen Level-Wert
        return expression.replace("ecoskills:" + statName, String.valueOf(currentStatLevel));
    }
}
