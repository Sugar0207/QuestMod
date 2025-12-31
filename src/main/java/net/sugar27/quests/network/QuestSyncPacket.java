// root/src/main/java/net/sugar27/quests/network/QuestSyncPacket.java

package net.sugar27.quests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.client.QuestClientState;
import net.sugar27.quests.quest.QuestDefinition;
import net.sugar27.quests.quest.QuestProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Payload for syncing quest definitions and progress to clients.
public record QuestSyncPacket(
        SyncType syncType,
        List<QuestDefinition> questDefinitions,
        List<QuestProgress> questProgresses,
        List<String> dailyQuestIds,
        String notificationQuestId,
        NotificationType notificationType
) implements CustomPacketPayload {
    public static final Type<QuestSyncPacket> TYPE = new Type<>(
            Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(ShugaQuestsMod.MODID, "quest_sync"))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestSyncPacket> STREAM_CODEC = StreamCodec.of(QuestSyncPacket::write, QuestSyncPacket::read);

    // Supported sync flavors.
    public enum SyncType {
        FULL,
        DELTA
    }

    // Types of client notifications.
    public enum NotificationType {
        NONE,
        UPDATED,
        COMPLETED
    }

    // Encode the packet payload.
    private static void write(RegistryFriendlyByteBuf buf, QuestSyncPacket payload) {
        buf.writeEnum(Objects.requireNonNull(payload.syncType));
        buf.writeVarInt(payload.questDefinitions.size());
        for (QuestDefinition definition : payload.questDefinitions) {
            definition.writeToBuf(buf);
        }
        buf.writeVarInt(payload.questProgresses.size());
        for (QuestProgress progress : payload.questProgresses) {
            progress.writeToBuf(buf);
        }
        buf.writeVarInt(payload.dailyQuestIds.size());
        for (String id : payload.dailyQuestIds) {
            buf.writeUtf(Objects.requireNonNull(id));
        }
        buf.writeUtf(Objects.requireNonNull(Objects.requireNonNullElse(payload.notificationQuestId, "")));
        buf.writeEnum(Objects.requireNonNull(payload.notificationType));
    }

    // Decode the packet payload.
    private static QuestSyncPacket read(RegistryFriendlyByteBuf buf) {
        SyncType syncType = buf.readEnum(SyncType.class);
        int questCount = buf.readVarInt();
        List<QuestDefinition> definitions = new ArrayList<>();
        for (int i = 0; i < questCount; i++) {
            definitions.add(QuestDefinition.readFromBuf(buf));
        }
        int progressCount = buf.readVarInt();
        List<QuestProgress> progresses = new ArrayList<>();
        for (int i = 0; i < progressCount; i++) {
            progresses.add(QuestProgress.readFromBuf(buf));
        }
        int dailyCount = buf.readVarInt();
        List<String> daily = new ArrayList<>();
        for (int i = 0; i < dailyCount; i++) {
            daily.add(buf.readUtf());
        }
        String notificationQuestId = buf.readUtf();
        NotificationType notificationType = buf.readEnum(NotificationType.class);
        return new QuestSyncPacket(syncType, definitions, progresses, daily, notificationQuestId, notificationType);
    }

    // Handle client-side sync processing.
    public static void handle(QuestSyncPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> QuestClientState.applySync(payload));
    }

    // Provide the payload type for NeoForge.
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
