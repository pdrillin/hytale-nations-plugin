package com.universe.nations.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.universe.nations.nation.NationInfo;
import com.universe.nations.nation.NationLevel;
import com.universe.nations.nation.NationManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class NationInfoEditGui extends InteractiveCustomUIPage<NationInfoEditGui.NationInfoData> {

    private final NationManager nationManager;
    private final NationInfo nation;
    private final boolean creating;
    private String name;
    private String description;

    public NationInfoEditGui(@NonNullDecl PlayerRef playerRef, NationManager nationManager, NationInfo nation) {
        super(playerRef, CustomPageLifetime.CanDismiss, NationInfoData.CODEC);
        this.nationManager = nationManager;
        this.nation = nation; // peut être null
        this.creating = (nation == null);
        this.name = creating ? "" : nation.getName();
        this.description = creating ? "" : nation.getDescription(); // nécessite NationInfo.getDescription()
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref,
                                @NonNullDecl Store<EntityStore> store,
                                @NonNullDecl NationInfoData data) {
        super.handleDataEvent(ref, store, data);

        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        if (pr == null || player == null) {
            this.sendUpdate();
            return;
        }

        boolean canModify = creating || nation.getOwnerUuid().equals(pr.getUuid());

        // Si pas le chef -> on force juste un refresh sans accepter les changements
        if (!canModify) {
            UICommandBuilder cb = new UICommandBuilder();
            UIEventBuilder eb = new UIEventBuilder();
            this.build(ref, cb, eb, store);
            this.sendUpdate(cb, eb, true);
            return;
        }

        if (data.name != null) {
            this.name = data.name;
        }
        if (data.description != null) {
            this.description = data.description;
        }

        if (data.save != null) {
            String cleanedDesc = (this.description == null ? "" : this.description).replace("\n", "");

            try {
                if (creating) {
                    NationInfo created = nationManager.createNation(
                            pr.getUuid(),
                            this.name,
                            cleanedDesc
                    );

                    // ouvrir l’UI en mode édition
                    player.getPageManager().openCustomPage(
                            ref, store,
                            new NationInfoEditGui(pr, nationManager, created)
                    );
                    return;
                } else {
                    nationManager.updateNationInfo(
                            pr.getUuid(),
                            nation.getId(),
                            this.name,
                            cleanedDesc
                    );
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage(Message.raw(e.getMessage()));
            }
        }

        if (data.cancel != null) {
            this.close();
            return;
        }

        this.sendUpdate();
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref,
                      @NonNullDecl UICommandBuilder uiCommandBuilder,
                      @NonNullDecl UIEventBuilder uiEventBuilder,
                      @NonNullDecl Store<EntityStore> store) {

        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (pr == null) return;

        boolean canModify = creating || nation.getOwnerUuid().equals(pr.getUuid());


        uiCommandBuilder.append("Pages/Universe_Nations_EditNation.ui");
        uiCommandBuilder.set("#TitleText.Text", creating ? "Create your Nation" : "Nation Informations");

        // Champs texte
        uiCommandBuilder.set("#NationInfo #NationNameField.Value", name);
        uiCommandBuilder.set("#NationInfo #NationNameField.IsReadOnly", !canModify);
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#NationNameField",
                EventData.of(NationInfoData.KEY_NAME, "#NationNameField.Value"),
                false
        );

        uiCommandBuilder.set("#NationInfo #NationDescriptionField.Value", description);
        uiCommandBuilder.set("#NationInfo #NationDescriptionField.IsReadOnly", !canModify);
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#NationDescriptionField",
                EventData.of(NationInfoData.KEY_DESCRIPTION, "#NationDescriptionField.Value"),
                false
        );

        // Status
        uiCommandBuilder.set(
                "#NationStatusInfo #NationStatusText.Text",
                creating ? NationLevel.HAMLET.name()
                        : NationLevel.fromId(nation.getLevel()).name()
        );

        // Claims placeholder (tu mettras les vraies valeurs plus tard)
        uiCommandBuilder.set("#ClaimedInfo #ClaimsUsed.Text", "0");
        uiCommandBuilder.set("#ClaimedInfo #ClaimsMax.Text", "1");

        // Boutons
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveButton",
                EventData.of(NationInfoData.KEY_SAVE, "true"),
                false
        );
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelButton",
                EventData.of(NationInfoData.KEY_CANCEL, "true"),
                false
        );

        uiCommandBuilder.set("#ClaimedInfo.Visible", !creating);
        uiCommandBuilder.set("#SaveButton.Text", creating ? "Create" : "Save");
        uiCommandBuilder.set("#SaveButton.Disabled", !canModify);
    }

    public static class NationInfoData {
        static final String KEY_NAME = "@Name";
        static final String KEY_DESCRIPTION = "@Description";
        static final String KEY_SAVE = "Save";
        static final String KEY_CANCEL = "Cancel";

        public static final BuilderCodec<NationInfoData> CODEC =
                BuilderCodec.<NationInfoData>builder(NationInfoData.class, NationInfoData::new)
                        .addField(new KeyedCodec<>(KEY_NAME, Codec.STRING), (d, s) -> d.name = s, d -> d.name)
                        .addField(new KeyedCodec<>(KEY_DESCRIPTION, Codec.STRING), (d, s) -> d.description = s, d -> d.description)
                        .addField(new KeyedCodec<>(KEY_SAVE, Codec.STRING), (d, s) -> d.save = s, d -> d.save)
                        .addField(new KeyedCodec<>(KEY_CANCEL, Codec.STRING), (d, s) -> d.cancel = s, d -> d.cancel)
                        .build();

        private String name;
        private String description;
        private String save;
        private String cancel;
    }
}
