// root/src/main/java/net/sugar27/quests/network/NetworkHandler.java

package net.sugar27.quests.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.quest.QuestDefinition;
import net.sugar27.quests.quest.QuestProgress;

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
        PayloadRegistrar registrar = event.registrar(ShugaQuestsMod.MODID).versioned("1");
        registrar.playToClient(
                Objects.requireNonNull(QuestSyncPacket.TYPE),
                Objects.requireNonNull(QuestSyncPacket.STREAM_CODEC),
                QuestSyncPacket::handle
        );
    }

    // Send a full sync payload to a player.
    public static void sendFullSync(@Nonnull ServerPlayer player, List<QuestDefinition> definitions, List<QuestProgress> progresses, List<String> daily) {
        QuestSyncPacket payload = new QuestSyncPacket(
                QuestSyncPacket.SyncType.FULL,
                definitions,
                progresses,
                daily,
                "",
                QuestSyncPacket.NotificationType.NONE
        );
        PacketDistributor.sendToPlayer(Objects.requireNonNull(player), payload);
    }

    // Send a delta sync payload to a player.
    public static void sendDeltaSync(@Nonnull ServerPlayer player, QuestDefinition definition, QuestProgress progress, QuestSyncPacket.NotificationType notificationType) {
        QuestSyncPacket payload = new QuestSyncPacket(
                QuestSyncPacket.SyncType.DELTA,
                List.of(definition),
                List.of(progress),
                List.of(),
                definition.id(),
                notificationType
        );
        PacketDistributor.sendToPlayer(Objects.requireNonNull(player), payload);
    }
}
