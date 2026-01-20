// root/src/main/java/net/sugar27/quests/quest/QuestProgressManager.java

package net.sugar27.quests.quest;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.sugar27.quests.network.NetworkHandler;
import net.sugar27.quests.network.QuestSyncPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import javax.annotation.Nonnull;

// Manages per-player quest progress persistence and updates.
public final class QuestProgressManager {
    private static final String DATA_NAME = "shuga_quests_progress";

    // Update quest progress based on an event context.
    public void handleEvent(ServerPlayer player, QuestEventContext context) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        QuestManager questManager = QuestManager.get();
        List<QuestDefinition> quests = questManager.getQuestsByCriteriaType(context.type());
        if (quests.isEmpty()) {
            return;
        }

        QuestProgressData data = QuestProgressData.get(server.overworld());
        String activeQuestId = data.getActiveQuestId(player.getUUID());
        if (activeQuestId == null || activeQuestId.isEmpty()) {
            return;
        }
        boolean dirty = false;

        for (QuestDefinition quest : quests) {
            if (!quest.id().equals(activeQuestId)) {
                continue;
            }
            QuestProgress progress = data.getOrCreateProgress(player.getUUID(), quest.id());
            if (progress.isCompleted() && !quest.repeatable()) {
                continue;
            }

            boolean questUpdated = false;
            for (QuestObjective objective : quest.objectives()) {
                QuestProgress.ObjectiveProgress objectiveProgress = progress.getOrCreateObjective(objective.id(), objective.criteria().size());
                if (objectiveProgress.isCompleted()) {
                    continue;
                }
                for (int i = 0; i < objective.criteria().size(); i++) {
                    QuestCriteria criteria = objective.criteria().get(i);
                    int increment = QuestCriteriaHandlers.getProgressIncrement(criteria, context);
                    if (increment > 0) {
                        int current = objectiveProgress.criteriaCounts().get(i);
                        int updated = Math.min(criteria.count(), current + increment);
                        objectiveProgress.criteriaCounts().set(i, updated);
                        questUpdated = true;
                    }
                }

                if (!objectiveProgress.isCompleted() && isObjectiveComplete(objective, objectiveProgress)) {
                    objectiveProgress.markCompleted();
                    questUpdated = true;
                }
            }

            if (questUpdated) {
                dirty = true;
                if (!progress.isCompleted() && isQuestComplete(quest, progress)) {
                    progress.markCompleted();
                    if (quest.id().equals(activeQuestId)) {
                        data.setActiveQuestId(player.getUUID(), "");
                        activeQuestId = "";
                    }
                    if (!progress.rewardsGranted()) {
                        grantRewards(player, quest, progress);
                    }
                    NetworkHandler.sendDeltaSync(player, quest, progress, QuestSyncPacket.NotificationType.COMPLETED, activeQuestId);
                } else {
                    NetworkHandler.sendDeltaSync(player, quest, progress, QuestSyncPacket.NotificationType.UPDATED, activeQuestId);
                }
            }
        }

        if (dirty) {
            data.setDirty();
        }
    }

    // Sync all quest data to the player on login.
    public void syncFull(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        QuestManager questManager = QuestManager.get();
        QuestProgressData data = QuestProgressData.get(server.overworld());
        List<QuestProgress> progressList = new ArrayList<>(data.getPlayerProgress(player.getUUID()).values());
        List<String> daily = DailyQuestManager.get().getDailyQuestIds(server);
        NetworkHandler.sendFullSync(player, questManager.getAll().values().stream().toList(), progressList, daily, data.getActiveQuestId(player.getUUID()));
    }

    // Sync quest data to all connected players.
    public void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncFull(player);
        }
    }

    // Grant rewards to the player when a quest completes.
    private void grantRewards(ServerPlayer player, QuestDefinition quest, QuestProgress progress) {
        for (QuestReward reward : quest.rewards()) {
            reward.apply(player);
        }
        progress.markRewardsGranted();
    }

    // Check if a quest objective is complete based on criteria counts.
    private boolean isObjectiveComplete(QuestObjective objective, QuestProgress.ObjectiveProgress objectiveProgress) {
        if (objective.criteria().isEmpty()) {
            return true;
        }
        if (objective.logic() == QuestLogicOperator.AND) {
            for (int i = 0; i < objective.criteria().size(); i++) {
                if (objectiveProgress.criteriaCounts().get(i) < objective.criteria().get(i).count()) {
                    return false;
                }
            }
            return true;
        }
        for (int i = 0; i < objective.criteria().size(); i++) {
            if (objectiveProgress.criteriaCounts().get(i) >= objective.criteria().get(i).count()) {
                return true;
            }
        }
        return false;
    }

    // Check if all objectives are complete for a quest.
    private boolean isQuestComplete(QuestDefinition quest, QuestProgress progress) {
        for (QuestObjective objective : quest.objectives()) {
            QuestProgress.ObjectiveProgress objectiveProgress = progress.getOrCreateObjective(objective.id(), objective.criteria().size());
            if (!objectiveProgress.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    // Force-complete a quest for a player.
    public void grantQuest(ServerPlayer player, String questId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        QuestDefinition quest = QuestManager.get().getQuest(questId);
        if (quest == null) {
            return;
        }
        QuestProgressData data = QuestProgressData.get(server.overworld());
        QuestProgress progress = data.getOrCreateProgress(player.getUUID(), quest.id());
        for (QuestObjective objective : quest.objectives()) {
            QuestProgress.ObjectiveProgress objectiveProgress = progress.getOrCreateObjective(objective.id(), objective.criteria().size());
            for (int i = 0; i < objective.criteria().size(); i++) {
                objectiveProgress.criteriaCounts().set(i, objective.criteria().get(i).count());
            }
            objectiveProgress.markCompleted();
        }
        if (!progress.isCompleted()) {
            progress.markCompleted();
            if (!progress.rewardsGranted()) {
                grantRewards(player, quest, progress);
            }
        }
        data.setDirty();
        String activeQuestId = data.getActiveQuestId(player.getUUID());
        if (quest.id().equals(activeQuestId)) {
            data.setActiveQuestId(player.getUUID(), "");
            activeQuestId = "";
        }
        NetworkHandler.sendDeltaSync(player, quest, progress, QuestSyncPacket.NotificationType.COMPLETED, activeQuestId);
    }

    // Force-complete all quests for a player.
    public void grantAllQuests(ServerPlayer player) {
        for (QuestDefinition quest : QuestManager.get().getAll().values()) {
            grantQuest(player, quest.id());
        }
    }

    // Reset quest progress for a player.
    public void resetQuest(ServerPlayer player, String questId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        QuestProgressData data = QuestProgressData.get(server.overworld());
        String activeQuestId = data.getActiveQuestId(player.getUUID());
        if (questId == null || questId.isEmpty()) {
            data.getPlayerProgress(player.getUUID()).clear();
            data.setActiveQuestId(player.getUUID(), "");
        } else {
            data.getPlayerProgress(player.getUUID()).remove(questId);
            if (questId.equals(activeQuestId)) {
                data.setActiveQuestId(player.getUUID(), "");
            }
        }
        data.setDirty();
        syncFull(player);
    }

    // Start a quest for a player, enforcing only one active quest.
    public void startQuest(ServerPlayer player, String questId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        QuestDefinition quest = QuestManager.get().getQuest(questId);
        if (quest == null) {
            return;
        }
        QuestProgressData data = QuestProgressData.get(server.overworld());
        String activeQuestId = data.getActiveQuestId(player.getUUID());
        QuestProgress progress = data.getOrCreateProgress(player.getUUID(), quest.id());
        if (progress.isCompleted() && !quest.repeatable()) {
            return;
        }
        boolean switchedQuest = activeQuestId != null && !activeQuestId.isEmpty() && !activeQuestId.equals(quest.id());
        if (switchedQuest) {
            data.getPlayerProgress(player.getUUID()).remove(activeQuestId);
            data.setActiveQuestId(player.getUUID(), "");
        }
        data.setActiveQuestId(player.getUUID(), quest.id());
        data.setDirty();
        if (switchedQuest) {
            syncFull(player);
        } else {
            NetworkHandler.sendDeltaSync(player, quest, progress, QuestSyncPacket.NotificationType.NONE, quest.id());
        }
    }

    /**
     * Stops tracking the given quest for the player and clears any stored progress for it.
     * <p>
     * This will only take effect if the specified quest is currently the player's active quest.
     * When invoked, the quest's progress entry is removed from persistent storage and the
     * player's active quest id is reset.
     * <p>
     * Note: In earlier versions this operation only deactivated tracking and left the stored
     * progress intact. Callers relying on that behavior should use a different mechanism if
     * they need to preserve progress.
     *
     * @param player the player whose quest should be stopped
     * @param questId the id of the quest to stop and clear
     */
    public void stopQuest(ServerPlayer player, String questId) {
        MinecraftServer server = player.getServer();
        if (server == null || questId == null || questId.isEmpty()) {
            return;
        }
        QuestDefinition quest = QuestManager.get().getQuest(questId);
        if (quest == null) {
            return;
        }
        QuestProgressData data = QuestProgressData.get(server.overworld());
        String activeQuestId = data.getActiveQuestId(player.getUUID());
        if (!quest.id().equals(activeQuestId)) {
            return;
        }
        data.getPlayerProgress(player.getUUID()).remove(quest.id());
        data.setActiveQuestId(player.getUUID(), "");
        data.setDirty();
        syncFull(player);
    }

    // Persistent saved data for all player progress.
    public static class QuestProgressData extends SavedData {
        @Nonnull
        public static final Codec<QuestProgressData> CODEC = Objects.requireNonNull(
                Objects.requireNonNull(CompoundTag.CODEC).xmap(QuestProgressData::loadFromTag, QuestProgressData::saveToTag)
        );
        @Nonnull
        public static final SavedDataType<QuestProgressData> TYPE = Objects.requireNonNull(new SavedDataType<>(DATA_NAME, QuestProgressData::new, CODEC));

        private final Map<UUID, Map<String, QuestProgress>> playerProgress = new HashMap<>();
        private final Map<UUID, String> activeQuestIds = new HashMap<>();

        // Create a new progress data entry.
        public QuestProgressData() {
        }

        // Load progress data from NBT.
        public static QuestProgressData loadFromTag(CompoundTag tag) {
            QuestProgressData data = new QuestProgressData();
            ListTag players = tag.getListOrEmpty("players");
            for (int i = 0; i < players.size(); i++) {
                CompoundTag playerTag = players.getCompoundOrEmpty(i);
                String uuidString = playerTag.getStringOr("uuid", "");
                if (uuidString.isEmpty()) {
                    continue;
                }
                UUID uuid = UUID.fromString(uuidString);
                Map<String, QuestProgress> quests = new HashMap<>();
                ListTag questTags = playerTag.getListOrEmpty("quests");
                for (int q = 0; q < questTags.size(); q++) {
                    QuestProgress progress = QuestProgress.loadFromTag(questTags.getCompoundOrEmpty(q));
                    quests.put(progress.questId(), progress);
                }
                data.playerProgress.put(uuid, quests);
                String activeQuestId = playerTag.getStringOr("activeQuestId", "");
                if (!activeQuestId.isEmpty()) {
                    data.activeQuestIds.put(uuid, activeQuestId);
                }
            }
            return data;
        }

        // Save progress data to NBT.
        public CompoundTag saveToTag() {
            CompoundTag tag = new CompoundTag();
            ListTag players = new ListTag();
            for (Map.Entry<UUID, Map<String, QuestProgress>> entry : playerProgress.entrySet()) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putString("uuid", Objects.requireNonNull(entry.getKey().toString()));
                ListTag questTags = new ListTag();
                for (QuestProgress progress : entry.getValue().values()) {
                    questTags.add(progress.saveToTag());
                }
                playerTag.put("quests", questTags);
                String activeQuestId = activeQuestIds.getOrDefault(entry.getKey(), "");
                if (!activeQuestId.isEmpty()) {
                    playerTag.putString("activeQuestId", Objects.requireNonNull(activeQuestId));
                }
                players.add(playerTag);
            }
            tag.put("players", players);
            return tag;
        }

        // Fetch or create the progress data for the server.
        public static QuestProgressData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        // Get or create progress for a player/quest pair.
        public QuestProgress getOrCreateProgress(UUID playerId, String questId) {
            Map<String, QuestProgress> quests = getPlayerProgress(playerId);
            return quests.computeIfAbsent(questId, QuestProgress::new);
        }

        // Get progress map for a player.
        public Map<String, QuestProgress> getPlayerProgress(UUID playerId) {
            return playerProgress.computeIfAbsent(playerId, key -> new HashMap<>());
        }

        public String getActiveQuestId(UUID playerId) {
            return activeQuestIds.getOrDefault(playerId, "");
        }

        public void setActiveQuestId(UUID playerId, String questId) {
            if (questId == null || questId.isEmpty()) {
                activeQuestIds.remove(playerId);
            } else {
                activeQuestIds.put(playerId, questId);
            }
        }
    }
}


