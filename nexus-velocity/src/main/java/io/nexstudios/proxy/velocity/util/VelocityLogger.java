package io.nexstudios.proxy.velocity.util;

import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class VelocityLogger {

    private final Audience console;
    private final String prefix;
    private boolean debugEnabled;
    private int debugLevel;
    private final String themeColor;

    private final MiniMessage miniMessage;

    public VelocityLogger(ProxyServer proxyServer, String prefix, boolean debugEnabled, int debugLevel, String themeColor) {
        Objects.requireNonNull(proxyServer, "proxyServer");
        this.console = proxyServer.getConsoleCommandSource();
        this.prefix = (prefix == null) ? "" : prefix;
        this.debugEnabled = debugEnabled;
        this.debugLevel = debugLevel;
        this.themeColor = (themeColor == null) ? "" : themeColor;
        this.miniMessage = MiniMessage.miniMessage();
    }

    private void sendRaw(String miniMessageText) {
        String msg = (miniMessageText == null) ? "" : miniMessageText;
        Component component = miniMessage.deserialize(prefix + " " + msg);
        console.sendMessage(component);
    }

    private void emptyLine() {
        console.sendMessage(Component.text(" "));
    }

    public void info(String message) {
        sendRaw("<gray>[<white>INFO<gray>] <reset>" + (message == null ? "" : message));
    }

    public void info(List<String> messages) {
        emptyLine();
        if (messages != null) {
            for (String message : messages) {
                info(message);
            }
        }
        emptyLine();
    }

    public void warning(String message) {
        sendRaw("<yellow>[WARNING] <reset>" + (message == null ? "" : message));
    }

    public void warning(List<String> messages) {
        emptyLine();
        sendRaw("<gold># <yellow>############## <dark_red>WARNING <yellow>############## <gold>#");
        emptyLine();
        if (messages != null) {
            for (String message : messages) {
                sendRaw("<yellow>[<gold>WARNING<yellow>] <reset>" + (message == null ? "" : message));
            }
        }
        emptyLine();
        sendRaw("<gold># <yellow>################################### <gold>#");
        emptyLine();
    }

    public void error(String message) {
        sendRaw("<red>[ERROR] <reset>" + (message == null ? "" : message));
    }

    public void error(List<String> messages) {
        emptyLine();
        sendRaw("<dark_red># <red>############## <dark_red>ERROR <red>############## <dark_red>#");
        emptyLine();
        if (messages != null) {
            for (String message : messages) {
                sendRaw("<red>[<dark_red>ERROR<red>] <reset>" + (message == null ? "" : message));
            }
        }
        emptyLine();
        sendRaw("<dark_red># <red>################################### <dark_red>#");
        emptyLine();
    }

    public void custom(String message) {
        sendRaw(message == null ? "" : message);
    }

    public void custom(List<String> messages) {
        if (messages == null) return;
        for (String message : messages) {
            custom(message);
        }
    }

    public void debug(String message, int debugLevel) {
        if (!debugEnabled || debugLevel > this.debugLevel) return;
        sendRaw("<blue>[<aqua>DEBUG<blue>] <reset>" + (message == null ? "" : message));
    }

    public void debug(List<String> messages, int debugLevel) {
        if (!debugEnabled || debugLevel > this.debugLevel) return;
        if (messages == null) return;
        for (String message : messages) {
            debug(message, debugLevel);
        }
    }
}