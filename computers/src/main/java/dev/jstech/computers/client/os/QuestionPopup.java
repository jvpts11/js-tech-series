/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
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
 *
 * <p>On the classic Quince systems it is their alert (computers/docs/design/quince.md): the double frame or the
 * Platinum box with no title, the stop sign on a note and the caution sign on a question, the words beside the sign,
 * and the round buttons along the right of its foot, nothing dimmed behind it.
 */
@PaletteHolder
public final class QuestionPopup extends Popup {

    private final List<Label> lines = new ArrayList<>();
    private final Button first;
    @Nullable
    private final Button second;
    @Nullable
    private final Runnable yes;
    /** The classic Quince face it wears, or null for the dialog every other desktop has. */
    @Nullable
    private final QuinceLook classic;

    private static final int WIDTH = 214;
    private static final int TITLE_H = 14;
    private static final int PADDING = 8;
    private static final int ICON = 18;
    private static final int LINE_H = 10;
    private static final int BTN_W = 52;
    private static final int BTN_H = 16;
    private static final int BTN_GAP = 8;
    private static final int MAX_TEXT_WIDTH = WIDTH - PADDING * 2 - ICON - 6;
    /* The classic alert as the approved mock lays it: its width, the sign's inset, the words, and the buttons. */
    private static final int CLASSIC_WIDTH = 244;
    private static final int CLASSIC_MIN_HEIGHT = 80;
    private static final int CLASSIC_INSET = 14;
    private static final int CLASSIC_TEXT_X = 42;
    private static final int CLASSIC_TEXT_WIDTH = 180;
    private static final int CLASSIC_LINE_H = 11;
    private static final int CLASSIC_BUTTONS_UP = 24;
    private static final int CLASSIC_FIRST_RIGHT = 66;
    private static final int CLASSIC_SECOND_RIGHT = 128;
    private static final int CLASSIC_BUTTON_W = 50;
    private static final int CLASSIC_BUTTON_H = 13;
    private static final int PLATINUM_BUTTON_H = 14;
    /** The question's own colours, {@code jsc:desktop/question}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "desktop/question",
            new Colours(0x80000000, 0xFFF2C230, 0xFF8A6A00, 0xFF000000, 0xFF2F6FD0, 0xFFFFFFFF));

    /**
     * A dialog that asks, when {@code yes} is what answering Yes does, or that only tells, when it is null; in the
     * face of a classic Quince system when {@code classic} is one.
     */
    QuestionPopup(final String title, final String message, final Font font, @Nullable final Runnable yes,
                  @Nullable final QuinceLook classic) {
        super(classic == null ? title : "", classic == null ? WIDTH : CLASSIC_WIDTH, 0);
        this.yes = yes;
        this.classic = classic;
        final List<String> wrapped = wrap(message, font, classic == null ? MAX_TEXT_WIDTH : CLASSIC_TEXT_WIDTH);
        for (final String line : wrapped) {
            this.lines.add(add(new Label(line)));
        }
        if (yes == null) {
            this.first = add(new Button(GameText.resolve(DesktopTexts.QUESTION_OK), this::close).setPrimary(true));
            this.second = null;
        } else {
            this.first = add(new Button(GameText.resolve(DesktopTexts.QUESTION_YES), this::answerYes)
                    .setPrimary(true));
            this.second = add(new Button(GameText.resolve(DesktopTexts.QUESTION_NO), this::close));
        }
        if (classic == null) {
            final int bodyH = PADDING + Math.max(ICON, wrapped.size() * LINE_H) + PADDING + BTN_H + PADDING;
            setPreferredSize(WIDTH, TITLE_H + bodyH);
            setDim(PALETTE.get().dim());
            setLayouter(this::layoutContent);
        } else {
            setPreferredSize(CLASSIC_WIDTH, Math.max(CLASSIC_MIN_HEIGHT,
                    CLASSIC_INSET + wrapped.size() * CLASSIC_LINE_H + 6 + CLASSIC_BUTTONS_UP));
            setDim(0);
            setLayouter(this::layoutClassic);
        }
        setCloseOnOutsideClick(false);
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

    /** The desktop-local middle of the second button, No on a question, where a test clicks it; null on a note. */
    public int[] secondButtonCentre() {
        return this.second == null ? null
                : new int[] {this.second.x() + this.second.width() / 2, this.second.y() + this.second.height() / 2};
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
        if (this.classic != null) {
            renderClassic(g, ctx.font(), this.classic);
            return;
        }
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

    /* The classic alert's words beside its sign, and its buttons along the right of its foot, the first the default. */
    private void layoutClassic(final Popup p) {
        int textY = p.y() + CLASSIC_INSET;
        for (final Label line : this.lines) {
            line.setBounds(p.x() + CLASSIC_TEXT_X, textY, CLASSIC_TEXT_WIDTH, 8);
            textY += CLASSIC_LINE_H;
        }
        final int buttonsY = p.bottom() - CLASSIC_BUTTONS_UP;
        final int height = classicButtonHeight();
        this.first.setBounds(p.right() - CLASSIC_FIRST_RIGHT, buttonsY, CLASSIC_BUTTON_W, height);
        if (this.second != null) {
            this.second.setBounds(p.right() - CLASSIC_SECOND_RIGHT, buttonsY, CLASSIC_BUTTON_W, height);
        }
    }

    private void renderClassic(final GuiGraphics g, final Font font, final QuinceLook look) {
        final QuinceLook.Colours c = look.colours();
        final boolean platinum = look == QuinceLook.PLATINUM;
        QuinceArt.dialog(g, look, x(), y(), right(), bottom());
        final String[] sign = this.yes == null ? platinum ? QuinceArt.STOP_RED : QuinceArt.STOP
                : platinum ? QuinceArt.CAUTION_YELLOW : QuinceArt.CAUTION;
        QuinceArt.sprite(g, sign, x() + CLASSIC_INSET, y() + CLASSIC_INSET, c.ink());
        for (final Label line : this.lines) {
            Draw.text(g, font, line.text(), line.x(), line.y(), c.ink());
        }
        classicButton(g, font, look, this.first, true);
        if (this.second != null) {
            classicButton(g, font, look, this.second, false);
        }
    }

    private static void classicButton(final GuiGraphics g, final Font font, final QuinceLook look, final Button button,
                                      final boolean primary) {
        QuinceArt.button(g, look, button.x(), button.y(), button.width(), button.height(), primary, false);
        Draw.textCentered(g, font, button.label(), button.x() + button.width() / 2, button.y() + 3,
                QuinceArt.buttonInk(look, primary));
    }

    private int classicButtonHeight() {
        return this.classic == QuinceLook.PLATINUM ? PLATINUM_BUTTON_H : CLASSIC_BUTTON_H;
    }

    /** A warning sign, as the question the Windows of that time asked before a delete wore it. */
    private static void drawWarning(final GuiGraphics g, final int ox, final int oy) {
        for (int row = 0; row < 15; row++) {
            final int half = row / 2 + 1;
            final int cx = ox + ICON / 2;
            g.fill(cx - half, oy + 2 + row, cx + half, oy + 3 + row,
                    row == 14 ? PALETTE.get().warningEdge() : PALETTE.get().warning());
        }
        g.fill(ox + ICON / 2 - 1, oy + 7, ox + ICON / 2 + 1, oy + 12, PALETTE.get().warningMark());
        g.fill(ox + ICON / 2 - 1, oy + 13, ox + ICON / 2 + 1, oy + 15, PALETTE.get().warningMark());
    }

    /** A round note sign, blue with an i in it. */
    private static void drawNote(final GuiGraphics g, final int ox, final int oy) {
        final int[] half = {4, 6, 7, 8, 9, 9, 9, 9, 8, 7, 6, 4};
        final int cx = ox + ICON / 2;
        for (int i = 0; i < half.length; i++) {
            g.fill(cx - half[i], oy + 3 + i, cx + half[i], oy + 4 + i, PALETTE.get().note());
        }
        g.fill(cx - 1, oy + 5, cx + 1, oy + 7, PALETTE.get().noteMark());
        g.fill(cx - 1, oy + 8, cx + 1, oy + 13, PALETTE.get().noteMark());
    }

    /** Breaks the message where it says so, then wraps each part to {@code width} on its spaces. */
    private static List<String> wrap(final String message, final Font font, final int width) {
        final List<String> out = new ArrayList<>();
        for (final String part : message.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (final String word : part.split(" ")) {
                final String candidate = line.length() == 0 ? word : line + " " + word;
                if (font.width(candidate) > width && line.length() > 0) {
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

    /**
     * The question's colours: what dims the desktop behind it, the warning sign and its edge and mark, and the
     * note sign and its mark.
     */
    private record Colours(int dim, int warning, int warningEdge, int warningMark, int note, int noteMark) {
    }
}
