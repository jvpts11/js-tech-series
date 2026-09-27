/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Pure layout of the Command Prompt window: a header bar, the console panel with the scrollback on the glass
 * below it, and the input strip at the foot. A bare terminal (MC-DOS, the Linux TTY, MC-NET with nothing
 * installed) draws none of that chrome and is the glass alone, so it carries its own, narrower top margin.
 * The window is resizable, so every position is worked out from the size it opens at rather than fixed; what
 * is recorded here is the glass, its margins on every side, and {@link #LINE_STEP}, the step from one row of
 * it to the next, not the scrollback itself, which is the machine's own output and not this screen's geometry.
 */
public final class CommandPromptLayout {

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    /** The header bar's own height, drawn by the theme. */
    public static final int HEADER_H = 17;

    public static final int TITLE_X = 12;
    public static final int TITLE_Y = 11;
    /** How far the program name stands from the right edge, at the same height as the title. */
    public static final int PROGRAM_RIGHT_MARGIN = 10;

    /** The console panel: where it starts under the header, and how far it stops short of the foot. */
    public static final int CONSOLE_X = 6;
    public static final int CONSOLE_TOP = 26;
    public static final int CONSOLE_BOTTOM_MARGIN = 22;

    /** The input strip at the foot, and the line that opens it. */
    public static final int INPUT_STRIP_TOP_MARGIN = 20;
    public static final int INPUT_STRIP_BOTTOM_MARGIN = 8;
    public static final int INPUT_LINE_BOTTOM_MARGIN = 19;

    /** Where the glass itself starts on each side, so a row's cell lines up whichever way the prompt is drawn. */
    public static final int GLASS_LEFT = 10;
    public static final int GLASS_RIGHT_MARGIN = 10;
    public static final int GLASS_BOTTOM_MARGIN = 23;
    /** How far down the glass starts: under the header of a windowed prompt, or near the very top of a bare one. */
    public static final int WINDOWED_GLASS_TOP = 27;
    public static final int BARE_GLASS_TOP = 8;

    /** The usage hint beside the input line, and the key hint at the very foot; both drawn at the small scale. */
    public static final int USAGE_BOTTOM_MARGIN = 17;
    public static final int KEYS_X = 10;
    public static final int KEYS_BOTTOM_MARGIN = 7;
    /** The scale {@code JsTechTheme.textS}/{@code textSRight} draw at, shared by every era's theme. */
    public static final float SMALL_TEXT_SCALE = 0.75f;
    /**
     * How far above the bottom of the scrollback (the glass's foot, less the rows being typed) the "scrolled +n" mark
     * is written.
     */
    public static final int SCROLLED_ABOVE = 7;

    /** The margin an editor that has taken the glass over is drawn at, on every side. */
    public static final int EDITOR_MARGIN = 8;

    /** The step from one row of the glass to the next, at the text's normal size. */
    public static final int LINE_STEP = 9;

    private CommandPromptLayout() {
    }

    public static int consoleBottom(final int height) {
        return height - CONSOLE_BOTTOM_MARGIN;
    }

    public static int inputStripTop(final int height) {
        return height - INPUT_STRIP_TOP_MARGIN;
    }

    public static int inputStripBottom(final int height) {
        return height - INPUT_STRIP_BOTTOM_MARGIN;
    }

    public static int inputLineBottom(final int height) {
        return height - INPUT_LINE_BOTTOM_MARGIN;
    }

    /** Where the scrollback starts down the glass, which a bare terminal (no header above it) starts higher. */
    public static int glassTop(final boolean bare) {
        return bare ? BARE_GLASS_TOP : WINDOWED_GLASS_TOP;
    }

    public static int glassBottom(final int height) {
        return height - GLASS_BOTTOM_MARGIN;
    }

    public static int usageY(final int height) {
        return height - USAGE_BOTTOM_MARGIN;
    }

    public static int keysY(final int height) {
        return height - KEYS_BOTTOM_MARGIN;
    }

    /**
     * A windowed prompt's chrome at the given size: the header, the console panel and the input strip, plus
     * the title, program name and key-hint captions. A bare terminal draws none of this (its only furniture
     * is the glass itself, which every size is validated against by construction), so it is the empty layout.
     */
    public static GuiLayout layout(final int width, final int height, final boolean bare) {
        final GuiLayout l = new GuiLayout(width, height);
        if (bare) {
            return l;
        }
        l.box("header", HEADER_X, HEADER_Y, width - 2 * HEADER_X, HEADER_H);
        l.box("console", CONSOLE_X, CONSOLE_TOP, width - 2 * CONSOLE_X, consoleBottom(height) - CONSOLE_TOP);
        l.box("inputStrip", CONSOLE_X, inputStripTop(height), width - 2 * CONSOLE_X,
                inputStripBottom(height) - inputStripTop(height));
        l.text("title", TITLE_X, TITLE_Y, 20, 1.0f);
        // The program name is written at the title's own height, right-aligned instead of left.
        l.text("program", width - PROGRAM_RIGHT_MARGIN - Math.round(16 * GuiLayout.GLYPH_WIDTH), TITLE_Y, 16, 1.0f);
        // 57 characters is the longer of KEYS and KEYS_BUSY, so the glass at its smallest still has room for it.
        l.text("keys", KEYS_X, keysY(height), 57, SMALL_TEXT_SCALE);
        return l;
    }
}
