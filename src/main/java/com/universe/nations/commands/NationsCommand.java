package com.universe.nations.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.universe.nations.commands.subcommand.NationCreateCommand;
import com.universe.nations.nation.NationManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class NationsCommand extends AbstractAsyncCommand {
    public NationsCommand(NationManager manager) {
        super("nation", "Nation commands");
        this.addAliases("n");
        this.addSubCommand(new NationCreateCommand(manager));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        sender.sendMessage(Message.raw("Usage: /nation <subcommand>"));
        return CompletableFuture.completedFuture(null);
    }

}
