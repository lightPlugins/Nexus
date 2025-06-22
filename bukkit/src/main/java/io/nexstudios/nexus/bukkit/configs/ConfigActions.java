package io.nexstudios.nexus.bukkit.configs;

import io.nexstudios.nexus.bukkit.Nexus;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.Subst;

import java.util.List;

public class ConfigActions {

    private final ConfigurationSection actionSection;
    private final String fileName;
    private final Player player;

    /**
     * Constructor for ConfigActions.
     *
     * @param fileName The name of the configuration file, where the actions are defined.
     * @param actionSection The section of the configuration that contains the actions.
     * @param player The player for whom the actions are executed.
     */
    public ConfigActions(String fileName, ConfigurationSection actionSection, Player player) {
        this.actionSection = actionSection;
        this.fileName = fileName;
        this.player = player;
        executeActions();
    }

    private void executeActions() {
        // Read the action configuration from the actionSection
        // This method should be implemented to parse the actionSection
        // and perform the necessary actions based on its contents.

        actionSection.getKeys(false).forEach(key -> {
            String id = actionSection.getString(key + ".id");

            if(id == null) {
                // Handle missing action ID
                Nexus.nexusLogger.error(List.of(
                        "Action ID is missing for key: " + key,
                        "In file: " + fileName,
                        "Please check your action configuration."
                ));
                return;
            }

            ConfigurationSection arguments = actionSection.getConfigurationSection(key + "args");

            if(arguments == null) {
                // Handle missing action section
                Nexus.nexusLogger.error(List.of(
                        "Action arguments are missing for key: " + key,
                        "In file: " + fileName,
                        "Please check your action configuration."
                ));
                return;
            }

            switch (id) {
                case "message": sendMessage(arguments);
                case "title": sendTitle(arguments);
                case "sound": playSound(arguments);
                case "custom-sound": playCustomSound(arguments);
                case "player-command": executePlayerCommand(arguments);
                case "console-command": executeConsoleCommand(arguments);
                case "actionbar": sendActionBar(arguments);
                case "vault": addMoney(arguments);
                default: {
                    Nexus.nexusLogger.error(List.of(
                            "Unknown action ID: " + id,
                            "In file: " + fileName,
                            "Please check if this actions is available."
                    ));
                }
            }
        });
    }

    public void addMoney(ConfigurationSection section) {
        // This method should be implemented to add money to a player
        // based on the provided section.
        // It could involve updating the player's balance in a database or economy plugin.

        if(!section.contains("amount")) {
            Nexus.nexusLogger.error(List.of(
                    "'amount' is missing for action type 'vault'",
                    "In file: " + fileName,
                    "Please check if the action is configured correctly."
            ));
            return;
        }

        double amount = section.getDouble("amount");

        if(amount <= 0) {
            Nexus.nexusLogger.error(List.of(
                    "'amount' must be greater than 0 for action type 'vault'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }

        EconomyResponse response = Nexus.getInstance().getVaultHook().getEconomy().depositPlayer(player, amount);

        if(!response.transactionSuccess()) {
            Nexus.nexusLogger.error(List.of(
                    "Config action for 'vault' failed with following error:",
                    "Failed to add money to player: " + player.getName(),
                    "In file: " + fileName,
                    "Reason: " + response.errorMessage
            ));
        }
    }

    public void sendActionBar(ConfigurationSection section) {
        // This method should be implemented to send an action bar message
        // based on the provided section.
        // It could involve sending a message to a player, console, etc.
        if(!section.contains("message")) {
            Nexus.nexusLogger.error(List.of(
                    "Input is missing for action type 'action-bar'",
                    "In file: " + fileName,
                    "Please check if the action is configured correctly."
            ));
            return;
        }
        String message = section.getString("message");
        if(message == null) {
            Nexus.nexusLogger.error(List.of(
                    "Input is missing for action type 'action-bar'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        player.sendActionBar(Nexus.getInstance().messageSender.stringToComponent(player, message));
    }

    private void sendMessage(ConfigurationSection section) {
        // This method should be implemented to send a message
        // based on the provided messageSection.
        // It could involve sending a message to a player, console, etc.
        if(!section.contains("message")) {
            Nexus.nexusLogger.error(List.of(
                    "Input is missing for action type 'message'",
                    "In file: " + fileName,
                    "Please check if the action is configured correctly."
            ));
            return;
        }
        String message = section.getString("message");
        if(message == null) {
            Nexus.nexusLogger.error(List.of(
                    "Input is missing for action type 'message'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        player.sendMessage(Nexus.getInstance().messageSender.stringToComponent(player, message));

    }

    private void sendTitle(ConfigurationSection section) {
        // This method should be implemented to send a message
        // based on the provided messageSection.
        // It could involve sending a message to a player, console, etc.
        if(!section.contains("message")) {
            Nexus.nexusLogger.error(List.of(
                    "Input is missing for action type 'title'",
                    "In file: " + fileName,
                    "Please check if the action is configured correctly."
            ));
            return;
        }
        String upperTitle = section.getString("upper");
        if(upperTitle == null) {
            Nexus.nexusLogger.error(List.of(
                    "'upper' is missing for action type 'title'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        String lowerTitle = section.getString("lower");
        if(lowerTitle == null) {
            Nexus.nexusLogger.error(List.of(
                    "'lower' is missing for action type 'title'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        // optional values
        int fadeIn = section.getInt("fade-in", 20);
        int stay = section.getInt("stay", 60);
        int fadeOut = section.getInt("fade-out", 20);

        // Create Times
        Title.Times titleTimes = Title.Times.times(
                java.time.Duration.ofMillis(fadeIn * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(fadeOut * 50L)
        );
        // Create Title renderer
        Title title = Title.title(
                Nexus.getInstance().messageSender.stringToComponent(player, upperTitle),
                Nexus.getInstance().messageSender.stringToComponent(player, lowerTitle),
                titleTimes
        );
        // Send the title to the player
        player.showTitle(title);
    }

    private void playSound(ConfigurationSection section) {
        // This method should be implemented to send a title
        // based on the provided titleSection.
        // It could involve sending a title to a player, console, etc.

        @Subst(".") String soundKey = section.getString("sound");
        if(soundKey == null) {
            Nexus.nexusLogger.error(List.of(
                    "Sound key is missing for action type 'sound'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }

        NamespacedKey key = NamespacedKey.fromString(soundKey);
        if(key == null) {
            Nexus.nexusLogger.error(List.of(
                    "Invalid sound key: " + soundKey,
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        if(Registry.SOUNDS.get(key) == null) {
            Nexus.nexusLogger.error(List.of(
                    "Sound not found: " + soundKey,
                    "In file: " + fileName,
                    "Please check if the sound exists in the server's resources."
            ));
            return;
        }


        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);
        Sound sound = Sound.sound(Key.key(soundKey), Sound.Source.MUSIC, volume, pitch);
        // Play the sound for the player (self emitter)
        // The sound will follow the player's position
        player.playSound(sound, Sound.Emitter.self());
    }

    private void playCustomSound(ConfigurationSection section) {
        // This method should be implemented to send a title
        // based on the provided titleSection.
        // It could involve sending a title to a player, console, etc.

        @Subst(".") String soundNamespace = section.getString("namespace");
        @Subst(".") String soundKey = section.getString("key");

        if (soundKey == null) {
            Nexus.nexusLogger.error(List.of(
                    "Sound key is missing for action type 'custom_sound'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }

        if (soundNamespace == null) {
            Nexus.nexusLogger.error(List.of(
                    "Sound namespace is missing for action type 'custom_sound'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }

        NamespacedKey key = NamespacedKey.fromString(soundNamespace + ":" + soundKey);
        if (key == null) {
            Nexus.nexusLogger.error(List.of(
                    "Invalid sound key: " + soundNamespace + ":" + soundKey,
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        if (Registry.SOUNDS.get(key) == null) {
            Nexus.nexusLogger.error(List.of(
                    "Sound not found: " + soundNamespace + ":" + soundKey,
                    "In file: " + fileName,
                    "Please check if the sound exists in the server's resources."
            ));
            return;
        }
        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);
        Sound sound = Sound.sound(Key.key(soundNamespace, soundKey), Sound.Source.MUSIC, volume, pitch);
        // Play the sound for the player (self emitter)
        // The sound will follow the player's position
        player.playSound(sound, Sound.Emitter.self());
    }

    public void executePlayerCommand(ConfigurationSection section) {
        // This method should be implemented to execute a command
        // based on the provided commandSection.
        // It could involve executing a command as the player or console.
        if(!section.contains("command")) {
            Nexus.nexusLogger.error(List.of(
                    "Command is missing for action type 'command'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        String command = section.getString("command");
        if(command == null) {
            Nexus.nexusLogger.error(List.of(
                    "Command is missing for action type 'command'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        TagResolver resolver = Placeholder.unparsed("name", player.getName());
        Component componentCommand = Nexus.getInstance().getMessageSender().stringToComponent(player, command, resolver);
        String stringCommand = PlainTextComponentSerializer.plainText().serialize(componentCommand);
        Nexus.nexusLogger.debug("Executing command: " + stringCommand + " for player " + player.getName(), 1);
        player.performCommand(stringCommand);
    }

    public void executeConsoleCommand(ConfigurationSection section) {
        // This method should be implemented to execute a command
        // based on the provided commandSection.
        // It could involve executing a command as the player or console.
        if(!section.contains("command")) {
            Nexus.nexusLogger.error(List.of(
                    "Command is missing for action type 'command'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }
        String command = section.getString("command");
        if(command == null) {
            Nexus.nexusLogger.error(List.of(
                    "Command is missing for action type 'command'",
                    "In file: " + fileName,
                    "Please check your action configuration."
            ));
            return;
        }

        Component componentCommand = Nexus.getInstance().getMessageSender().stringToComponent(player, command);
        String stringCommand = PlainTextComponentSerializer.plainText().serialize(componentCommand);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stringCommand);
    }
}
