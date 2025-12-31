// root/src/main/java/net/sugar27/quests/quest/QuestCategory.java

package net.sugar27.quests.quest;

// Quest categories used for filtering and UI tabs.
public enum QuestCategory {
    ALL,
    LIFE,
    EXPLORE,
    COMBAT,
    OTHER;

    // Map from JSON text to enum value.
    public static QuestCategory fromString(String value) {
        if (value == null) {
            return OTHER;
        }
        return switch (value.toLowerCase()) {
            case "life" -> LIFE;
            case "explore" -> EXPLORE;
            case "combat" -> COMBAT;
            default -> OTHER;
        };
    }
}
