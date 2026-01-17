// root/src/main/java/net/sugar27/quests/client/ClientLocaleSender.java

package net.sugar27.quests.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.sugar27.quests.network.ClientLanguagePacket;
import net.sugar27.quests.network.QuestSyncRequestPacket;

// Sends the client locale to the server and requests sync when it changes.
public final class ClientLocaleSender {
    private static boolean sent = false;
    private static String lastLocale = "";

    // Utility class; no instantiation.
    private ClientLocaleSender() {
    }

    // Send the locale after the local player is available.
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            sent = false;
            lastLocale = "";
            return;
        }
        String locale = minecraft.getLanguageManager().getSelected();
        if (locale == null) {
            locale = "";
        }
        if (sent && locale.equals(lastLocale)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new ClientLanguagePacket(locale));
        ClientPacketDistributor.sendToServer(new QuestSyncRequestPacket());
        sent = true;
        lastLocale = locale;
    }
}
