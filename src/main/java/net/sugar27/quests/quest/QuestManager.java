// root/src/main/java/net/sugar27/quests/quest/QuestManager.java

package net.sugar27.quests.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.config.QuestConfigPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Loads and stores quest definitions from JSON files.
public final class QuestManager {
    private static final QuestManager INSTANCE = new QuestManager();
    private final Map<String, QuestDefinition> quests = new HashMap<>();
    private final Map<QuestCriteriaType, List<QuestDefinition>> criteriaIndex = new EnumMap<>(QuestCriteriaType.class);

    // Utility singleton; use get().
    private QuestManager() {
    }

    // Get the singleton instance.
    public static QuestManager get() {
        return INSTANCE;
    }

    // Load all quest definitions from config.
    public void loadAll() {
        quests.clear();
        criteriaIndex.clear();
        try {
            Files.createDirectories(QuestConfigPaths.getQuestsDir());
            Files.createDirectories(QuestConfigPaths.getDailyDir());
        } catch (IOException ex) {
            ShugaQuestsMod.LOGGER.error("Failed to create quest config directory", ex);
            return;
        }

        loadQuestDefinitions(QuestConfigPaths.getQuestsDir(), false);
        loadQuestDefinitions(QuestConfigPaths.getDailyDir(), true);
        rebuildCriteriaIndex();
        ShugaQuestsMod.LOGGER.info("Loaded {} quests", quests.size());
    }

    // Get a quest definition by id.
    public QuestDefinition getQuest(String id) {
        return quests.get(id);
    }

    // Get all quest definitions.
    public Map<String, QuestDefinition> getAll() {
        return Collections.unmodifiableMap(quests);
    }

    // Get quests indexed by criteria type.
    public List<QuestDefinition> getQuestsByCriteriaType(QuestCriteriaType type) {
        return criteriaIndex.getOrDefault(type, List.of());
    }

    // Load quest JSON files from the given directory.
    private void loadQuestDefinitions(Path dir, boolean forceDailyType) {
        try (var paths = Files.list(dir)) {
            paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    QuestDefinition quest = QuestDefinition.fromJson(json);
                    if (forceDailyType) {
                        quest = new QuestDefinition(
                                quest.id(),
                                quest.titleKey(),
                                quest.descriptionKey(),
                                quest.category(),
                                "daily",
                                quest.repeatable(),
                                quest.objectives(),
                                quest.rewards()
                        );
                    }
                    quests.put(quest.id(), quest);
                } catch (Exception ex) {
                    ShugaQuestsMod.LOGGER.error("Failed to load quest file: {}", path, ex);
                }
            });
        } catch (IOException ex) {
            ShugaQuestsMod.LOGGER.error("Failed to list quest config directory", ex);
        }
    }

    // Build the criteria index for fast lookup.
    private void rebuildCriteriaIndex() {
        for (QuestCriteriaType type : QuestCriteriaType.values()) {
            criteriaIndex.put(type, new ArrayList<>());
        }
        for (QuestDefinition quest : quests.values()) {
            var usedTypes = new java.util.HashSet<QuestCriteriaType>();
            for (QuestObjective objective : quest.objectives()) {
                for (QuestCriteria criteria : objective.criteria()) {
                    if (usedTypes.add(criteria.type())) {
                        criteriaIndex.get(criteria.type()).add(quest);
                    }
                }
            }
        }
    }
}


