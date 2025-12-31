// root/src/main/java/net/sugar27/quests/quest/QuestEventContext.java

package net.sugar27.quests.quest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// Context for evaluating criteria against a game event.
public record QuestEventContext(
        ServerPlayer player,
        QuestCriteriaType type,
        ResourceLocation targetId,
        int count,
        ServerLevel level,
        double x,
        double y,
        double z
) {
}


