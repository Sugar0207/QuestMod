// root/src/main/java/net/sugar27/quests/quest/QuestCriteriaEvaluator.java

package net.sugar27.quests.quest;

// Strategy interface for evaluating criteria types.
public interface QuestCriteriaEvaluator {
    // Return the progress increment for this event context.
    int getProgressIncrement(QuestCriteria criteria, QuestEventContext context);
}


