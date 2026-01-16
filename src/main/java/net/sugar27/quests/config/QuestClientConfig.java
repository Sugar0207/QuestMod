// root/src/main/java/net/sugar27/quests/config/QuestClientConfig.java

package net.sugar27.quests.config;

import net.neoforged.neoforge.common.ModConfigSpec;

// Client-only configuration for UI and feedback.
public final class QuestClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD_NOTIFICATIONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Client-side settings for Shuga Quests.").push("client");
        HUD_NOTIFICATIONS = builder
                .comment("Show quest HUD notifications and play sounds for updates/completions.")
                .define("hudNotifications", true);
        builder.pop();
        SPEC = builder.build();
    }

    // Utility class; no instantiation.
    private QuestClientConfig() {
    }

    public static boolean hudNotificationsEnabled() {
        return HUD_NOTIFICATIONS.get();
    }
}
