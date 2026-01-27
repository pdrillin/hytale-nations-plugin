package com.universe.nations.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.universe.nations.commands.subcommand.NationCreateCommand;
import com.universe.nations.gui.NationInfoEditGui;
import com.universe.nations.nation.NationInfo;
import com.universe.nations.nation.NationManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class NationsCommand extends AbstractAsyncCommand {
    private final NationManager nationManager;

    public NationsCommand(NationManager manager) {
        super("nation", "Nation commands");
        this.addAliases("n");
        this.addSubCommand(new NationCreateCommand(manager));
        this.nationManager = manager;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (playerRef != null) {
                        NationInfo nation = nationManager.getNationOf(playerRef.getUuid());
                        if (nation == null) {
                            player.getPageManager().openCustomPage(ref, store, new NationInfoEditGui(playerRef, nationManager, nation));
                        }else{
                            player.getPageManager().openCustomPage(ref, store, new NationInfoEditGui(playerRef, nationManager, nation));
                        }

                    }
                }, world);
            } else {
                commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

}
