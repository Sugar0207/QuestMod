// root/src/main/java/net/sugar27/quests/client/gui/QuestScreen.java

package net.sugar27.quests.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sugar27.quests.client.QuestClientState;
import net.sugar27.quests.quest.QuestCategory;
import net.sugar27.quests.quest.QuestCriteria;
import net.sugar27.quests.quest.QuestDefinition;
import net.sugar27.quests.quest.QuestObjective;
import net.sugar27.quests.quest.QuestProgress;
import net.sugar27.quests.quest.QuestReward;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// Main quest UI for browsing categories, list, and details.
public class QuestScreen extends Screen {
    private QuestCategory selectedCategory = QuestCategory.ALL;
    private String selectedQuestId = "";
    private boolean showDailyOnly = false;

    private final List<QuestDefinition> filteredQuests = new ArrayList<>();

    // Construct the screen with the localized title.
    public QuestScreen() {
        super(Objects.requireNonNull(Component.translatable("screen.shuga_quests.title")));
    }

    // Initialize buttons and layout.
    @Override
    protected void init() {
        int x = this.width / 2 - 150;
        int y = 20;
        int buttonWidth = 55;
        int buttonHeight = 20;
        int buttonStep = buttonWidth + 5;

        @Nonnull Component allLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.all"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(allLabel, button -> {
            selectedCategory = QuestCategory.ALL;
            showDailyOnly = false;
            refreshFiltered();
        }).bounds(x, y, buttonWidth, buttonHeight).build()));

        @Nonnull Component dailyLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.daily"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(dailyLabel, button -> {
            selectedCategory = QuestCategory.ALL;
            showDailyOnly = true;
            refreshFiltered();
        }).bounds(x + buttonStep, y, buttonWidth, buttonHeight).build()));

        @Nonnull Component lifeLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.life"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(lifeLabel, button -> {
            selectedCategory = QuestCategory.LIFE;
            showDailyOnly = false;
            refreshFiltered();
        }).bounds(x + buttonStep * 2, y, buttonWidth, buttonHeight).build()));

        @Nonnull Component exploreLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.explore"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(exploreLabel, button -> {
            selectedCategory = QuestCategory.EXPLORE;
            showDailyOnly = false;
            refreshFiltered();
        }).bounds(x + buttonStep * 3, y, buttonWidth, buttonHeight).build()));

        @Nonnull Component combatLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.combat"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(combatLabel, button -> {
            selectedCategory = QuestCategory.COMBAT;
            showDailyOnly = false;
            refreshFiltered();
        }).bounds(x + buttonStep * 4, y, buttonWidth, buttonHeight).build()));

        refreshFiltered();
    }

    // Render the quest list and detail panel.
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var questDefinitions = QuestClientState.getQuestDefinitions();
        refreshFiltered();
        List<String> dailyQuestIds = QuestClientState.getDailyQuestIds();

        // Call super.render first so that the screen background and widgets are
        // rendered by the parent implementation. Some vanilla implementations
        // may also trigger the background blur; calling renderBackground here
        // in addition to the parent's rendering can cause the blur to be
        // applied multiple times in one frame and crash with
        // "Can only blur once per frame". To avoid double-blur, avoid an
        // explicit background call here and draw our UI on top of the
        // parent's rendering.
        super.render(graphics, mouseX, mouseY, partialTick);

        Font font = Objects.requireNonNull(this.font);
        graphics.drawCenteredString(font, Objects.requireNonNull(this.title), this.width / 2, 6, 0xFFFFFFFF);

        int listX = 20;
        int listY = 50;
        int lineHeight = 12;

        int index = 0;
        for (QuestDefinition quest : filteredQuests) {
            int y = listY + index * lineHeight;
            boolean isDaily = dailyQuestIds.contains(quest.id());
            int color = quest.id().equals(selectedQuestId)
                    ? 0xFFFFE080
                    : isDaily
                        ? 0xFFFFC040
                        : 0xFFFFFFFF;
            Component title = Component.translatable(Objects.requireNonNull(quest.titleKey()));
            Component line = isDaily
                    ? Component.literal("[").append(Component.translatable("screen.shuga_quests.daily_tag")).append("] ").append(title)
                    : title;
            graphics.drawString(font, Objects.requireNonNull(line), listX, y, color);
            index++;
        }

        renderDetails(graphics, mouseX, mouseY);
    }

    // Render the detail panel for the selected quest.
    private void renderDetails(GuiGraphics graphics, int mouseX, int mouseY) {
        if (selectedQuestId.isEmpty()) {
            return;
        }
        QuestDefinition quest = QuestClientState.getQuestDefinitions().get(selectedQuestId);
        if (quest == null) {
            return;
        }

        int detailX = 220;
        int detailY = 50;
        boolean isDaily = QuestClientState.getDailyQuestIds().contains(quest.id());
        Font font = Objects.requireNonNull(this.font);
        if (isDaily) {
            graphics.drawString(font, Objects.requireNonNull(Component.translatable("screen.shuga_quests.daily_label")), detailX, detailY, 0xFFFFC040);
            detailY += 12;
        }
        graphics.drawString(font, Objects.requireNonNull(Component.translatable(Objects.requireNonNull(quest.titleKey()))), detailX, detailY, 0xFFFFFFFF);
        graphics.drawString(font, Objects.requireNonNull(Component.translatable(Objects.requireNonNull(quest.descriptionKey()))), detailX, detailY + 14, 0xFFB0B0B0);

        QuestProgress progress = QuestClientState.getQuestProgress().get(selectedQuestId);
        Component status = progress == null
                ? Component.translatable("screen.shuga_quests.status.not_started")
                : progress.isCompleted()
                    ? Component.translatable("screen.shuga_quests.status.complete")
                    : Component.translatable("screen.shuga_quests.status.in_progress");
        graphics.drawString(font, Objects.requireNonNull(status), detailX, detailY + 30, 0xFF80FF80);

        int offsetY = detailY + 50;
        graphics.drawString(font, Objects.requireNonNull(Component.translatable("screen.shuga_quests.objectives")), detailX, offsetY, 0xFFFFFFFF);
        offsetY += 12;
        for (QuestObjective objective : quest.objectives()) {
            Component objectiveLine = getObjectiveProgressComponent(objective, progress);
            graphics.drawString(font, Objects.requireNonNull(objectiveLine), detailX, offsetY, 0xFFD0D0D0);
            offsetY += 12;
        }

        offsetY += 6;
        graphics.drawString(font, Objects.requireNonNull(Component.translatable("screen.shuga_quests.rewards")), detailX, offsetY, 0xFFFFFFFF);
        offsetY += 12;
        for (QuestReward reward : quest.rewards()) {
            if (reward.type() == net.sugar27.quests.quest.QuestRewardType.COMMAND) {
                continue;
            }
            Component rewardLine = Component.literal("- ").append(Objects.requireNonNull(reward.describeComponent()));
            graphics.drawString(font, Objects.requireNonNull(rewardLine), detailX, offsetY, 0xFFD0D0D0);
            offsetY += 12;
        }
    }

    // Rebuild the filtered quest list based on category.
    private void refreshFiltered() {
        filteredQuests.clear();
        var questDefinitions = QuestClientState.getQuestDefinitions();
        List<String> dailyQuestIds = QuestClientState.getDailyQuestIds();
        for (QuestDefinition quest : questDefinitions.values()) {
            boolean isDailyQuest = "daily".equalsIgnoreCase(quest.type());
            boolean isSelectedDaily = dailyQuestIds.contains(quest.id());
            if (showDailyOnly) {
                if (isDailyQuest && isSelectedDaily) {
                    filteredQuests.add(quest);
                }
            } else if (!isDailyQuest || isSelectedDaily) {
                if (selectedCategory == QuestCategory.ALL || quest.category() == selectedCategory) {
                    filteredQuests.add(quest);
                }
            }
        }
        filteredQuests.sort(Comparator.comparing(QuestDefinition::id));
        if (!filteredQuests.isEmpty() && (selectedQuestId.isEmpty() || !QuestClientState.getQuestDefinitions().containsKey(selectedQuestId))) {
            selectedQuestId = filteredQuests.get(0).id();
        }
    }

    // Handle list selection by mouse click.
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = 20;
        int listY = 50;
        int lineHeight = 12;
        if (mouseX >= listX && mouseX <= listX + 180 && mouseY >= listY) {
            int index = (int) ((mouseY - listY) / lineHeight);
            if (index >= 0 && index < filteredQuests.size()) {
                selectedQuestId = filteredQuests.get(index).id();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // Allow the screen to close with inventory key.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Pause behavior is disabled for multiplayer usage.
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component getObjectiveProgressComponent(QuestObjective objective, QuestProgress progress) {
        int total = 0;
        int current = 0;
        List<QuestCriteria> criteriaList = objective.criteria();
        QuestProgress.ObjectiveProgress objectiveProgress = progress == null ? null : progress.objectives().get(objective.id());

        if (criteriaList.isEmpty()) {
            total = 1;
            current = (objectiveProgress != null && objectiveProgress.isCompleted()) ? 1 : 0;
        } else if (objective.logic() == net.sugar27.quests.quest.QuestLogicOperator.OR) {
            for (int i = 0; i < criteriaList.size(); i++) {
                int required = Math.max(0, criteriaList.get(i).count());
                total = Math.max(total, required);
                int value = 0;
                if (objectiveProgress != null && i < objectiveProgress.criteriaCounts().size()) {
                    value = Math.max(0, objectiveProgress.criteriaCounts().get(i));
                }
                current = Math.max(current, Math.min(value, required));
            }
            if (objectiveProgress != null && objectiveProgress.isCompleted()) {
                current = total;
            }
        } else {
            for (int i = 0; i < criteriaList.size(); i++) {
                int required = Math.max(0, criteriaList.get(i).count());
                total += required;
                int value = 0;
                if (objectiveProgress != null && i < objectiveProgress.criteriaCounts().size()) {
                    value = Math.max(0, objectiveProgress.criteriaCounts().get(i));
                }
                current += Math.min(value, required);
            }
        }

        Component label = Component.translatable(getObjectiveTranslationKey(objective));
        Component progressLabel = Component.translatable("quest.objective.progress", label, current, total);
        return Component.literal("- ").append(progressLabel);
    }

    private String getObjectiveTranslationKey(QuestObjective objective) {
        return "quest.objective." + objective.id();
    }
}
