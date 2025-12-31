// root/src/main/java/net/sugar27/quests/quest/QuestCriteriaType.java

package net.sugar27.quests.quest;

// Supported quest criteria types.
public enum QuestCriteriaType {
    ITEM_ACQUIRED,
    ITEM_CRAFTED,
    BLOCK_BROKEN,
    ENTITY_KILLED,
    LOCATION_REACHED,
    CUSTOM_EVENT;

    // Parse a criteria type from JSON text.
    public static QuestCriteriaType fromString(String value) {
        if (value == null) {
            return CUSTOM_EVENT;
        }
        return switch (value.toLowerCase()) {
            case "item_acquired" -> ITEM_ACQUIRED;
            case "item_crafted" -> ITEM_CRAFTED;
            case "block_broken" -> BLOCK_BROKEN;
            case "entity_killed" -> ENTITY_KILLED;
            case "location_reached" -> LOCATION_REACHED;
            case "custom_event" -> CUSTOM_EVENT;
            default -> CUSTOM_EVENT;
        };
    }
}


