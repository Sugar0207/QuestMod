// root/src/main/java/net/sugar27/quests/network/QuestStartPacket.java

package net.sugar27.quests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.quest.QuestProgressManager;

import java.util.Objects;

// Client-to-server request to start a quest.
public record QuestStartPacket(String questId) implements CustomPacketPayload {
    public static final Type<QuestStartPacket> TYPE = new Type<>(
            Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(ShugaQuestsMod.MODID, "quest_start"))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestStartPacket> STREAM_CODEC =
            StreamCodec.of(QuestStartPacket::write, QuestStartPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, QuestStartPacket payload) {
        QuestPacketUtil.writeQuestId(buf, payload.questId);
    }

    private static QuestStartPacket read(RegistryFriendlyByteBuf buf) {
        return new QuestStartPacket(QuestPacketUtil.readQuestId(buf));
    }

    public static void handle(QuestStartPacket payload, IPayloadContext context) {
        QuestPacketUtil.withServerPlayer(context, player -> new QuestProgressManager().startQuest(player, payload.questId()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
