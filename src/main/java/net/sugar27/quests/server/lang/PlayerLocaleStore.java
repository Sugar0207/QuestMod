// root/src/main/java/net/sugar27/quests/server/lang/PlayerLocaleStore.java

package net.sugar27.quests.server.lang;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Stores per-player locale selections from clients.
public final class PlayerLocaleStore {
    private static final Map<UUID, String> LOCALES = new ConcurrentHashMap<>();

    // Utility class; no instantiation.
    private PlayerLocaleStore() {
    }

    // Set a player's locale selection.
    public static void setLocale(UUID playerId, String locale) {
        if (playerId == null) {
            return;
        }
        LOCALES.put(playerId, normalizeLocale(locale));
    }

    // Get a player's locale selection with fallback to en_us.
    public static String getLocale(UUID playerId) {
        if (playerId == null) {
            return LangManager.DEFAULT_LOCALE;
        }
        return LOCALES.getOrDefault(playerId, LangManager.DEFAULT_LOCALE);
    }

    // Remove a player's locale mapping.
    public static void clearLocale(UUID playerId) {
        if (playerId != null) {
            LOCALES.remove(playerId);
        }
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return LangManager.DEFAULT_LOCALE;
        }
        return locale.toLowerCase(Locale.ROOT);
    }
}
