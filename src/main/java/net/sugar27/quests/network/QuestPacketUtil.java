// root/src/main/java/net/sugar27/quests/network/QuestPacketUtil.java

package net.sugar27.quests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;
import java.util.function.Consumer;

// Shared helpers for quest network packets.
final class QuestPacketUtil {
    private QuestPacketUtil() {
    }

    static void writeQuestId(RegistryFriendlyByteBuf buf, String questId) {
        buf.writeUtf(Objects.requireNonNullElse(questId, ""));
    }

    static String readQuestId(RegistryFriendlyByteBuf buf) {
        return buf.readUtf();
    }

    static void withServerPlayer(IPayloadContext context, Consumer<net.minecraft.server.level.ServerPlayer> action) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                action.accept(player);
            }
        });
    }
}
