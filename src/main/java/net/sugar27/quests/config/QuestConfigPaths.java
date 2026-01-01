// root/src/main/java/net/sugar27/quests/config/QuestConfigPaths.java

package net.sugar27.quests.config;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

// Centralizes config folder paths for quest JSON files.
public final class QuestConfigPaths {
    private static final String ROOT_FOLDER = "shuga_quests";

    // Utility class; no instantiation.
    private QuestConfigPaths() {
    }

    // Get the root config directory for this mod.
    public static Path getRootDir() {
        return FMLPaths.CONFIGDIR.get().resolve(ROOT_FOLDER);
    }

    // Get the normal quests directory.
    public static Path getQuestsDir() {
        return getRootDir().resolve("quests");
    }

    // Get the daily quests directory.
    public static Path getDailyDir() {
        return getRootDir().resolve("daily");
    }

    // Get the server lang directory.
    public static Path getLangDir() {
        return getRootDir().resolve("lang");
    }
}


