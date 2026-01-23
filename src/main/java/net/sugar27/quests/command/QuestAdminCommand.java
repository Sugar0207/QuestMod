// root/src/main/java/net/sugar27/quests/command/QuestAdminCommand.java

package net.sugar27.quests.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.sugar27.quests.quest.DailyQuestManager;
import net.sugar27.quests.quest.QuestManager;
import net.sugar27.quests.quest.QuestProgressManager;
import net.sugar27.quests.quest.QuestDefinition;
import net.sugar27.quests.server.lang.LangManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Handles the /questadmin command tree.
public final class QuestAdminCommand {
    private static final String ALL_QUESTS_TOKEN = "all";
    private static final String EMPTY_QUEST_ID = "";
    private static final QuestProgressManager PROGRESS_MANAGER = new QuestProgressManager();
    private static final SuggestionProvider<CommandSourceStack> QUEST_ID_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(getSortedQuestIds(false), builder);
    };
    private static final SuggestionProvider<CommandSourceStack> QUEST_ID_OR_ALL_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(getSortedQuestIds(true), builder);
    };

    // Utility class; no instantiation.
    private QuestAdminCommand() {
    }

    // Register the command dispatcher callback.
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    // Build the command tree.
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("questadmin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            Objects.requireNonNull(context);
                            QuestManager.get().loadAll();
                            DailyQuestManager.get().loadCandidates();
                            LangManager.get().reload();
                            PROGRESS_MANAGER.syncAll(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.translatable("command.shuga_quests.reload"), true);
                            return 1;
                        }))
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", Objects.requireNonNull(EntityArgument.player()))
                                .then(Commands.argument("quest_id", Objects.requireNonNull(StringArgumentType.string()))
                                        .suggests(QUEST_ID_OR_ALL_SUGGESTIONS)
                                        .executes(context -> {
                                            Objects.requireNonNull(context);
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                            String questId = StringArgumentType.getString(context, "quest_id");
                                            if (ALL_QUESTS_TOKEN.equalsIgnoreCase(questId)) {
                                                PROGRESS_MANAGER.grantAllQuests(player);
                                            } else {
                                                PROGRESS_MANAGER.grantQuest(player, questId);
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("list")
                        .executes(context -> {
                            Objects.requireNonNull(context);
                            var quests = QuestManager.get().getAll();
                            if (quests.isEmpty()) {
                                context.getSource().sendSuccess(
                                        () -> Component.translatable("command.shuga_quests.list_empty"), false);
                                return 1;
                            }
                            var ids = getSortedQuestIds(quests, false);
                            context.getSource().sendSuccess(
                                    () -> Component.translatable("command.shuga_quests.list_header", ids.size()), false);
                            for (String id : ids) {
                                context.getSource().sendSuccess(() -> Component.literal("- " + id), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", Objects.requireNonNull(EntityArgument.player()))
                                .executes(context -> {
                                    Objects.requireNonNull(context);
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    PROGRESS_MANAGER.resetQuest(player, EMPTY_QUEST_ID);
                                    return 1;
                                })
                                .then(Commands.argument("quest_id", Objects.requireNonNull(StringArgumentType.string()))
                                        .suggests(QUEST_ID_OR_ALL_SUGGESTIONS)
                                        .executes(context -> {
                                            Objects.requireNonNull(context);
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                            String questId = StringArgumentType.getString(context, "quest_id");
                                            if (ALL_QUESTS_TOKEN.equalsIgnoreCase(questId)) {
                                                PROGRESS_MANAGER.resetQuest(player, EMPTY_QUEST_ID);
                                            } else {
                                                PROGRESS_MANAGER.resetQuest(player, questId);
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("daily")
                        .then(Commands.literal("reroll")
                                .executes(context -> {
                                    Objects.requireNonNull(context);
                                    DailyQuestManager.get().reroll(context.getSource().getServer(), true);
                                    PROGRESS_MANAGER.syncAll(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.translatable("command.shuga_quests.daily_reroll"), true);
                                    return 1;
                                }))));
    }

    private static List<String> getSortedQuestIds(boolean includeAllToken) {
        return getSortedQuestIds(QuestManager.get().getAll(), includeAllToken);
    }

    private static List<String> getSortedQuestIds(Map<String, QuestDefinition> quests, boolean includeAllToken) {
        var ids = new ArrayList<>(quests.keySet());
        if (includeAllToken) {
            ids.add(ALL_QUESTS_TOKEN);
        }
        ids.sort(String::compareToIgnoreCase);
        return ids;
    }
}


