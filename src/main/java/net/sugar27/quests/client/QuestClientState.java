// root/src/main/java/net/sugar27/quests/client/QuestClientState.java

package net.sugar27.quests.client;

import net.minecraft.Util;
import net.sugar27.quests.network.QuestSyncPacket;
import net.sugar27.quests.quest.QuestDefinition;
import net.sugar27.quests.quest.QuestProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Holds client-side quest data synced from the server.
public final class QuestClientState {
    private static final Map<String, QuestDefinition> QUEST_DEFINITIONS = new HashMap<>();
    private static final Map<String, QuestProgress> QUEST_PROGRESS = new HashMap<>();
    private static final List<String> DAILY_QUESTS = new ArrayList<>();

    private static String notificationQuestId = "";
    private static QuestSyncPacket.NotificationType notificationType = QuestSyncPacket.NotificationType.NONE;
    private static long notificationExpiresAt = 0L;

    // Utility class; no instantiation.
    private QuestClientState() {
    }

    // Apply a sync payload from the server.
    public static void applySync(QuestSyncPacket packet) {
        if (packet.syncType() == QuestSyncPacket.SyncType.FULL) {
            QUEST_DEFINITIONS.clear();
            QUEST_PROGRESS.clear();
            DAILY_QUESTS.clear();
        }

        for (QuestDefinition definition : packet.questDefinitions()) {
            QUEST_DEFINITIONS.put(definition.id(), definition);
        }
        for (QuestProgress progress : packet.questProgresses()) {
            QUEST_PROGRESS.put(progress.questId(), progress);
        }
        if (packet.syncType() == QuestSyncPacket.SyncType.FULL) {
            DAILY_QUESTS.clear();
            DAILY_QUESTS.addAll(packet.dailyQuestIds());
        } else if (!packet.dailyQuestIds().isEmpty()) {
            DAILY_QUESTS.clear();
            DAILY_QUESTS.addAll(packet.dailyQuestIds());
        }

        if (packet.notificationType() != QuestSyncPacket.NotificationType.NONE) {
            notificationQuestId = packet.notificationQuestId();
            notificationType = packet.notificationType();
            notificationExpiresAt = Util.getMillis() + 5000L;
        }
    }

    // Get an immutable view of quest definitions.
    public static Map<String, QuestDefinition> getQuestDefinitions() {
        return Collections.unmodifiableMap(QUEST_DEFINITIONS);
    }

    // Get an immutable view of quest progress.
    public static Map<String, QuestProgress> getQuestProgress() {
        return Collections.unmodifiableMap(QUEST_PROGRESS);
    }

    // Get the current daily quest ids.
    public static List<String> getDailyQuestIds() {
        return Collections.unmodifiableList(DAILY_QUESTS);
    }

    // Get the current notification, clearing it if expired.
    public static QuestNotification getNotification() {
        if (notificationType == QuestSyncPacket.NotificationType.NONE) {
            return null;
        }
        if (Util.getMillis() > notificationExpiresAt) {
            notificationType = QuestSyncPacket.NotificationType.NONE;
            notificationQuestId = "";
            return null;
        }
        return new QuestNotification(notificationQuestId, notificationType);
    }

    // Notification payload used by the HUD overlay.
    public record QuestNotification(String questId, QuestSyncPacket.NotificationType type) {
    }
}


