// root/src/main/java/net/sugar27/quests/client/gui/QuestConfirmOverlayScreen.java

package net.sugar27.quests.client.gui;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

// Confirmation dialog rendered on top of a parent screen.
public class QuestConfirmOverlayScreen extends Screen {
    private static final int MESSAGE_MAX_ROWS = 15;
    private static final int MESSAGE_LINE_SPACING = 3;
    private static final int PANEL_COLOR = 0xC0000000;
    private final Screen parent;
    private final Component message;
    @Nullable
    private StringWidget titleWidget;
    @Nullable
    private MultiLineTextWidget messageWidget;
    protected LinearLayout layout = LinearLayout.vertical().spacing(8);
    protected Component yesButtonComponent;
    protected Component noButtonComponent;
    @Nullable
    protected Button yesButton;
    @Nullable
    protected Button noButton;
    private int delayTicker;
    protected final BooleanConsumer callback;

    public QuestConfirmOverlayScreen(Screen parent, BooleanConsumer callback, Component title, Component message) {
        this(parent, callback, title, message, CommonComponents.GUI_YES, CommonComponents.GUI_NO);
    }

    public QuestConfirmOverlayScreen(Screen parent, BooleanConsumer callback, Component title, Component message, Component yesButton, Component noButton) {
        super(title);
        this.parent = parent;
        this.callback = callback;
        this.message = message;
        this.yesButtonComponent = yesButton;
        this.noButtonComponent = noButton;
    }

    @Override
    protected void init() {
        super.init();
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.titleWidget = this.layout.addChild(new StringWidget(this.title, this.font));
        this.titleWidget.setAlpha(0.0F);
        this.messageWidget = this.layout.addChild(new MultiLineTextWidget(this.message, this.font)
                .setMaxWidth(this.width - 50)
                .setMaxRows(MESSAGE_MAX_ROWS)
                .setCentered(true));
        this.messageWidget.setAlpha(0.0F);
        LinearLayout linearlayout = this.layout.addChild(LinearLayout.horizontal().spacing(4));
        linearlayout.defaultCellSetting().paddingTop(16);
        this.addButtons(linearlayout);
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        if (this.delayTicker > 0) {
            if (this.yesButton != null) {
                this.yesButton.active = false;
            }
            if (this.noButton != null) {
                this.noButton.active = false;
            }
        }
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    protected void addButtons(LinearLayout layout) {
        this.yesButton = layout.addChild(Button.builder(this.yesButtonComponent, button -> handleChoice(true)).build());
        this.noButton = layout.addChild(Button.builder(this.noButtonComponent, button -> handleChoice(false)).build());
    }

    public void setDelay(int ticksUntilEnable) {
        this.delayTicker = ticksUntilEnable;
        if (this.yesButton != null) {
            this.yesButton.active = false;
        }
        if (this.noButton != null) {
            this.noButton.active = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.delayTicker > 0 && --this.delayTicker == 0) {
            if (this.yesButton != null) {
                this.yesButton.active = true;
            }
            if (this.noButton != null) {
                this.noButton.active = true;
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            handleChoice(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.parent.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.parent.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0x70000000);
        renderPanel(graphics);
        renderTextShadow(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderPanel(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, PANEL_COLOR);
    }

    private void renderTextShadow(GuiGraphics graphics) {
        if (this.font == null) {
            return;
        }
        if (this.titleWidget != null) {
            int titleWidth = this.font.width(this.title);
            int titleX = this.titleWidget.getX() + (this.titleWidget.getWidth() - titleWidth) / 2;
            int titleY = this.titleWidget.getY() + (this.titleWidget.getHeight() - this.font.lineHeight) / 2;
            graphics.drawString(this.font, this.title, titleX, titleY, 0xFFFFFFFF, true);
        }
        if (this.messageWidget != null) {
            int maxWidth = this.messageWidget.getWidth();
            int centerX = this.messageWidget.getX() + maxWidth / 2;
            int y = this.messageWidget.getY();
            List<FormattedCharSequence> lines = this.font.split(this.message, maxWidth);
            int count = Math.min(lines.size(), MESSAGE_MAX_ROWS);
            for (int i = 0; i < count; i++) {
                FormattedCharSequence line = lines.get(i);
                int lineX = centerX - this.font.width(line) / 2;
                graphics.drawString(this.font, line, lineX, y, 0xFFFFFFFF, true);
                y += this.font.lineHeight + MESSAGE_LINE_SPACING;
            }
        }
    }

    private void handleChoice(boolean confirmed) {
        this.callback.accept(confirmed);
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
