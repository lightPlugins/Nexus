package io.nexstudios.proxy.velocity.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import io.nexstudios.proxy.velocity.NexVelocity;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Objects;

/**
 * Root command: /nexusproxy
 */
public final class NexusProxyCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final NexVelocity plugin;
    private final List<ProxySubCommand> subCommands;

    public NexusProxyCommand(NexVelocity plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.subCommands = List.of(
                new ReloadSubCommand()
        );
    }

    public BrigadierCommand build() {
        LiteralArgumentBuilder<CommandSource> root = LiteralArgumentBuilder
                .<CommandSource>literal("nexusproxy")
                .executes(ctx -> {
                    ctx.getSource().sendMessage(MM.deserialize("<gray>Usage: <yellow>/nexusproxy reload</yellow>"));
                    return Command.SINGLE_SUCCESS;
                });

        for (ProxySubCommand sub : subCommands) {
            sub.register(root, plugin);
        }

        return new BrigadierCommand(root);
    }
}