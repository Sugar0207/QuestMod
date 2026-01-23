// root/src/main/java/net/sugar27/quests/quest/QuestCriteriaHandlers.java

package net.sugar27.quests.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

// Registry of criteria evaluators for each criteria type.
public final class QuestCriteriaHandlers {
    private static final Map<QuestCriteriaType, QuestCriteriaEvaluator> EVALUATORS = new EnumMap<>(QuestCriteriaType.class);

    static {
        EVALUATORS.put(QuestCriteriaType.ITEM_ACQUIRED, QuestCriteriaHandlers::matchTarget);
        EVALUATORS.put(QuestCriteriaType.ITEM_CRAFTED, QuestCriteriaHandlers::matchTarget);
        EVALUATORS.put(QuestCriteriaType.BLOCK_BROKEN, QuestCriteriaHandlers::matchTarget);
        EVALUATORS.put(QuestCriteriaType.ENTITY_KILLED, QuestCriteriaHandlers::matchTarget);
        EVALUATORS.put(QuestCriteriaType.LOCATION_REACHED, QuestCriteriaHandlers::matchLocation);
        EVALUATORS.put(QuestCriteriaType.CUSTOM_EVENT, QuestCriteriaHandlers::matchCustom);
    }

    // Utility class; no instantiation.
    private QuestCriteriaHandlers() {
    }

    // Evaluate progress increment for a criteria against a context.
    public static int getProgressIncrement(QuestCriteria criteria, QuestEventContext context) {
        QuestCriteriaEvaluator evaluator = EVALUATORS.get(criteria.type());
        if (evaluator == null) {
            return 0;
        }
        return evaluator.getProgressIncrement(criteria, context);
    }

    // Match criteria that use a target id (item, block, entity).
    private static int matchTarget(QuestCriteria criteria, QuestEventContext context) {
        if (!matchesType(criteria, context)) {
            return 0;
        }
        ResourceLocation target = context.targetId();
        if (criteria.item() != null && !criteria.item().equals(target)) {
            return 0;
        }
        if (criteria.block() != null && !criteria.block().equals(target)) {
            return 0;
        }
        if (criteria.entity() != null && !criteria.entity().equals(target)) {
            return 0;
        }
        return Math.max(1, context.count());
    }

    // Match criteria that require a location check.
    private static int matchLocation(QuestCriteria criteria, QuestEventContext context) {
        if (!matchesType(criteria, context)) {
            return 0;
        }
        var level = context.level();
        if (criteria.dimension() != null && level != null) {
            if (!criteria.dimension().equals(level.dimension().location())) {
                return 0;
            }
        }
        if (criteria.biome() != null && level != null) {
            var biomeKey = level.getBiome(BlockPos.containing(context.x(), context.y(), context.z())).unwrapKey();
            if (biomeKey.isEmpty() || !criteria.biome().equals(biomeKey.get().location())) {
                return 0;
            }
        }
        if (criteria.yMin() != null && context.y() < criteria.yMin()) {
            return 0;
        }
        if (criteria.yMax() != null && context.y() > criteria.yMax()) {
            return 0;
        }
        double dx = criteria.x() - context.x();
        double dy = criteria.y() - context.y();
        double dz = criteria.z() - context.z();
        double radius = criteria.radius();
        if (radius <= 0D) {
            return 1;
        }
        return (dx * dx + dy * dy + dz * dz) <= radius * radius ? 1 : 0;
    }

    // Placeholder for custom events in future expansions.
    private static int matchCustom(QuestCriteria criteria, QuestEventContext context) {
        if (!matchesType(criteria, context)) {
            return 0;
        }
        return normalizeCount(context.count());
    }

    private static boolean matchesType(QuestCriteria criteria, QuestEventContext context) {
        return criteria.type() == context.type();
    }

    private static int normalizeCount(int count) {
        return Math.max(1, count);
    }
}


