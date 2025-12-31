// root/src/main/java/net/sugar27/quests/quest/QuestObjective.java

package net.sugar27.quests.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// A quest objective grouping multiple criteria with a logic operator.
public record QuestObjective(
        String id,
        QuestLogicOperator logic,
        List<QuestCriteria> criteria
) {
    // Parse an objective from JSON.
    public static QuestObjective fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "objective";
        QuestLogicOperator logic = json.has("logic")
                ? QuestLogicOperator.valueOf(json.get("logic").getAsString().toUpperCase())
                : QuestLogicOperator.AND;
        List<QuestCriteria> criteria = new ArrayList<>();
        if (json.has("criteria")) {
            JsonArray array = json.getAsJsonArray("criteria");
            array.forEach(entry -> criteria.add(QuestCriteria.fromJson(entry.getAsJsonObject())));
        }
        return new QuestObjective(id, logic, criteria);
    }

    // Serialize this objective into a network buffer.
    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUtf(Objects.requireNonNull(id));
        buf.writeEnum(Objects.requireNonNull(logic));
        buf.writeVarInt(criteria.size());
        for (QuestCriteria entry : criteria) {
            entry.writeToBuf(buf);
        }
    }

    // Deserialize an objective from a network buffer.
    public static QuestObjective readFromBuf(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        QuestLogicOperator logic = buf.readEnum(QuestLogicOperator.class);
        int size = buf.readVarInt();
        List<QuestCriteria> criteria = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            criteria.add(QuestCriteria.readFromBuf(buf));
        }
        return new QuestObjective(id, logic, criteria);
    }
}


