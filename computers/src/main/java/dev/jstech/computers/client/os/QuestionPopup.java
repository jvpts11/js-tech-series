/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.UiContext;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * A modal dialog over the whole desktop that asks before something is done that cannot be undone, or that only
 * tells: a warning with Yes and No, or a note with OK. Nothing behind it takes a click while it is up.
 *
 * <p>Yes and Enter do the thing and close the dialog; No and Escape close it and leave everything as it was. A note
 * is closed by OK, Enter or Escape. The message may run over several lines, each broken where it says so and wrapped
 * to the dialog's width.
 */
public final class QuestionPopup extends Popup {

    private final List<Label> lines = new ArrayList<>();
    private final Button first;
    @Nullable
    private final Button second;
    @Nullable
    private final Runnable yes;

    private static final int WIDTH = 214;
    private static final int TITLE_H = 14;
    private static final int PADDING = 8;
    private static final int ICON = 18;
    private static final int LINE_H = 10;
    private static final int BTN_W = 52;
    private static final int BTN_H = 16;
    private static final int BTN_GAP = 8;
    private static final int MAX_TEXT_WIDTH = WIDTH - PADDING * 2 - ICON - 6;
    private static final int WARNING = 0xFFF2C230;
    private static final int WARNING_EDGE = 0xFF8A6A00;
    private static final int NOTE = 0xFF2F6FD0;

    /**
     * A dialog that asks, when {@code yes} is what answering Yes does, or that only tells, when it is null.
     */
    QuestionPopup(final String title, final String message, final Font font, @Nullable final Runnable yes) {
        super(title, WIDTH, 0);
        this.yes = yes;
        final List<String> wrapped = wrap(message, font);
        for (final String line : wrapped) {
            this.lines.add(add(new Label(line)));
        }
        if (yes == null) {
            this.first = add(new Button("OK", this::close).setPrimary(true));
            this.second = null;
        } else {
            this.first = add(new Button("Yes", this::answerYes).setPrimary(true));
            this.second = add(new Button("No", this::close));
        }
        final int bodyH = PADDING + Math.max(ICON, wrapped.size() * LINE_H) + PADDING + BTN_H + PADDING;
        setPreferredSize(WIDTH, TITLE_H + bodyH);
        setDim(0x80000000);
        setCloseOnOutsideClick(false);
        setLayouter(this::layoutContent);
        open();
    }

    /** Whether it asks, rather than only telling. */
    public boolean asks() {
        return this.yes != null;
    }

    /** What it says, line by line, for a test to read. */
    public List<String> text() {
        final List<String> out = new ArrayList<>();
        for (final Label line : this.lines) {
            out.add(line.text());
        }
        return out;
    }

    /** The desktop-local middle of the first button, Yes on a question and OK on a note, where a test clicks it. */
    public int[] firstButtonCentre() {
        return new int[] {this.first.x() + this.first.width() / 2, this.first.y() + this.first.height() / 2};
    }

    /** Answers Yes, as a click on it would. */
    public void answerYes() {
        close();
        if (this.yes != null) {
            this.yes.run();
        }
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        super.render(g, ctx);
        if (this.yes != null) {
            drawWarning(g, x() + PADDING, y() + TITLE_H + PADDING);
        } else {
            drawNote(g, x() + PADDING, y() + TITLE_H + PADDING);
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.yes != null) {
                answerYes();
            } else {
                close();
            }
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void layoutContent(final Popup p) {
        final int iconY = p.y() + TITLE_H + PADDING;
        final int textX = p.x() + PADDING + ICON + 6;
        int textY = iconY + (this.lines.size() == 1 ? (ICON - 8) / 2 : 0);
        for (final Label line : this.lines) {
            line.setBounds(textX, textY, p.right() - PADDING - textX, 8);
            textY += LINE_H;
        }
        final int buttonsY = p.bottom() - PADDING - BTN_H;
        if (this.second == null) {
            this.first.setBounds(p.x() + (p.width() - BTN_W) / 2, buttonsY, BTN_W, BTN_H);
        } else {
            final int left = p.x() + (p.width() - BTN_W * 2 - BTN_GAP) / 2;
            this.first.setBounds(left, buttonsY, BTN_W, BTN_H);
            this.second.setBounds(left + BTN_W + BTN_GAP, buttonsY, BTN_W, BTN_H);
        }
    }

    /** A warning sign, as the question the Windows of that time asked before a delete wore it. */
    private static void drawWarning(final GuiGraphics g, final int ox, final int oy) {
        for (int row = 0; row < 15; row++) {
            final int half = row / 2 + 1;
            final int cx = ox + ICON / 2;
            g.fill(cx - half, oy + 2 + row, cx + half, oy + 3 + row, row == 14 ? WARNING_EDGE : WARNING);
        }
        g.fill(ox + ICON / 2 - 1, oy + 7, ox + ICON / 2 + 1, oy + 12, 0xFF000000);
        g.fill(ox + ICON / 2 - 1, oy + 13, ox + ICON / 2 + 1, oy + 15, 0xFF000000);
    }

    /** A round note sign, blue with an i in it. */
    private static void drawNote(final GuiGraphics g, final int ox, final int oy) {
        final int[] half = {4, 6, 7, 8, 9, 9, 9, 9, 8, 7, 6, 4};
        final int cx = ox + ICON / 2;
        for (int i = 0; i < half.length; i++) {
            g.fill(cx - half[i], oy + 3 + i, cx + half[i], oy + 4 + i, NOTE);
        }
        g.fill(cx - 1, oy + 5, cx + 1, oy + 7, 0xFFFFFFFF);
        g.fill(cx - 1, oy + 8, cx + 1, oy + 13, 0xFFFFFFFF);
    }

    /** Breaks the message where it says so, then wraps each part to the dialog's width on its spaces. */
    private static List<String> wrap(final String message, final Font font) {
        final List<String> out = new ArrayList<>();
        for (final String part : message.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (final String word : part.split(" ")) {
                final String candidate = line.length() == 0 ? word : line + " " + word;
                if (font.width(candidate) > MAX_TEXT_WIDTH && line.length() > 0) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            out.add(line.toString());
        }
        return out;
    }
}
