// root/src/main/java/net/sugar27/quests/quest/QuestProgress.java

package net.sugar27.quests.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Tracks a single player's progress for a single quest.
public class QuestProgress {
    private final String questId;
    private final Map<String, ObjectiveProgress> objectives = new HashMap<>();
    private boolean completed;
    private boolean rewardsGranted;
    private long completedAt;

    // Create a new progress entry for a quest.
    public QuestProgress(String questId) {
        this.questId = questId;
    }

    // Get the quest id associated with this progress entry.
    public String questId() {
        return questId;
    }

    // Check if the quest is completed.
    public boolean isCompleted() {
        return completed;
    }

    // Mark the quest as completed.
    public void markCompleted() {
        this.completed = true;
        this.completedAt = System.currentTimeMillis();
    }

    // Check if rewards were already granted.
    public boolean rewardsGranted() {
        return rewardsGranted;
    }

    // Mark rewards as granted.
    public void markRewardsGranted() {
        this.rewardsGranted = true;
    }

    // Get or create progress for a specific objective.
    public ObjectiveProgress getOrCreateObjective(String objectiveId, int criteriaCount) {
        return objectives.computeIfAbsent(objectiveId, id -> new ObjectiveProgress(id, criteriaCount));
    }

    // Get objective progress for lookup.
    public Map<String, ObjectiveProgress> objectives() {
        return objectives;
    }

    // Serialize this progress to NBT for saving.
    public CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("questId", Objects.requireNonNull(questId));
        tag.putBoolean("completed", completed);
        tag.putBoolean("rewardsGranted", rewardsGranted);
        tag.putLong("completedAt", completedAt);

        ListTag objectiveTags = new ListTag();
        for (ObjectiveProgress progress : objectives.values()) {
            objectiveTags.add(progress.saveToTag());
        }
        tag.put("objectives", objectiveTags);
        return tag;
    }

    // Deserialize quest progress from NBT.
    public static QuestProgress loadFromTag(CompoundTag tag) {
        QuestProgress progress = new QuestProgress(tag.getStringOr("questId", ""));
        progress.completed = tag.getBooleanOr("completed", false);
        progress.rewardsGranted = tag.getBooleanOr("rewardsGranted", false);
        progress.completedAt = tag.getLongOr("completedAt", 0L);

        ListTag objectivesTag = tag.getListOrEmpty("objectives");
        for (int i = 0; i < objectivesTag.size(); i++) {
            CompoundTag objectiveTag = objectivesTag.getCompoundOrEmpty(i);
            ObjectiveProgress objectiveProgress = ObjectiveProgress.loadFromTag(objectiveTag);
            progress.objectives.put(objectiveProgress.objectiveId(), objectiveProgress);
        }
        return progress;
    }

    // Serialize this progress for networking.
    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUtf(Objects.requireNonNull(questId));
        buf.writeBoolean(completed);
        buf.writeBoolean(rewardsGranted);
        buf.writeLong(completedAt);
        buf.writeVarInt(objectives.size());
        for (ObjectiveProgress objective : objectives.values()) {
            objective.writeToBuf(buf);
        }
    }

    // Deserialize progress from a network buffer.
    public static QuestProgress readFromBuf(FriendlyByteBuf buf) {
        QuestProgress progress = new QuestProgress(buf.readUtf());
        progress.completed = buf.readBoolean();
        progress.rewardsGranted = buf.readBoolean();
        progress.completedAt = buf.readLong();
        int objectiveCount = buf.readVarInt();
        for (int i = 0; i < objectiveCount; i++) {
            ObjectiveProgress objective = ObjectiveProgress.readFromBuf(buf);
            progress.objectives.put(objective.objectiveId(), objective);
        }
        return progress;
    }

    // Tracks progress for a single objective.
    public static class ObjectiveProgress {
        private final String objectiveId;
        private final List<Integer> criteriaCounts = new ArrayList<>();
        private boolean completed;

        // Create a new objective progress.
        public ObjectiveProgress(String objectiveId, int criteriaCount) {
            this.objectiveId = objectiveId;
            for (int i = 0; i < criteriaCount; i++) {
                criteriaCounts.add(0);
            }
        }

        // Get objective id.
        public String objectiveId() {
            return objectiveId;
        }

        // Get criteria counts.
        public List<Integer> criteriaCounts() {
            return criteriaCounts;
        }

        // Check if objective is completed.
        public boolean isCompleted() {
            return completed;
        }

        // Mark objective as completed.
        public void markCompleted() {
            this.completed = true;
        }

        // Serialize objective progress to NBT.
        public CompoundTag saveToTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("objectiveId", Objects.requireNonNull(objectiveId));
            tag.putBoolean("completed", completed);
            ListTag counts = new ListTag();
            for (Integer count : criteriaCounts) {
                CompoundTag countTag = new CompoundTag();
                countTag.putInt("count", count);
                counts.add(countTag);
            }
            tag.put("counts", counts);
            return tag;
        }

        // Deserialize objective progress from NBT.
        public static ObjectiveProgress loadFromTag(CompoundTag tag) {
            ListTag countsTag = tag.getListOrEmpty("counts");
            ObjectiveProgress progress = new ObjectiveProgress(tag.getStringOr("objectiveId", ""), countsTag.size());
            progress.completed = tag.getBooleanOr("completed", false);
            for (int i = 0; i < countsTag.size(); i++) {
                CompoundTag countTag = countsTag.getCompoundOrEmpty(i);
                progress.criteriaCounts.set(i, countTag.getIntOr("count", 0));
            }
            return progress;
        }

        // Serialize objective progress for networking.
        public void writeToBuf(FriendlyByteBuf buf) {
            buf.writeUtf(Objects.requireNonNull(objectiveId));
            buf.writeBoolean(completed);
            buf.writeVarInt(criteriaCounts.size());
            for (Integer count : criteriaCounts) {
                buf.writeVarInt(count);
            }
        }

        // Deserialize objective progress from a network buffer.
        public static ObjectiveProgress readFromBuf(FriendlyByteBuf buf) {
            String id = buf.readUtf();
            boolean completed = buf.readBoolean();
            int size = buf.readVarInt();
            ObjectiveProgress progress = new ObjectiveProgress(id, size);
            progress.completed = completed;
            for (int i = 0; i < size; i++) {
                progress.criteriaCounts.set(i, buf.readVarInt());
            }
            return progress;
        }
    }
}


