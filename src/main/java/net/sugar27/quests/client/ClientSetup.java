// root/src/main/java/net/sugar27/quests/client/ClientSetup.java

package net.sugar27.quests.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.sugar27.quests.client.gui.QuestHudOverlay;

// Client-only initialization hooks.
public final class ClientSetup {
    // Utility class; no instantiation.
    private ClientSetup() {
    }

    // Register client event listeners and key bindings.
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(KeyBindings::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(KeyBindings::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientLocaleSender::onClientTick);
        NeoForge.EVENT_BUS.addListener(QuestHudOverlay::onRenderGui);
    }
}

