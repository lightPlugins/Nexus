package io.lightstudios.nexus.velocity.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;

import java.util.List;

/**
 * A utility class for logging messages to the console with a specific prefix.
 * It supports different log levels: INFO, WARNING, ERROR and CUSTOM.
 * Each log message is formatted with ANSI colors for better visibility in the console.
 */

public class ProxyLogger {

    private final String prefix;

    public ProxyLogger(String prefix) {
        this.prefix = prefix;
    }

    public void info(String message) {
        String suffix = " <reset>[<gray>INFO<reset>] ";
        Component formattedMessage = MiniMessage.miniMessage().deserialize(this.prefix + suffix + message);
        String ansiMessage = ANSIComponentSerializer.ansi().serialize(formattedMessage);
        System.out.println(ansiMessage);
    }

    public void info(List<String> messages) {
        System.out.println(" ");
        messages.forEach(s -> {
            String suffix = " <reset>[<gray>INFO<reset>] ";
            Component formattedMessage = MiniMessage.miniMessage().deserialize(this.prefix + suffix + s);
            String ansiMessage = ANSIComponentSerializer.ansi().serialize(formattedMessage);
            System.out.println(ansiMessage);
        });
        System.out.println(" ");
    }

    public void warning(String message) {
        String suffix = " <reset>[<gold>Warning<reset>] ";
        Component formattedMessage = MiniMessage.miniMessage().deserialize(this.prefix + suffix + message);
        String ansiMessage = ANSIComponentSerializer.ansi().serialize(formattedMessage);
        System.out.println(ansiMessage);
    }

    public void warning(List<String> messages) {
        System.out.println(" ");
        messages.forEach(s -> {
            String suffix = " <reset>[<gold>WARNING<reset>] ";
            Component formattedMessage = MiniMessage.miniMessage().deserialize(this.prefix + suffix + s);
            String ansiMessage = ANSIComponentSerializer.ansi().serialize(formattedMessage);
            System.out.println(ansiMessage);
        });
        System.out.println(" ");
    }

    public void error(String message) {
        String suffix = " <reset>[<red>ERROR<reset>] ";
        Component formattedMessage = MiniMessage.miniMessage().deserialize(this.prefix + suffix + message);
        String ansiMessage = ANSIComponentSerializer.ansi().serialize(formattedMessage);
        System.out.println(ansiMessage);
    }

    public void error(List<String> message) {
        System.out.println(" ");
        message.forEach(s -> {
            String suffix = " <reset>[<red>ERROR<reset>] ";
            Component formattedMessage = MiniMessage.miniMessage().deserialize(this.prefix + suffix + s);
            String ansiMessage = ANSIComponentSerializer.ansi().serialize(formattedMessage);
            System.out.println(ansiMessage);
        });
        System.out.println(" ");
    }

    public void custom(String message) {
        Component formattedMessage = MiniMessage.miniMessage().deserialize(this.prefix + " " + message);
        String ansiMessage = ANSIComponentSerializer.ansi().serialize(formattedMessage);
        System.out.println(ansiMessage);
    }

    public void custom(List<String> message) {
        System.out.println(" ");
        message.forEach(s -> {
            Component formattedMessage = MiniMessage.miniMessage().deserialize(this.prefix + " " + s);
            String ansiMessage = ANSIComponentSerializer.ansi().serialize(formattedMessage);
            System.out.println(ansiMessage);
        });
        System.out.println(" ");
    }
}
