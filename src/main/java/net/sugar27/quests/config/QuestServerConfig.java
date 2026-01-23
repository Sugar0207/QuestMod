// root/src/main/java/net/sugar27/quests/config/QuestServerConfig.java

package net.sugar27.quests.config;

import net.neoforged.neoforge.common.ModConfigSpec;

// Server-only configuration for gameplay behavior.
public final class QuestServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DAILY_REROLL_HOUR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Server-side settings for Shuga Quests.").push("server");
        DAILY_REROLL_HOUR = builder
                .comment("Hour of day (0-23) when daily quests reroll.")
                .defineInRange("dailyRerollHour", 4, 0, 23);
        builder.pop();
        SPEC = builder.build();
    }

    // Utility class; no instantiation.
    private QuestServerConfig() {
    }

    public static int dailyRerollHour() {
        return DAILY_REROLL_HOUR.get();
    }
}
