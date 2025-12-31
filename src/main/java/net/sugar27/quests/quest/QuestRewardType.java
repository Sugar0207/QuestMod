// root/src/main/java/net/sugar27/quests/quest/QuestRewardType.java

package net.sugar27.quests.quest;

// Supported reward types for quest completion.
public enum QuestRewardType {
    ITEM,
    XP,
    EFFECT,
    COMMAND,
    ADVANCEMENT;

    // Parse a reward type from JSON text.
    public static QuestRewardType fromString(String value) {
        if (value == null) {
            return ITEM;
        }
        return switch (value.toLowerCase()) {
            case "item" -> ITEM;
            case "xp" -> XP;
            case "effect" -> EFFECT;
            case "command" -> COMMAND;
            case "advancement" -> ADVANCEMENT;
            default -> ITEM;
        };
    }
}
