// root/src/main/java/net/sugar27/quests/client/gui/QuestScreen.java

package net.sugar27.quests.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.sugar27.quests.client.QuestClientState;
import net.sugar27.quests.network.QuestStartPacket;
import net.sugar27.quests.network.QuestStopPacket;
import net.sugar27.quests.quest.QuestCategory;
import net.sugar27.quests.quest.QuestCriteria;
import net.sugar27.quests.quest.QuestDefinition;
import net.sugar27.quests.quest.QuestObjective;
import net.sugar27.quests.quest.QuestProgress;
import net.sugar27.quests.quest.QuestReward;
import net.sugar27.quests.quest.QuestTypes;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;

// Main quest UI for browsing categories, list, and details.
public class QuestScreen extends Screen {
    private static final int DETAIL_X = 220;
    private static final int DETAIL_Y = 50;
    private static final int START_BUTTON_WIDTH = 90;
    private static final int START_BUTTON_HEIGHT = 20;
    private static final int START_BUTTON_Y_OFFSET = 42;
    private static final int LIST_X = 20;
    private static final int LIST_WIDTH = 180;
    private static final int LIST_HIGHLIGHT_PAD = 2;
    private static final int LIST_HEADER_STRIPE_WIDTH = 2;
    private static final int LIST_TOP_BASE_Y = 50;
    private static final int LIST_BOTTOM_PADDING = 20;
    private static final float HEADER_HEIGHT_SCALE = 1.5f;
    private static final Component HIDDEN_TEXT = Component.translatable("screen.shuga_quests.hidden");

    private QuestCategory selectedCategory = QuestCategory.ALL;
    private String selectedQuestId = "";
    private boolean showDailyOnly = false;
    private Button startButton;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;

    private final List<QuestDefinition> filteredQuests = new ArrayList<>();
    private final List<QuestListEntry> listEntries = new ArrayList<>();

    private static final class QuestListEntry {
        private final QuestDefinition quest;
        private final Component header;
        private final Component info;
        private final boolean spacer;

        private QuestListEntry(QuestDefinition quest, Component header, Component info, boolean spacer) {
            this.quest = quest;
            this.header = header;
            this.info = info;
            this.spacer = spacer;
        }

        private static QuestListEntry header(Component header) {
            return new QuestListEntry(null, header, null, false);
        }

        private static QuestListEntry info(Component info) {
            return new QuestListEntry(null, null, info, false);
        }

        private static QuestListEntry quest(QuestDefinition quest) {
            return new QuestListEntry(quest, null, null, false);
        }

        private static QuestListEntry spacer() {
            return new QuestListEntry(null, null, null, true);
        }

        private boolean isHeader() {
            return header != null;
        }

        private boolean isInfo() {
            return info != null;
        }

        private boolean isSpacer() {
            return spacer;
        }

        private Component header() {
            return Objects.requireNonNull(header);
        }

        private Component info() {
            return Objects.requireNonNull(info);
        }

        private QuestDefinition quest() {
            return Objects.requireNonNull(quest);
        }
    }

    // Construct the screen with the localized title.
    public QuestScreen() {
        super(Objects.requireNonNull(Component.translatable("screen.shuga_quests.title")));
    }

    // Initialize buttons and layout.
    @Override
    protected void init() {
        int y = 20;
        int buttonWidth = 70;
        int buttonHeight = 20;
        int buttonGap = 5;
        int buttonCount = 4;
        int totalWidth = buttonWidth * buttonCount + buttonGap * (buttonCount - 1);
        int x = this.width / 2 - totalWidth / 2;
        int buttonStep = buttonWidth + buttonGap;

        @Nonnull Component allLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.all"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(allLabel, button -> {
            selectedCategory = QuestCategory.ALL;
            showDailyOnly = false;
            refreshFiltered();
        }).bounds(x, y, buttonWidth, buttonHeight).build()));

        @Nonnull Component lifeLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.life"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(lifeLabel, button -> {
            selectedCategory = QuestCategory.LIFE;
            showDailyOnly = false;
            refreshFiltered();
        }).bounds(x + buttonStep, y, buttonWidth, buttonHeight).build()));

        @Nonnull Component exploreLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.explore"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(exploreLabel, button -> {
            selectedCategory = QuestCategory.EXPLORE;
            showDailyOnly = false;
            refreshFiltered();
        }).bounds(x + buttonStep * 2, y, buttonWidth, buttonHeight).build()));

        @Nonnull Component combatLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.category.combat"));
        addRenderableWidget(Objects.requireNonNull(Button.builder(combatLabel, button -> {
            selectedCategory = QuestCategory.COMBAT;
            showDailyOnly = false;
            refreshFiltered();
        }).bounds(x + buttonStep * 3, y, buttonWidth, buttonHeight).build()));

        @Nonnull Component startLabel = Objects.requireNonNull(Component.translatable("screen.shuga_quests.start"));
        startButton = Button.builder(startLabel, button -> {
            if (!selectedQuestId.isEmpty()) {
                String activeQuestId = QuestClientState.getActiveQuestId();
                if (activeQuestId != null && !activeQuestId.isEmpty() && activeQuestId.equals(selectedQuestId)) {
                    openStopConfirm(selectedQuestId);
                } else if (activeQuestId != null && !activeQuestId.isEmpty()) {
                    openStartOverrideConfirm(activeQuestId, selectedQuestId);
                } else {
                    ClientPacketDistributor.sendToServer(new QuestStartPacket(selectedQuestId));
                }
            }
        }).bounds(DETAIL_X, DETAIL_Y + START_BUTTON_Y_OFFSET, START_BUTTON_WIDTH, START_BUTTON_HEIGHT).build();
        addRenderableWidget(startButton);

        refreshFiltered();
    }

    // Render the quest list and detail panel.
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var questDefinitions = QuestClientState.getQuestDefinitions();
        refreshFiltered();
        List<String> dailyQuestIds = QuestClientState.getDailyQuestIds();
        QuestDefinition selectedQuest = selectedQuestId.isEmpty() ? null : questDefinitions.get(selectedQuestId);
        boolean selectedDaily = selectedQuest != null && dailyQuestIds.contains(selectedQuest.id());
        updateStartButtonState(selectedQuest, selectedDaily);

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

        int listX = LIST_X;
        float titleScale = getListTitleScale(font);
        int lineHeight = getListLineHeight(font);
        int listY = getListTopY(font);

        String activeQuestId = QuestClientState.getActiveQuestId();
        int visibleLines = getVisibleListLines(listY, lineHeight);
        int startIndex = Math.min(scrollOffset, Math.max(0, listEntries.size() - 1));
        int endIndex = Math.min(listEntries.size(), startIndex + visibleLines);
        QuestListEntry stickyHeader = getStickyHeaderEntry();
        if (stickyHeader != null) {
            int stickyY = listY - getHeaderHeight(lineHeight);
            drawSectionHeader(graphics, font, listX, stickyY, lineHeight, titleScale, stickyHeader.header(), true);
        }
        int index = 0;
        for (int i = startIndex; i < endIndex; i++) {
            QuestListEntry entry = listEntries.get(i);
            int y = listY + index * lineHeight;
            int scaledLineHeight = (int) Math.ceil(font.lineHeight * titleScale);
            int textY = y + (lineHeight - scaledLineHeight) / 2;
            if (entry.isHeader()) {
                drawSectionHeader(graphics, font, listX, y, lineHeight, titleScale, entry.header(), false);
                index++;
                continue;
            }
            if (entry.isSpacer()) {
                index++;
                continue;
            }
            if (entry.isInfo()) {
                drawSectionInfo(graphics, font, listX, y, lineHeight, titleScale, entry.info());
                index++;
                continue;
            }
            QuestDefinition quest = entry.quest();
            boolean isDaily = dailyQuestIds.contains(quest.id());
            boolean isSelected = quest.id().equals(selectedQuestId);
            boolean isActive = quest.id().equals(activeQuestId);
            if (isSelected) {
                int highlightLeft = listX - LIST_HIGHLIGHT_PAD;
                int highlightRight = listX + LIST_WIDTH - LIST_HIGHLIGHT_PAD;
                graphics.fill(highlightLeft, y, highlightRight, y + lineHeight - 1, 0x402D3446);
            }
            int color = isSelected
                    ? (isActive ? 0xFFB8FFB8 : 0xFFFFE080)
                    : isActive
                        ? 0xFF80FF80
                    : isDaily
                        ? 0xFFFFC040
                        : 0xFFFFFFFF;
            Component title = isQuestLocked(quest)
                    ? HIDDEN_TEXT
                    : Component.translatable(Objects.requireNonNull(quest.titleKey()));
            Component line = title;
            if (isDaily) {
                line = Component.literal("[").append(Component.translatable("screen.shuga_quests.daily_tag")).append("] ").append(line);
            }
            graphics.pose().pushMatrix();
            graphics.pose().translate(listX, textY);
            graphics.pose().scale(titleScale, titleScale);
            graphics.drawString(font, Objects.requireNonNull(line), 0, 0, color);
            graphics.pose().popMatrix();
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

        int detailX = DETAIL_X;
        int detailY = DETAIL_Y;
        boolean isDaily = QuestClientState.getDailyQuestIds().contains(quest.id());
        boolean maskLocked = isQuestLocked(quest);
        Font font = Objects.requireNonNull(this.font);
        if (isDaily) {
            graphics.drawString(font, Objects.requireNonNull(Component.translatable("screen.shuga_quests.daily_label")), detailX, detailY, 0xFFFFC040);
            detailY += 12;
        }
        Component title = maskLocked
                ? HIDDEN_TEXT
                : Component.translatable(Objects.requireNonNull(quest.titleKey()));
        graphics.drawString(font, Objects.requireNonNull(title), detailX, detailY, 0xFFFFFFFF);
        Component description = maskLocked
                ? HIDDEN_TEXT
                : Component.translatable(Objects.requireNonNull(quest.descriptionKey()));
        graphics.drawString(font, Objects.requireNonNull(description), detailX, detailY + 14, 0xFFB0B0B0);

        QuestProgress progress = QuestClientState.getQuestProgress().get(selectedQuestId);
        Component status = progress == null
                ? Component.translatable("screen.shuga_quests.status.not_started")
                : progress.isCompleted()
                    ? Component.translatable("screen.shuga_quests.status.complete")
                    : Component.translatable("screen.shuga_quests.status.in_progress");
        graphics.drawString(font, Objects.requireNonNull(status), detailX, detailY + 30, 0xFF80FF80);

        int offsetY = detailY + 50 + START_BUTTON_HEIGHT;
        if (!quest.prerequisites().isEmpty()) {
            graphics.drawString(font, Objects.requireNonNull(Component.translatable("screen.shuga_quests.prerequisites")), detailX, offsetY, 0xFFFFFFFF);
            offsetY += 12;
            for (String prerequisiteId : quest.prerequisites()) {
                Component prerequisiteLine = getPrerequisiteLine(prerequisiteId);
                graphics.drawString(font, Objects.requireNonNull(prerequisiteLine), detailX, offsetY, 0xFFD0D0D0);
                offsetY += 12;
            }
            offsetY += 6;
        }
        graphics.drawString(font, Objects.requireNonNull(Component.translatable("screen.shuga_quests.objectives")), detailX, offsetY, 0xFFFFFFFF);
        offsetY += 12;
        if (maskLocked) {
            for (int i = 0; i < quest.objectives().size(); i++) {
                Component objectiveLine = Component.literal("- ").append(HIDDEN_TEXT);
                graphics.drawString(font, Objects.requireNonNull(objectiveLine), detailX, offsetY, 0xFFD0D0D0);
                offsetY += 12;
            }
        } else {
            for (QuestObjective objective : quest.objectives()) {
                Component objectiveLine = getObjectiveProgressComponent(objective, progress);
                graphics.drawString(font, Objects.requireNonNull(objectiveLine), detailX, offsetY, 0xFFD0D0D0);
                offsetY += 12;
            }
        }

        offsetY += 6;
        graphics.drawString(font, Objects.requireNonNull(Component.translatable("screen.shuga_quests.rewards")), detailX, offsetY, 0xFFFFFFFF);
        offsetY += 12;
        if (maskLocked) {
            for (QuestReward reward : quest.rewards()) {
                if (reward.type() == net.sugar27.quests.quest.QuestRewardType.COMMAND) {
                    continue;
                }
                Component rewardLine = Component.literal("- ").append(HIDDEN_TEXT);
                graphics.drawString(font, Objects.requireNonNull(rewardLine), detailX, offsetY, 0xFFD0D0D0);
                offsetY += 12;
            }
        } else {
            for (QuestReward reward : quest.rewards()) {
                if (reward.type() == net.sugar27.quests.quest.QuestRewardType.COMMAND) {
                    continue;
                }
                Component rewardLine = Component.literal("- ").append(Objects.requireNonNull(reward.describeComponent()));
                graphics.drawString(font, Objects.requireNonNull(rewardLine), detailX, offsetY, 0xFFD0D0D0);
                offsetY += 12;
            }
        }
    }

    // Rebuild the filtered quest list based on category.
    private void refreshFiltered() {
        filteredQuests.clear();
        listEntries.clear();
        var questDefinitions = QuestClientState.getQuestDefinitions();
        List<String> dailyQuestIds = QuestClientState.getDailyQuestIds();
        List<QuestDefinition> dailyQuests = new ArrayList<>();
        List<QuestDefinition> incompleteQuests = new ArrayList<>();
        List<QuestDefinition> completeQuests = new ArrayList<>();
        int dailyTotal = 0;
        for (QuestDefinition quest : questDefinitions.values()) {
            boolean isDailyQuest = QuestTypes.DAILY.equalsIgnoreCase(quest.type());
            boolean isSelectedDaily = dailyQuestIds.contains(quest.id());
            boolean includeQuest = false;
            if (showDailyOnly) {
                includeQuest = isDailyQuest && isSelectedDaily;
            } else if (!isDailyQuest || isSelectedDaily) {
                includeQuest = selectedCategory == QuestCategory.ALL || quest.category() == selectedCategory;
            }
            if (!includeQuest) {
                continue;
            }
            QuestProgress progress = QuestClientState.getQuestProgress().get(quest.id());
            if (isDailyQuest && isSelectedDaily) {
                dailyTotal++;
                if (progress == null || !progress.isCompleted()) {
                    dailyQuests.add(quest);
                }
                continue;
            }
            if (progress != null && progress.isCompleted()) {
                completeQuests.add(quest);
            } else {
                incompleteQuests.add(quest);
            }
        }
        dailyQuests.sort(Comparator.comparing(QuestDefinition::id));
        incompleteQuests.sort(Comparator.comparingInt(this::getIncompleteSortRank)
                .thenComparing(QuestDefinition::id));
        completeQuests.sort(Comparator.comparing(QuestDefinition::id));
        addSectionWithInfo(Component.translatable("screen.shuga_quests.list.daily"), dailyQuests,
                dailyTotal > 0 && dailyQuests.isEmpty() ? Component.translatable("screen.shuga_quests.list.daily_complete") : null);
        addSection(Component.translatable("screen.shuga_quests.list.incomplete"), incompleteQuests);
        addSection(Component.translatable("screen.shuga_quests.list.complete"), completeQuests);
        Font font = Objects.requireNonNull(this.font);
        maxScrollOffset = Math.max(0, listEntries.size() - getVisibleListLines(getListTopY(font), getListLineHeight(font)));
        scrollOffset = Math.min(scrollOffset, maxScrollOffset);
        if (!filteredQuests.isEmpty() && (selectedQuestId.isEmpty() || !QuestClientState.getQuestDefinitions().containsKey(selectedQuestId))) {
            selectedQuestId = filteredQuests.get(0).id();
        }
    }

    // Handle list selection by mouse click.
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Font font = Objects.requireNonNull(this.font);
        int listX = LIST_X;
        int listY = getListTopY(font);
        int lineHeight = getListLineHeight(font);
        int visibleLines = getVisibleListLines(listY, lineHeight);
        int listBottom = listY + visibleLines * lineHeight;
        if (mouseX >= listX && mouseX <= listX + LIST_WIDTH && mouseY >= listY && mouseY < listBottom) {
            int index = (int) ((mouseY - listY) / lineHeight) + scrollOffset;
            if (index >= 0 && index < listEntries.size()) {
                QuestListEntry entry = listEntries.get(index);
                if (!entry.isHeader() && !entry.isInfo() && !entry.isSpacer()) {
                    selectedQuestId = entry.quest().id();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (listEntries.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int listX = LIST_X;
        int listY = getListTopY(font);
        Font font = Objects.requireNonNull(this.font);
        int lineHeight = getListLineHeight(font);
        int visibleLines = getVisibleListLines(listY, lineHeight);
        int listBottom = listY + visibleLines * lineHeight;
        if (mouseX < listX || mouseX > listX + LIST_WIDTH || mouseY < listY || mouseY >= listBottom) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int direction = (int) Math.signum(scrollY);
        if (direction == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = clamp(scrollOffset - direction, 0, maxScrollOffset);
        return true;
    }

    // Allow the screen to close with inventory key.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            int direction = keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
            if (!filteredQuests.isEmpty()) {
                int currentIndex = getSelectedIndex();
                int nextIndex = clamp(currentIndex + direction, 0, filteredQuests.size() - 1);
                selectedQuestId = filteredQuests.get(nextIndex).id();
                ensureSelectedVisible(selectedQuestId);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Pause behavior is disabled for multiplayer usage.
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
    }

    private void updateStartButtonState(QuestDefinition selectedQuest, boolean selectedDaily) {
        if (startButton == null) {
            return;
        }
        startButton.setY(DETAIL_Y + (selectedDaily ? 12 : 0) + START_BUTTON_Y_OFFSET);
        if (selectedQuest == null) {
            startButton.active = false;
            return;
        }
        QuestProgress progress = QuestClientState.getQuestProgress().get(selectedQuest.id());
        if (progress != null && progress.isCompleted()) {
            startButton.active = false;
            return;
        }
        String activeQuestId = QuestClientState.getActiveQuestId();
        boolean isActiveQuest = activeQuestId != null && !activeQuestId.isEmpty() && activeQuestId.equals(selectedQuest.id());
        if (isActiveQuest) {
            startButton.active = true;
            startButton.setMessage(Objects.requireNonNull(Component.translatable("screen.shuga_quests.stop")));
            return;
        }
        if (!arePrerequisitesMet(selectedQuest)) {
            startButton.active = false;
            startButton.setMessage(Objects.requireNonNull(Component.translatable("screen.shuga_quests.locked")));
            return;
        }
        startButton.active = true;
        startButton.setMessage(Objects.requireNonNull(Component.translatable("screen.shuga_quests.start")));
    }

    private void addSection(Component header, List<QuestDefinition> quests) {
        addSectionWithInfo(header, quests, null);
    }

    private void addSectionWithInfo(Component header, List<QuestDefinition> quests, Component info) {
        if (quests.isEmpty() && info == null) {
            return;
        }
        listEntries.add(QuestListEntry.header(header));
        listEntries.add(QuestListEntry.spacer());
        if (info != null) {
            listEntries.add(QuestListEntry.info(info));
            listEntries.add(QuestListEntry.spacer());
        }
        for (QuestDefinition quest : quests) {
            listEntries.add(QuestListEntry.quest(quest));
            filteredQuests.add(quest);
        }
    }

    private void drawSectionInfo(GuiGraphics graphics, Font font, int listX, int y, int lineHeight, float titleScale,
                                 Component info) {
        int scaledLineHeight = (int) Math.ceil(font.lineHeight * titleScale);
        int textY = y + (lineHeight - scaledLineHeight) / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(listX + 6, textY);
        graphics.pose().scale(titleScale, titleScale);
        graphics.drawString(font, Objects.requireNonNull(info), 0, 0, 0xFFFFD060);
        graphics.pose().popMatrix();
    }

    private int getHeaderHeight(int lineHeight) {
        return Math.max(1, Math.round(lineHeight * HEADER_HEIGHT_SCALE));
    }

    private int getIncompleteSortRank(QuestDefinition quest) {
        if (quest.prerequisites().isEmpty()) {
            return 0;
        }
        if (arePrerequisitesMet(quest)) {
            return 1;
        }
        return isQuestLocked(quest) ? 3 : 2;
    }

    private void openStopConfirm(String questId) {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null) {
            return;
        }
        QuestDefinition quest = QuestClientState.getQuestDefinitions().get(questId);
        Component questTitle = quest == null || quest.titleKey() == null
                ? Component.literal(questId)
                : Component.translatable(Objects.requireNonNull(quest.titleKey()));
        Component title = Component.translatable("screen.shuga_quests.stop_confirm.title");
        Component message = Component.translatable("screen.shuga_quests.stop_confirm.message", questTitle);
        QuestConfirmOverlayScreen confirmScreen = new QuestConfirmOverlayScreen(this, confirmed -> {
            if (confirmed) {
                ClientPacketDistributor.sendToServer(new QuestStopPacket(questId));
            }
        }, title, message);
        minecraft.setScreen(confirmScreen);
    }

    private void openStartOverrideConfirm(String activeQuestId, String newQuestId) {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null) {
            return;
        }
        QuestDefinition activeQuest = QuestClientState.getQuestDefinitions().get(activeQuestId);
        Component activeTitle = activeQuest == null || activeQuest.titleKey() == null
                ? Component.literal(activeQuestId)
                : Component.translatable(Objects.requireNonNull(activeQuest.titleKey()));
        Component title = Component.translatable("screen.shuga_quests.start_confirm.title");
        Component message = Component.translatable("screen.shuga_quests.start_confirm.message", activeTitle);
        QuestConfirmOverlayScreen confirmScreen = new QuestConfirmOverlayScreen(this, confirmed -> {
            if (confirmed) {
                ClientPacketDistributor.sendToServer(new QuestStartPacket(newQuestId));
            }
        }, title, message);
        minecraft.setScreen(confirmScreen);
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

    private Component getPrerequisiteLine(String prerequisiteId) {
        QuestDefinition prerequisite = QuestClientState.getQuestDefinitions().get(prerequisiteId);
        Component title;
        if (prerequisite == null || prerequisite.titleKey() == null) {
            title = Component.literal(prerequisiteId);
        } else if (isQuestLocked(prerequisite)) {
            title = HIDDEN_TEXT;
        } else {
            title = Component.translatable(Objects.requireNonNull(prerequisite.titleKey()));
        }
        QuestProgress prerequisiteProgress = QuestClientState.getQuestProgress().get(prerequisiteId);
        Component status = prerequisiteProgress != null && prerequisiteProgress.isCompleted()
                ? Component.translatable("screen.shuga_quests.status.complete")
                : Component.translatable("screen.shuga_quests.status.not_started");
        return Component.literal("- ").append(title).append(Component.literal(" (")).append(status).append(Component.literal(")"));
    }

    private boolean arePrerequisitesMet(QuestDefinition quest) {
        if (quest.prerequisites().isEmpty()) {
            return true;
        }
        for (String prerequisiteId : quest.prerequisites()) {
            QuestProgress prerequisiteProgress = QuestClientState.getQuestProgress().get(prerequisiteId);
            if (prerequisiteProgress == null || !prerequisiteProgress.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private boolean isQuestLocked(QuestDefinition quest) {
        if (quest.prerequisites().isEmpty()) {
            return false;
        }
        for (String prerequisiteId : quest.prerequisites()) {
            QuestDefinition prerequisite = QuestClientState.getQuestDefinitions().get(prerequisiteId);
            if (prerequisite == null) {
                return true;
            }
            if (!prerequisite.prerequisites().isEmpty() && !arePrerequisitesMet(prerequisite)) {
                return true;
            }
        }
        return false;
    }

    private String getObjectiveTranslationKey(QuestObjective objective) {
        return "quest.objective." + objective.id();
    }

    private int getListLineHeight(Font font) {
        float titleScale = getListTitleScale(font);
        int scaledLineHeight = (int) Math.ceil(font.lineHeight * titleScale);
        return scaledLineHeight + 3;
    }

    private float getListTitleScale(Font font) {
        return (font.lineHeight + 2.0f) / font.lineHeight;
    }

    private int getVisibleListLines(int listY, int lineHeight) {
        int listHeight = Math.max(0, this.height - listY - LIST_BOTTOM_PADDING);
        int lines = listHeight / Math.max(1, lineHeight);
        return Math.max(1, lines);
    }

    private int getListTopY(Font font) {
        int baseListY = LIST_TOP_BASE_Y;
        int lineHeight = getListLineHeight(font);
        return baseListY + getHeaderHeight(lineHeight);
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private int getSelectedIndex() {
        if (selectedQuestId.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < filteredQuests.size(); i++) {
            if (filteredQuests.get(i).id().equals(selectedQuestId)) {
                return i;
            }
        }
        return 0;
    }

    private void ensureSelectedVisible(String questId) {
        int selectedIndex = getEntryIndex(questId);
        Font font = Objects.requireNonNull(this.font);
        int lineHeight = getListLineHeight(font);
        int visibleLines = getVisibleListLines(getListTopY(font), lineHeight);
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleLines) {
            scrollOffset = Math.max(0, selectedIndex - visibleLines + 1);
        }
    }

    private int getEntryIndex(String questId) {
        if (questId == null || questId.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < listEntries.size(); i++) {
            QuestListEntry entry = listEntries.get(i);
            if (!entry.isHeader() && !entry.isInfo() && !entry.isSpacer() && entry.quest().id().equals(questId)) {
                return i;
            }
        }
        return 0;
    }

    private QuestListEntry getStickyHeaderEntry() {
        if (listEntries.isEmpty()) {
            return null;
        }
        int start = Math.min(scrollOffset, listEntries.size() - 1);
        if (start < 0) {
            return null;
        }
        if (listEntries.get(start).isHeader()) {
            return null;
        }
        for (int i = start; i >= 0; i--) {
            QuestListEntry entry = listEntries.get(i);
            if (entry.isHeader()) {
                return entry;
            }
        }
        return null;
    }

    private void drawSectionHeader(GuiGraphics graphics, Font font, int listX, int y, int lineHeight, float titleScale,
                                   Component header, boolean sticky) {
        int left = listX - LIST_HIGHLIGHT_PAD;
        int right = listX + LIST_WIDTH - LIST_HIGHLIGHT_PAD;
        int height = getHeaderHeight(lineHeight);
        int background = sticky ? 0x90303030 : 0x80202020;
        int textColor = 0xFFAEDCFF;
        int textIndent = 4;
        graphics.fill(left, y, right, y + height - 1, background);
        graphics.fill(left, y, left + LIST_HEADER_STRIPE_WIDTH, y + height - 1, 0xFF6FB6FF);
        int scaledLineHeight = (int) Math.ceil(font.lineHeight * titleScale);
        int textY = y + (height - scaledLineHeight) / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(listX + textIndent, textY);
        graphics.pose().scale(titleScale, titleScale);
        Component safeHeader = Objects.requireNonNull(header).copy().withStyle(ChatFormatting.BOLD);
        graphics.drawString(font, safeHeader, 1, 1, 0xFF000000);
        graphics.drawString(font, safeHeader, 0, 0, textColor);
        graphics.pose().popMatrix();
    }

}
