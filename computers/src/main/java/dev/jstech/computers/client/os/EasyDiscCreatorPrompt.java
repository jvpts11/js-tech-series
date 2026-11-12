/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Easy Disc Creator's message box over its window, as the desktop's own boxes are laid out: a picture at the left,
 * the words beside it, the keys under them. A question (put in a blank, take the disc out) shows what it asks for and
 * has OK and Cancel at the right; a refusal shows the error mark and has OK alone, in the middle.
 */
final class EasyDiscCreatorPrompt implements IDesktopApp {

    private final EasyDiscCreatorApp owner;
    private final boolean asking;
    private final ItemStack picture;
    private final List<String> lines;
    private final Panel root = new Panel();
    private final Button ok;
    private final Button cancel;
    private OsSkin skin = OsSkin.fallback();

    private static final int WIDTH = 300;
    private static final int LEAST_HEIGHT = 76;
    private static final int MARGIN = 11;
    private static final int TEXT_X = 41;
    private static final int TEXT_Y = 12;
    private static final int LINE = 11;
    /* The keys under the words: their size, how far up from the bottom, and the room the words leave them. */
    private static final int BUTTON_W = 56;
    private static final int BUTTON_H = 15;
    private static final int BUTTONS_DY = 24;
    private static final int UNDER_WORDS = 34;
    /* The picture of what is asked for, an item a quarter larger than in a slot. */
    private static final float PICTURE_SCALE = 1.25F;
    /** The box up now, for a test; null when none is. */
    @Nullable
    private static EasyDiscCreatorPrompt showing;

    private EasyDiscCreatorPrompt(final EasyDiscCreatorApp owner, final Text message, final boolean asking,
                                  final ItemStack picture) {
        this.owner = owner;
        this.asking = asking;
        this.picture = picture;
        this.lines = Minecraft.getInstance().font.getSplitter().splitLines(GameText.resolve(message),
                WIDTH - TEXT_X - MARGIN, Style.EMPTY).stream().map(FormattedText::getString).toList();
        this.ok = root.add(new Button(GameText.resolve(EasyDiscCreatorTexts.OK), this::confirm).setPrimary(true));
        this.cancel = root.add(new Button(GameText.resolve(EasyDiscCreatorTexts.CANCEL), this::refuse));
        this.cancel.setVisible(asking);
    }

    /**
     * Opens the box over {@code owner}: a question with OK and Cancel and the picture of what it asks for, or a
     * refusal with OK and the error mark when {@code picture} is empty.
     */
    static void open(final EasyDiscCreatorApp owner, final Text message, final boolean asking,
                     final ItemStack picture) {
        showing = new EasyDiscCreatorPrompt(owner, message, asking, picture);
        ActiveDesktop.openDialogFor(owner, showing);
    }

    /** The middle of the OK key of the box up now, or null, for a test. */
    @Nullable
    static int[] okPointShowing() {
        return showing == null ? null : new int[] {showing.ok.x() + showing.ok.width() / 2,
                showing.ok.y() + showing.ok.height() / 2};
    }

    @Override
    public String title() {
        return GameText.resolve(EasyDiscCreatorTexts.NAME);
    }

    @Override
    public int defaultWidth() {
        return WIDTH;
    }

    @Override
    public int defaultHeight() {
        return Math.max(LEAST_HEIGHT, TEXT_Y + lines.size() * LINE + UNDER_WORDS) + DesktopWindow.TITLE_H;
    }

    @Override
    public void applySkin(final OsSkin value) {
        this.skin = value;
    }

    @Override
    public void onClosed() {
        if (showing == this) {
            showing = null;
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(x, y, x + width, y + height, skin.windowBg());
        if (picture.isEmpty()) {
            DesktopPopup.drawErrorIcon(g, x + MARGIN, y + MARGIN);
        } else {
            g.pose().pushPose();
            g.pose().translate(x + MARGIN, y + MARGIN, 0);
            g.pose().scale(PICTURE_SCALE, PICTURE_SCALE, 1F);
            g.renderItem(picture, 0, 0);
            g.pose().popPose();
        }
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(g, font, lines.get(i), x + TEXT_X, y + TEXT_Y + i * LINE, skin.text());
        }
        final int by = y + height - BUTTONS_DY;
        if (asking) {
            ok.setBounds(x + width - MARGIN - 2 * BUTTON_W - 7, by, BUTTON_W, BUTTON_H);
            cancel.setBounds(x + width - MARGIN - BUTTON_W, by, BUTTON_W, BUTTON_H);
        } else {
            ok.setBounds(x + (width - BUTTON_W) / 2, by, BUTTON_W, BUTTON_H);
        }
        root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseClicked(mouseX, mouseY, button);
    }

    private void confirm() {
        if (asking) {
            owner.goOn();
        }
        ActiveDesktop.closeDialog(this);
    }

    private void refuse() {
        owner.stop();
        ActiveDesktop.closeDialog(this);
    }
}
