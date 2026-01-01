// root/src/main/java/net/sugar27/quests/network/ClientLanguagePacket.java

package net.sugar27.quests.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.server.lang.PlayerLocaleStore;

import java.util.Objects;

// Client-to-server locale notification payload.
public record ClientLanguagePacket(String locale) implements CustomPacketPayload {
    public static final Type<ClientLanguagePacket> TYPE = new Type<>(
            Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(ShugaQuestsMod.MODID, "client_language"))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientLanguagePacket> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> buf.writeUtf(Objects.requireNonNullElse(payload.locale, "")),
                    buf -> new ClientLanguagePacket(buf.readUtf()));

    // Handle server-side locale registration.
    public static void handle(ClientLanguagePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                PlayerLocaleStore.setLocale(player.getUUID(), payload.locale());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
