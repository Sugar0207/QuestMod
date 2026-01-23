// root/src/main/java/net/sugar27/quests/network/NetworkHandler.java

package net.sugar27.quests.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.quest.QuestDefinition;
import net.sugar27.quests.quest.QuestProgress;
import net.sugar27.quests.server.lang.LangManager;
import net.sugar27.quests.server.lang.PlayerLocaleStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

// Registers payloads and provides helper send methods.
public final class NetworkHandler {
    // Utility class; no instantiation.
    private NetworkHandler() {
    }

    // Register client-bound payloads.
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ShugaQuestsMod.MODID).versioned("2");
        registrar.playToClient(
                Objects.requireNonNull(QuestSyncPacket.TYPE),
                Objects.requireNonNull(QuestSyncPacket.STREAM_CODEC),
                QuestSyncPacket::handle
        );
        registrar.playToServer(
                Objects.requireNonNull(QuestSyncRequestPacket.TYPE),
                Objects.requireNonNull(QuestSyncRequestPacket.STREAM_CODEC),
                QuestSyncRequestPacket::handle
        );
        registrar.playToServer(
                Objects.requireNonNull(ClientLanguagePacket.TYPE),
                Objects.requireNonNull(ClientLanguagePacket.STREAM_CODEC),
                ClientLanguagePacket::handle
        );
        registrar.playToServer(
                Objects.requireNonNull(QuestStartPacket.TYPE),
                Objects.requireNonNull(QuestStartPacket.STREAM_CODEC),
                QuestStartPacket::handle
        );
        registrar.playToServer(
                Objects.requireNonNull(QuestStopPacket.TYPE),
                Objects.requireNonNull(QuestStopPacket.STREAM_CODEC),
                QuestStopPacket::handle
        );
    }

    // Send a full sync payload to a player.
    public static void sendFullSync(@Nonnull ServerPlayer player, List<QuestDefinition> definitions, List<QuestProgress> progresses, List<String> daily, String activeQuestId) {
        String locale = PlayerLocaleStore.getLocale(player.getUUID());
        QuestSyncPacket payload = new QuestSyncPacket(
                QuestSyncPacket.SyncType.FULL,
                localizeDefinitions(definitions, locale),
                progresses,
                daily,
                "",
                QuestSyncPacket.NotificationType.NONE,
                activeQuestId
        );
        PacketDistributor.sendToPlayer(Objects.requireNonNull(player), payload);
    }

    // Send a delta sync payload to a player.
    public static void sendDeltaSync(@Nonnull ServerPlayer player, QuestDefinition definition, QuestProgress progress, QuestSyncPacket.NotificationType notificationType, String activeQuestId) {
        String locale = PlayerLocaleStore.getLocale(player.getUUID());
        QuestSyncPacket payload = new QuestSyncPacket(
                QuestSyncPacket.SyncType.DELTA,
                localizeDefinitions(List.of(definition), locale),
                List.of(progress),
                List.of(),
                definition.id(),
                notificationType,
                activeQuestId
        );
        PacketDistributor.sendToPlayer(Objects.requireNonNull(player), payload);
    }

    private static List<QuestDefinition> localizeDefinitions(List<QuestDefinition> definitions, String locale) {
        if (definitions.isEmpty()) {
            return List.of();
        }
        LangManager langManager = LangManager.get();
        List<QuestDefinition> localized = new ArrayList<>(definitions.size());
        for (QuestDefinition definition : definitions) {
            localized.add(new QuestDefinition(
                    definition.id(),
                    langManager.translate(locale, definition.titleKey()),
                    langManager.translate(locale, definition.descriptionKey()),
                    definition.category(),
                    definition.type(),
                    definition.repeatable(),
                    definition.prerequisites(),
                    definition.objectives(),
                    definition.rewards()
            ));
        }
        return localized;
    }
}


