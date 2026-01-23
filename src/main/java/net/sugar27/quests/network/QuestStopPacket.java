// root/src/main/java/net/sugar27/quests/network/QuestStopPacket.java

package net.sugar27.quests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.quest.QuestProgressManager;

import java.util.Objects;

// Client-to-server request to stop a quest.
public record QuestStopPacket(String questId) implements CustomPacketPayload {
    public static final Type<QuestStopPacket> TYPE = new Type<>(
            Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(ShugaQuestsMod.MODID, "quest_stop"))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestStopPacket> STREAM_CODEC =
            StreamCodec.of(QuestStopPacket::write, QuestStopPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, QuestStopPacket payload) {
        QuestPacketUtil.writeQuestId(buf, payload.questId);
    }

    private static QuestStopPacket read(RegistryFriendlyByteBuf buf) {
        return new QuestStopPacket(QuestPacketUtil.readQuestId(buf));
    }

    public static void handle(QuestStopPacket payload, IPayloadContext context) {
        QuestPacketUtil.withServerPlayer(context, player -> new QuestProgressManager().stopQuest(player, payload.questId()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
