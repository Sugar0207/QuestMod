// root/src/main/java/net/sugar27/quests/command/QuestAdminCommand.java

package net.sugar27.quests.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.sugar27.quests.quest.DailyQuestManager;
import net.sugar27.quests.quest.QuestManager;
import net.sugar27.quests.quest.QuestProgressManager;

import java.util.Objects;

// Handles the /questadmin command tree.
public final class QuestAdminCommand {
    private static final QuestProgressManager PROGRESS_MANAGER = new QuestProgressManager();

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
                            PROGRESS_MANAGER.syncAll(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.translatable("command.shuga_quests.reload"), true);
                            return 1;
                        }))
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", Objects.requireNonNull(EntityArgument.player()))
                                .then(Commands.argument("quest_id", Objects.requireNonNull(StringArgumentType.string()))
                                        .executes(context -> {
                                            Objects.requireNonNull(context);
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                            String questId = StringArgumentType.getString(context, "quest_id");
                                            PROGRESS_MANAGER.grantQuest(player, questId);
                                            return 1;
                                        }))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", Objects.requireNonNull(EntityArgument.player()))
                                .executes(context -> {
                                    Objects.requireNonNull(context);
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    PROGRESS_MANAGER.resetQuest(player, "");
                                    return 1;
                                })
                                .then(Commands.argument("quest_id", Objects.requireNonNull(StringArgumentType.string()))
                                        .executes(context -> {
                                            Objects.requireNonNull(context);
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                            String questId = StringArgumentType.getString(context, "quest_id");
                                            PROGRESS_MANAGER.resetQuest(player, questId);
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
}


