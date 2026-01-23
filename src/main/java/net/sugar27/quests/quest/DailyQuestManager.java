// root/src/main/java/net/sugar27/quests/quest/DailyQuestManager.java

package net.sugar27.quests.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedDataType;
import javax.annotation.Nonnull;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.config.QuestConfigPaths;
import net.sugar27.quests.config.QuestServerConfig;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Objects;

// Manages daily quest selection and persistence.
public final class DailyQuestManager {
    private static final DailyQuestManager INSTANCE = new DailyQuestManager();
    private static final String DATA_NAME = "shuga_quests_daily";
    private static final int DAILY_COUNT = 3;

    private final List<QuestDefinition> dailyCandidates = new ArrayList<>();

    // Utility singleton; use get().
    private DailyQuestManager() {
    }

    // Get the singleton instance.
    public static DailyQuestManager get() {
        return INSTANCE;
    }

    // Load daily candidate definitions from config.
    public void loadCandidates() {
        dailyCandidates.clear();
        try {
            Files.createDirectories(QuestConfigPaths.getDailyDir());
        } catch (IOException ex) {
            ShugaQuestsMod.LOGGER.error("Failed to create daily quest directory", ex);
            return;
        }

        try (var paths = Files.list(QuestConfigPaths.getDailyDir())) {
            paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    QuestDefinition quest = QuestDefinition.fromJson(json);
                    dailyCandidates.add(quest);
                } catch (Exception ex) {
                    ShugaQuestsMod.LOGGER.error("Failed to load daily quest file: {}", path, ex);
                }
            });
        } catch (IOException ex) {
            ShugaQuestsMod.LOGGER.error("Failed to list daily quest directory", ex);
        }
    }

    // Ensure the daily selection is up to date for the current date.
    public void ensureDailySelection(MinecraftServer server) {
        ServerLevel level = server.overworld();
        DailyQuestData data = DailyQuestData.get(level);
        String rollDate = getRollDate().toString();
        if (!rollDate.equals(data.date)) {
            reroll(server, false);
        }
    }

    // Reroll the daily quest set and persist it.
    public void reroll(MinecraftServer server, boolean force) {
        if (dailyCandidates.isEmpty()) {
            loadCandidates();
        }
        ServerLevel level = server.overworld();
        DailyQuestData data = DailyQuestData.get(level);
        String rollDate = getRollDate().toString();
        if (!force && rollDate.equals(data.date)) {
            return;
        }
        data.date = rollDate;
        data.questIds.clear();

        List<QuestDefinition> shuffled = new ArrayList<>(dailyCandidates);
        Collections.shuffle(shuffled, new Random());
        for (int i = 0; i < Math.min(DAILY_COUNT, shuffled.size()); i++) {
            data.questIds.add(shuffled.get(i).id());
        }
        data.setDirty();
        ShugaQuestsMod.LOGGER.info("Selected {} daily quests", data.questIds.size());
    }

    // Get the current daily quest ids.
    public List<String> getDailyQuestIds(MinecraftServer server) {
        return DailyQuestData.get(server.overworld()).questIds;
    }

    private static LocalDate getRollDate() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        LocalDate date = now.toLocalDate();
        int rerollHour = QuestServerConfig.dailyRerollHour();
        if (now.getHour() < rerollHour) {
            date = date.minusDays(1);
        }
        return date;
    }

    // Saved data container for daily quest selection.
    public static class DailyQuestData extends SavedData {
        @Nonnull
        public static final Codec<DailyQuestData> CODEC = Objects.requireNonNull(
                Objects.requireNonNull(CompoundTag.CODEC).xmap(DailyQuestData::loadFromTag, DailyQuestData::saveToTag)
        );
        @Nonnull
        public static final SavedDataType<DailyQuestData> TYPE = Objects.requireNonNull(new SavedDataType<>(DATA_NAME, DailyQuestData::new, CODEC));

        private String date = "";
        private final List<String> questIds = new ArrayList<>();

        // Create a new daily quest data entry.
        public DailyQuestData() {
        }

        // Load daily quest data from NBT.
        public static DailyQuestData loadFromTag(CompoundTag tag) {
            DailyQuestData data = new DailyQuestData();
            data.date = tag.getStringOr("date", "");
            ListTag list = tag.getListOrEmpty("quests");
            for (int i = 0; i < list.size(); i++) {
                data.questIds.add(list.getStringOr(i, ""));
            }
            return data;
        }

        // Save daily quest data to NBT.
        public CompoundTag saveToTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("date", Objects.requireNonNull(date));
            ListTag list = new ListTag();
            for (String id : questIds) {
                list.add(net.minecraft.nbt.StringTag.valueOf(Objects.requireNonNull(id)));
            }
            tag.put("quests", list);
            return tag;
        }

        // Fetch or create the daily quest data for the server.
        public static DailyQuestData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}


