// root/src/main/java/net/sugar27/quests/network/QuestSyncRequestPacket.java

package net.sugar27.quests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.quest.QuestProgressManager;

import java.util.Objects;

// Client-to-server request for a full quest sync.
public record QuestSyncRequestPacket() implements CustomPacketPayload {
    public static final Type<QuestSyncRequestPacket> TYPE = new Type<>(
            Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(ShugaQuestsMod.MODID, "quest_sync_request"))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestSyncRequestPacket> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
            }, buf -> new QuestSyncRequestPacket());

    // Handle server-side sync request.
    public static void handle(QuestSyncRequestPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                ShugaQuestsMod.LOGGER.info("Quest sync request received from {}", player.getName().getString());
                new QuestProgressManager().syncFull(player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
