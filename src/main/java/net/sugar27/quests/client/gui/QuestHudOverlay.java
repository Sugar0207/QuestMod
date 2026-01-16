// root/src/main/java/net/sugar27/quests/client/gui/QuestHudOverlay.java

package net.sugar27.quests.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.sugar27.quests.client.QuestClientState;
import net.sugar27.quests.config.QuestClientConfig;
import net.sugar27.quests.network.QuestSyncPacket;
import net.sugar27.quests.quest.QuestDefinition;

import java.util.Objects;

// Renders lightweight quest update notifications on the HUD.
public final class QuestHudOverlay {
    // Utility class; no instantiation.
    private QuestHudOverlay() {
    }

    // Draw quest notifications after the main HUD renders.
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!QuestClientConfig.hudNotificationsEnabled()) {
            return;
        }
        QuestClientState.QuestNotification notification = QuestClientState.getNotification();
        if (notification == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        QuestDefinition quest = QuestClientState.getQuestDefinitions().get(notification.questId());
        Component questTitle = quest == null
                ? Component.literal(Objects.requireNonNull(notification.questId()))
                : Component.literal(Objects.requireNonNull(quest.titleKey()));

        Component prefix = notification.type() == QuestSyncPacket.NotificationType.COMPLETED
                ? Component.translatable(Objects.requireNonNull("hud.shuga_quests.completed"))
                : Component.translatable(Objects.requireNonNull("hud.shuga_quests.updated"));

        GuiGraphics graphics = event.getGuiGraphics();
        int x = minecraft.getWindow().getGuiScaledWidth() - 10;
        int y = 10;
        graphics.drawString(Objects.requireNonNull(minecraft.font), Objects.requireNonNull(prefix), x - minecraft.font.width(Objects.requireNonNull(prefix)), y, 0xFFFFE080);
        graphics.drawString(Objects.requireNonNull(minecraft.font), Objects.requireNonNull(questTitle), x - minecraft.font.width(Objects.requireNonNull(questTitle)), y + 12, 0xFFFFFFFF);
    }
}


