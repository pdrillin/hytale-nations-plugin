package com.universe.nations.commands.subcommand;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.MultiArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.universe.nations.nation.NationManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class NationCreateCommand extends AbstractAsyncCommand {

    private final NationManager manager;
    private final RequiredArg<String> name;

    public NationCreateCommand(NationManager manager) {
        super("create", "Creates a new nation");
        this.manager = manager;
        this.name = this.withRequiredArg("name", "Nation name", ArgTypes.STRING);
        this.setAllowsExtraArguments(true);
        this.requirePermission("nations.create");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        String args = extractNationName(commandContext);
        sender.sendMessage(Message.raw("Args are: " + args));
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef == null) return;
                    String nationName = commandContext.get(this.name);
                    if (nationName == null || nationName.isBlank()) {
                        player.sendMessage(Message.raw("Usage: /nation create <name>"));
                        return;
                    }

                    try {
                        var nation = manager.createNation(playerRef.getUuid(), nationName);
                        player.sendMessage(Message.raw("§aNation créée: §f" + nation.getName() + " §7(statut: Hameau)"));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Message.raw("§c" + e.getMessage()));
                    }
                }, world);
            } else {
                commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private static String extractNationName(CommandContext ctx) {
        String input = ctx.getInputString();
        if (input == null) return null;

        // On cherche " create " (avec espaces) pour éviter des faux positifs
        String lower = input.toLowerCase();

        int idx = lower.indexOf(" create ");
        if (idx >= 0) {
            return input.substring(idx + " create ".length()).trim();
        }

        // fallback : si jamais c'est "create" en fin de chaîne ou format étrange
        idx = lower.indexOf("create");
        if (idx >= 0) {
            return input.substring(idx + "create".length()).trim();
        }

        return null;
    }
}
