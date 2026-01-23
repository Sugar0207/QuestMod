// root/src/main/java/net/sugar27/quests/util/QuestJsonUtil.java

package net.sugar27.quests.util;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

// Shared helpers for reading quest JSON fields.
public final class QuestJsonUtil {
    private QuestJsonUtil() {
    }

    // Safely read string values from JSON.
    public static String getString(JsonObject json, String key) {
        return json.has(key) ? Objects.requireNonNull(json.get(key).getAsString()) : null;
    }

    // Safely read resource locations from JSON.
    public static ResourceLocation getResource(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        return ResourceLocation.tryParse(Objects.requireNonNull(json.get(key).getAsString()));
    }
}
