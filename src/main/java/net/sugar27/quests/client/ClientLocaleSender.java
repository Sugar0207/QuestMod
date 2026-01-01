// root/src/main/java/net/sugar27/quests/client/ClientLocaleSender.java

package net.sugar27.quests.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.sugar27.quests.network.ClientLanguagePacket;

// Sends the client locale to the server once after login.
public final class ClientLocaleSender {
    private static boolean sent = false;

    // Utility class; no instantiation.
    private ClientLocaleSender() {
    }

    // Send the locale after the local player is available.
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            sent = false;
            return;
        }
        if (sent) {
            return;
        }
        String locale = minecraft.getLanguageManager().getSelected();
        ClientPacketDistributor.sendToServer(new ClientLanguagePacket(locale));
        sent = true;
    }
}
