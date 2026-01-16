// root/src/main/java/net/sugar27/quests/quest/QuestCriteria.java

package net.sugar27.quests.quest;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

// Represents a single criteria entry inside an objective.
public record QuestCriteria(
        QuestCriteriaType type,
        ResourceLocation item,
        ResourceLocation block,
        ResourceLocation entity,
        int count,
        ResourceLocation dimension,
        ResourceLocation biome,
        Double yMin,
        Double yMax,
        double x,
        double y,
        double z,
        double radius
) {
    // Parse a criteria entry from JSON.
    public static QuestCriteria fromJson(JsonObject json) {
        QuestCriteriaType type = QuestCriteriaType.fromString(getString(json, "type"));
        ResourceLocation item = getResource(json, "item");
        ResourceLocation block = getResource(json, "block");
        ResourceLocation entity = getResource(json, "entity");
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        ResourceLocation dimension = getResource(json, "dimension");
        ResourceLocation biome = getResource(json, "biome");
        Double yMin = json.has("y_min") ? json.get("y_min").getAsDouble() : null;
        Double yMax = json.has("y_max") ? json.get("y_max").getAsDouble() : null;
        double x = json.has("x") ? json.get("x").getAsDouble() : 0D;
        double y = json.has("y") ? json.get("y").getAsDouble() : 0D;
        double z = json.has("z") ? json.get("z").getAsDouble() : 0D;
        double radius = json.has("radius") ? json.get("radius").getAsDouble() : 0D;
        return new QuestCriteria(type, item, block, entity, count, dimension, biome, yMin, yMax, x, y, z, radius);
    }

    // Serialize this criteria into a network buffer.
    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeEnum(Objects.requireNonNull(type));
        buf.writeBoolean(item != null);
        if (item != null) {
            buf.writeResourceLocation(item);
        }
        buf.writeBoolean(block != null);
        if (block != null) {
            buf.writeResourceLocation(block);
        }
        buf.writeBoolean(entity != null);
        if (entity != null) {
            buf.writeResourceLocation(entity);
        }
        buf.writeVarInt(count);
        buf.writeBoolean(dimension != null);
        if (dimension != null) {
            buf.writeResourceLocation(dimension);
        }
        buf.writeBoolean(biome != null);
        if (biome != null) {
            buf.writeResourceLocation(biome);
        }
        buf.writeBoolean(yMin != null);
        if (yMin != null) {
            buf.writeDouble(yMin);
        }
        buf.writeBoolean(yMax != null);
        if (yMax != null) {
            buf.writeDouble(yMax);
        }
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(radius);
    }

    // Deserialize a criteria entry from a network buffer.
    public static QuestCriteria readFromBuf(FriendlyByteBuf buf) {
        QuestCriteriaType type = buf.readEnum(QuestCriteriaType.class);
        ResourceLocation item = buf.readBoolean() ? buf.readResourceLocation() : null;
        ResourceLocation block = buf.readBoolean() ? buf.readResourceLocation() : null;
        ResourceLocation entity = buf.readBoolean() ? buf.readResourceLocation() : null;
        int count = buf.readVarInt();
        ResourceLocation dimension = buf.readBoolean() ? buf.readResourceLocation() : null;
        ResourceLocation biome = buf.readBoolean() ? buf.readResourceLocation() : null;
        Double yMin = buf.readBoolean() ? buf.readDouble() : null;
        Double yMax = buf.readBoolean() ? buf.readDouble() : null;
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double radius = buf.readDouble();
        return new QuestCriteria(type, item, block, entity, count, dimension, biome, yMin, yMax, x, y, z, radius);
    }

    // Safely read string values from JSON.
    private static String getString(JsonObject json, String key) {
        return json.has(key) ? Objects.requireNonNull(json.get(key).getAsString()) : null;
    }

    // Safely read resource locations from JSON.
    private static ResourceLocation getResource(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        return ResourceLocation.tryParse(Objects.requireNonNull(json.get(key).getAsString()));
    }
}


