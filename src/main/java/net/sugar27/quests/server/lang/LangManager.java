// root/src/main/java/net/sugar27/quests/server/lang/LangManager.java

package net.sugar27.quests.server.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sugar27.quests.ShugaQuestsMod;
import net.sugar27.quests.config.QuestConfigPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Loads server-side language JSON files and resolves quest strings.
public final class LangManager {
    public static final String DEFAULT_LOCALE = "en_us";

    private static final LangManager INSTANCE = new LangManager();
    private final Map<String, Map<String, String>> translations = new HashMap<>();

    // Utility singleton; use get().
    private LangManager() {
    }

    // Get the singleton instance.
    public static LangManager get() {
        return INSTANCE;
    }

    // Reload translations from the config lang directory.
    public void reload() {
        translations.clear();
        Path langDir = QuestConfigPaths.getLangDir();
        try {
            Files.createDirectories(langDir);
        } catch (IOException ex) {
            ShugaQuestsMod.LOGGER.error("Failed to create lang directory", ex);
            return;
        }

        int totalKeys = 0;
        try (var paths = Files.list(langDir)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                String locale = normalizeLocale(stripExtension(path.getFileName().toString()));
                Map<String, String> entries = new HashMap<>();
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                        entries.put(entry.getKey(), entry.getValue().getAsString());
                    }
                } catch (Exception ex) {
                    ShugaQuestsMod.LOGGER.error("Failed to load lang file: {}", path, ex);
                }
                totalKeys += entries.size();
                translations.put(locale, entries);
            }
        } catch (IOException ex) {
            ShugaQuestsMod.LOGGER.error("Failed to list lang directory", ex);
        }

        ShugaQuestsMod.LOGGER.info("Loaded {} lang locales with {} keys", translations.size(), totalKeys);
    }

    // Translate a key for a locale with fallback to en_us and the key itself.
    public String translate(String locale, String key) {
        if (key == null) {
            return "";
        }
        String normalized = normalizeLocale(locale);
        String value = findTranslation(normalized, key);
        if (value != null) {
            return value;
        }
        if (!DEFAULT_LOCALE.equals(normalized)) {
            value = findTranslation(DEFAULT_LOCALE, key);
            if (value != null) {
                return value;
            }
        }
        return key;
    }

    // Normalize locale strings to lowercase and default when missing.
    public String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        return locale.toLowerCase(Locale.ROOT);
    }

    private String findTranslation(String locale, String key) {
        Map<String, String> localeMap = translations.get(locale);
        if (localeMap == null) {
            return null;
        }
        return localeMap.get(key);
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }
}
