// root/src/main/java/net/sugar27/quests/quest/QuestDefinition.java

package net.sugar27.quests.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Defines a quest loaded from JSON.
public record QuestDefinition(
        String id,
        String titleKey,
        String descriptionKey,
        QuestCategory category,
        String type,
        boolean repeatable,
        List<QuestObjective> objectives,
        List<QuestReward> rewards
) {
    // Parse a quest definition from JSON.
    public static QuestDefinition fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String titleKey = json.get("title_key").getAsString();
        String descriptionKey = json.get("description_key").getAsString();
        QuestCategory category = QuestCategory.fromString(
                json.has("category") ? Objects.requireNonNull(json.get("category").getAsString()) : null
        );
        String type = json.has("type") ? Objects.requireNonNull(json.get("type").getAsString()) : "normal";
        boolean repeatable = json.has("repeatable") && json.get("repeatable").getAsBoolean();

        List<QuestObjective> objectives = new ArrayList<>();
        if (json.has("objectives")) {
            JsonArray array = json.getAsJsonArray("objectives");
            array.forEach(entry -> objectives.add(QuestObjective.fromJson(entry.getAsJsonObject())));
        }

        List<QuestReward> rewards = new ArrayList<>();
        if (json.has("rewards")) {
            JsonArray array = json.getAsJsonArray("rewards");
            array.forEach(entry -> rewards.add(QuestReward.fromJson(entry.getAsJsonObject())));
        }

        return new QuestDefinition(id, titleKey, descriptionKey, category, type, repeatable, objectives, rewards);
    }

    // Serialize this definition into a network buffer.
    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUtf(Objects.requireNonNull(id));
        buf.writeUtf(Objects.requireNonNull(titleKey));
        buf.writeUtf(Objects.requireNonNull(descriptionKey));
        buf.writeEnum(Objects.requireNonNull(category));
        buf.writeUtf(Objects.requireNonNull(Objects.requireNonNullElse(type, "")));
        buf.writeBoolean(repeatable);
        buf.writeVarInt(objectives.size());
        for (QuestObjective objective : objectives) {
            objective.writeToBuf(buf);
        }
        buf.writeVarInt(rewards.size());
        for (QuestReward reward : rewards) {
            reward.writeToBuf(buf);
        }
    }

    // Deserialize a quest definition from a network buffer.
    public static QuestDefinition readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String titleKey = buf.readUtf();
        String descriptionKey = buf.readUtf();
        QuestCategory category = buf.readEnum(QuestCategory.class);
        String type = buf.readUtf();
        boolean repeatable = buf.readBoolean();
        int objectiveSize = buf.readVarInt();
        List<QuestObjective> objectives = new ArrayList<>();
        for (int i = 0; i < objectiveSize; i++) {
            objectives.add(QuestObjective.readFromBuf(buf));
        }
        int rewardSize = buf.readVarInt();
        List<QuestReward> rewards = new ArrayList<>();
        for (int i = 0; i < rewardSize; i++) {
            rewards.add(QuestReward.readFromBuf(buf));
        }
        return new QuestDefinition(id, titleKey, descriptionKey, category, type, repeatable, objectives, rewards);
    }
}
