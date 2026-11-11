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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/**
 * Easy Disc Creator's small box over its window: what it asks (put in a blank, take the disc out), with OK and Cancel,
 * or why it will not copy, with OK alone.
 */
final class EasyDiscCreatorPrompt implements IDesktopApp {

    private final EasyDiscCreatorApp owner;
    private final Text message;
    private final boolean asking;
    private final Panel root = new Panel();
    private final Button ok;
    private final Button cancel;
    private OsSkin skin = OsSkin.fallback();

    private static final int WIDTH = 240;
    private static final int HEIGHT = 70;
    private static final int MARGIN = 10;
    private static final int LINE = 10;
    private static final int BUTTON_W = 54;

    private EasyDiscCreatorPrompt(final EasyDiscCreatorApp owner, final Text message, final boolean asking) {
        this.owner = owner;
        this.message = message;
        this.asking = asking;
        this.ok = root.add(new Button(GameText.resolve(EasyDiscCreatorTexts.OK), this::confirm).setPrimary(true));
        this.cancel = root.add(new Button(GameText.resolve(EasyDiscCreatorTexts.CANCEL), this::refuse));
        this.cancel.setVisible(asking);
    }

    /** Opens the box over {@code owner}: a question with OK and Cancel, or a refusal with OK. */
    static void open(final EasyDiscCreatorApp owner, final Text message, final boolean asking) {
        ActiveDesktop.openDialogFor(owner, new EasyDiscCreatorPrompt(owner, message, asking));
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
        return HEIGHT + DesktopWindow.TITLE_H;
    }

    @Override
    public void applySkin(final OsSkin value) {
        this.skin = value;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(x, y, x + width, y + height, skin.windowBg());
        final List<String> lines = font.getSplitter().splitLines(GameText.resolve(message), width - 2 * MARGIN,
                Style.EMPTY).stream().map(FormattedText::getString).toList();
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(g, font, lines.get(i), x + MARGIN, y + MARGIN + i * LINE, skin.text());
        }
        final int by = y + height - 18;
        if (asking) {
            ok.setBounds(x + width - MARGIN - 2 * BUTTON_W - 4, by, BUTTON_W, 13);
            cancel.setBounds(x + width - MARGIN - BUTTON_W, by, BUTTON_W, 13);
        } else {
            ok.setBounds(x + (width - BUTTON_W) / 2, by, BUTTON_W, 13);
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
