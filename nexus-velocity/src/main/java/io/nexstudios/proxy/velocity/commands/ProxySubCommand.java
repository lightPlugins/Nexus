package io.nexstudios.proxy.velocity.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import io.nexstudios.proxy.velocity.NexVelocity;

/**
 * Registers a subcommand under the root command.
 */
public interface ProxySubCommand {

    void register(LiteralArgumentBuilder<CommandSource> root, NexVelocity plugin);
}