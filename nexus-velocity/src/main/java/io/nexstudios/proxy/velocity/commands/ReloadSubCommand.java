package io.nexstudios.proxy.velocity.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import io.nexstudios.proxy.velocity.NexVelocity;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ReloadSubCommand implements ProxySubCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PERM_RELOAD = "nexus.proxy.reload";

    @Override
    public void register(LiteralArgumentBuilder<CommandSource> root, NexVelocity plugin) {
        root.then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                .requires(src -> src.hasPermission(PERM_RELOAD))
                .executes(ctx -> {
                    plugin.onReload();
                    ctx.getSource().sendMessage(MM.deserialize("<green>Reloaded Nexus proxy configuration.</green>"));
                    return Command.SINGLE_SUCCESS;
                })
        );
    }
}