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
 * The geometry of the Open with chooser, in desktop units: the title, the question and the line under it, a well
 * listing four programs at a time, and the two buttons at the bottom right. The chooser and its test both read it,
 * so what the test proves clean is what the chooser draws.
 */
public final class OpenWithLayout {

    public static final int WIDTH = 204;
    public static final int HEIGHT = 156;
    /** The margin around the content, below the title. */
    public static final int PAD = 8;
    public static final int QUESTION_Y = 16;
    public static final int NOTE_Y = 28;
    public static final int LINE_H = 8;
    /** Where the well the programs are listed in starts. */
    public static final int LIST_Y = 42;
    public static final int ROW_H = 18;
    /** How many programs show at once; more scroll. */
    public static final int ROWS = 4;
    /** The well's own border, inside which the rows sit. */
    public static final int WELL_INSET = 2;
    public static final int LIST_H = ROW_H * ROWS + WELL_INSET * 2;
    /** Where a row's icon and its name start, from the row's left. */
    public static final int ICON_X = 4;
    public static final int ROW_TEXT_X = 25;
    public static final int BUTTON_H = 16;
    public static final int BUTTON_Y = HEIGHT - PAD - BUTTON_H;
    /** Wide enough for "Only this time" (65 pixels in the game's font) and its margins. */
    public static final int ONCE_W = 76;
    /** Wide enough for "Always" (33 pixels) and its margins. */
    public static final int ALWAYS_W = 48;
    public static final int BUTTON_GAP = 6;

    private OpenWithLayout() {
    }

    /** How wide the question and the well are. */
    public static int listW() {
        return WIDTH - PAD * 2;
    }

    /**
     * How wide the line under the question may run. It is written small, and it runs half a margin past the well
     * so the sentence about a three-letter extension (222 units of the game's font) fits whole.
     */
    public static int noteW() {
        return WIDTH - PAD - PAD / 2;
    }

    public static int alwaysX() {
        return WIDTH - PAD - ALWAYS_W;
    }

    public static int onceX() {
        return alwaysX() - BUTTON_GAP - ONCE_W;
    }

    /** Every piece of the chooser as a solid box, so a test proves none of them meet or leave the chooser. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("title", 5, 4, 60, LINE_H);
        l.box("question", PAD, QUESTION_Y, listW(), LINE_H);
        l.box("note", PAD, NOTE_Y, noteW(), LINE_H);
        l.box("list", PAD, LIST_Y, listW(), LIST_H);
        l.box("only-this-time", onceX(), BUTTON_Y, ONCE_W, BUTTON_H);
        l.box("always", alwaysX(), BUTTON_Y, ALWAYS_W, BUTTON_H);
        return l;
    }
}
