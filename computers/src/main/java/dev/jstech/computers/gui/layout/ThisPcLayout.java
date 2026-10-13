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
 * The geometry of the This PC window, in desktop units: a machine card, then a scrolling page of
 * sections (drives, hardware, programs), each a header row over rows of its own height. A drive row
 * carries up to three buttons on its right; the text beside them must never run under them, which is
 * what the layout test proves.
 */
public final class ThisPcLayout {

    public static final int DEFAULT_W = 300;
    public static final int DEFAULT_H = 210;
    public static final int MIN_W = 244;
    public static final int MIN_H = 140;

    /** The machine card at the top: name and kind, then the system and network lines. */
    public static final int CARD_H = 30;
    public static final int CARD_ICON_W = 22;
    /** A section header. */
    public static final int HEADER_H = 12;
    /** A drive or disk row: two lines of text, a usage bar, buttons. */
    public static final int DRIVE_ROW_H = 24;
    /** A hardware key/value line. */
    public static final int KV_ROW_H = 10;
    /** A program cell in the grid. */
    public static final int PROG_CELL_W = 58;
    public static final int PROG_CELL_H = 30;
    public static final int BTN_W = 34;
    public static final int BTN_H = 11;
    public static final int BTN_GAP = 2;
    /** The row icon on the left of a drive row. */
    public static final int ROW_ICON_W = 14;
    public static final int TEXT_X = 22;
    /** The shortest a drive's bar gets, and the gap between it and the words after it on the same line. */
    public static final int BAR_MIN_W = 40;
    public static final int BAR_GAP = 6;
    /** The gap between the longest name of the hardware list and the values beside the names. */
    public static final int HW_KEY_GAP = 6;

    /*
     * The About-style content: what KDE's Info Center, GNOME's About and Cinnamon's System Info draw instead
     * of the drives explorer above, a hero (the system's mark, name and maker) over sections or lists of
     * plain key/value facts.
     */
    public static final int ABOUT_ORB = 32;
    public static final int ABOUT_ORB_BIG = 36;
    static final int ABOUT_ROW_H = 10;
    /** How far a hero title at one and a half times reaches below its top: 12 px of glyph and its shadow. */
    static final int HERO_LINE_H = 14;

    private ThisPcLayout() {
    }

    /** The x of button {@code i} of {@code n} on a drive row, right-aligned from the window edge. */
    public static int buttonX(final int width, final int i, final int n) {
        return width - 3 - (n - i) * (BTN_W + BTN_GAP) + BTN_GAP;
    }

    /** The widest the text beside {@code n} buttons may be before it runs under them. */
    public static int driveTextMaxW(final int width, final int n) {
        return (n == 0 ? width - 4 : buttonX(width, 0, n) - 3) - TEXT_X;
    }

    /**
     * How wide a drive's bar is when the words saying how much is free, {@code usageW} wide, close the same line at
     * its right end: the bar ends a gap before them, and keeps a short length of its own when the words take the rest.
     */
    public static int barWidth(final int maxW, final int usageW) {
        return Math.max(BAR_MIN_W, maxW - usageW - BAR_GAP);
    }

    /** How many program cells fit across the window. */
    public static int programColumns(final int width) {
        return Math.max(1, (width - 8) / PROG_CELL_W);
    }

    /** The x of program cell {@code column}. */
    public static int programCellX(final int width, final int column) {
        return 4 + column * PROG_CELL_W;
    }

    /** The page below the card, where the sections scroll. */
    public static int pageY() {
        return CARD_H + 1;
    }

    public static int pageH(final int height) {
        return height - pageY();
    }

    /** One drive row with its {@code buttons}, as solid boxes, so a test proves the text and the buttons never meet. */
    public static GuiLayout driveRow(final int width, final int buttons) {
        final GuiLayout l = new GuiLayout(width, DRIVE_ROW_H);
        l.box("icon", 4, 3, ROW_ICON_W, 10);
        l.box("text", TEXT_X, 1, driveTextMaxW(width, buttons), DRIVE_ROW_H - 2);
        for (int i = 0; i < buttons; i++) {
            l.box("button" + i, buttonX(width, i, buttons), (DRIVE_ROW_H - BTN_H) / 2, BTN_W, BTN_H);
        }
        return l;
    }

    /** The window's fixed pieces at {@code width} x {@code height}: the card and the page under it. */
    public static GuiLayout layout(final int width, final int height) {
        final GuiLayout l = new GuiLayout(width, height);
        l.box("card-icon", 4, 4, CARD_ICON_W, CARD_H - 8);
        l.box("card-text", 4 + CARD_ICON_W + 4, 2, width - (4 + CARD_ICON_W + 4) - 4, CARD_H - 4);
        l.box("page", 0, pageY(), width, pageH(height));
        return l;
    }

    /** The program grid at {@code width} with {@code count} cells, so a test proves the cells tile without touching. */
    public static GuiLayout programGrid(final int width, final int count) {
        final int columns = programColumns(width);
        final int rows = (count + columns - 1) / columns;
        final GuiLayout l = new GuiLayout(width, Math.max(1, rows) * PROG_CELL_H);
        for (int i = 0; i < count; i++) {
            l.box("cell" + i, programCellX(width, i % columns), (i / columns) * PROG_CELL_H,
                    PROG_CELL_W - 2, PROG_CELL_H - 2);
        }
        return l;
    }

    /*
     * Fixed size: none of the three About-style contents reflows, so there is no min size distinct from the
     * default one.
     */

    /** KDE's Info Center: a hero row, then Software, Hardware and Computer, each a header over its rows. */
    public static final class KdeAbout {

        /*
         * The label column has to clear the longest translated label this page ever draws (20 characters)
         * before the value column starts, or the label runs into the value; the value column then keeps the
         * longest data This PC ever writes on one line in full (a processor's full name and clock) instead
         * of cutting it to a few letters.
         */
        public static final int W = 348;
        public static final int H = 176;
        public static final int ORB_X = 6;
        public static final int ORB_Y = 6;
        public static final int TITLE_X = 44;
        public static final int TITLE_Y = 8;
        /** Under the hero title's full height at its scale, shadow included. */
        public static final int SUBTITLE_Y = TITLE_Y + HERO_LINE_H;
        public static final int SOFTWARE_Y = 42;
        public static final int HARDWARE_Y = 94;
        public static final int COMPUTER_Y = 146;
        public static final int LABEL_X = 6;
        public static final int VALUE_X = 132;
        public static final int LABEL_W = VALUE_X - LABEL_X - 4;
        public static final int VALUE_W = W - VALUE_X - 6;
        /** The scale the hero title draws at, larger than the sections under it. */
        public static final float HERO_SCALE = 1.5f;

        /** The longest translated label either language draws on this page, 20 characters. */
        private static final int WORST_LABEL_CHARS = 20;
        /** The longest processor name and clock This PC ever writes here, 35 characters. */
        private static final int WORST_VALUE_CHARS = 35;
        /** "FreeBSD 14.1-RELEASE", the longest "Operating System" hero this page draws. */
        private static final int HERO_TITLE_CHARS = 20;
        /** "Daemon Foundation", the longest house name this page draws under the hero. */
        private static final int HERO_HOUSE_CHARS = 17;

        private KdeAbout() {
        }

        /** Where row {@code index} under the section headed at {@code sectionY} sits. */
        public static int rowY(final int sectionY, final int index) {
            return sectionY + HEADER_H + index * ABOUT_ROW_H;
        }

        /**
         * The window as solids: the hero and the three sections, each header and its rows one box, with the
         * hero and every row's label and value recorded as text at their longest so a window too narrow for
         * them, in either language, spills past the panel and fails the audit.
         */
        public static GuiLayout layout() {
            final GuiLayout l = new GuiLayout(W, H);
            l.box("orb", ORB_X, ORB_Y, ABOUT_ORB, ABOUT_ORB);
            l.box("hero-text", TITLE_X, TITLE_Y, W - TITLE_X - 4, SUBTITLE_Y - TITLE_Y + 8);
            l.text("hero-title", TITLE_X, TITLE_Y, HERO_TITLE_CHARS, HERO_SCALE);
            l.text("hero-subtitle", TITLE_X, SUBTITLE_Y, HERO_HOUSE_CHARS, 1.0f);
            l.box("software", LABEL_X, SOFTWARE_Y, W - LABEL_X * 2, HEADER_H + 4 * ABOUT_ROW_H);
            l.box("hardware", LABEL_X, HARDWARE_Y, W - LABEL_X * 2, HEADER_H + 4 * ABOUT_ROW_H);
            l.box("computer", LABEL_X, COMPUTER_Y, W - LABEL_X * 2, HEADER_H + ABOUT_ROW_H);
            worstCaseRows(l, "software", SOFTWARE_Y, 4);
            worstCaseRows(l, "hardware", HARDWARE_Y, 4);
            worstCaseRows(l, "computer", COMPUTER_Y, 1);
            return l;
        }

        private static void worstCaseRows(final GuiLayout l, final String name, final int sectionY, final int rows) {
            for (int i = 0; i < rows; i++) {
                final int y = rowY(sectionY, i);
                l.text(name + "-label" + i, LABEL_X, y, WORST_LABEL_CHARS, 1.0f);
                l.text(name + "-value" + i, VALUE_X, y, WORST_VALUE_CHARS, 1.0f);
            }
        }
    }

    /** GNOME's About: a centred hero over two boxed lists, the machine's own facts and the system's. */
    public static final class GnomeAbout {

        public static final int W = 346;
        public static final int H = 170;
        public static final int ORB_Y = 6;
        public static final int TITLE_Y = 46;
        /** Under the hero title's full height at its scale, shadow included. */
        public static final int SUBTITLE_Y = TITLE_Y + HERO_LINE_H;
        public static final int LIST1_Y = SUBTITLE_Y + 13;
        public static final int LIST1_ROWS = 5;
        public static final int LIST_PAD = 2;
        public static final int LIST1_H = LIST_PAD * 2 + LIST1_ROWS * ABOUT_ROW_H;
        public static final int LIST2_Y = LIST1_Y + LIST1_H + 4;
        public static final int LIST2_ROWS = 3;
        public static final int LIST2_H = LIST_PAD * 2 + LIST2_ROWS * ABOUT_ROW_H;
        /** Wide enough for the longest translated label either language draws here, 20 characters. */
        public static final int LABEL_W = 128;
        /** Where a row's label starts: inside the list's outline, with a few pixels of air. */
        public static final int LABEL_X = 8;
        public static final int VALUE_X = 4 + LABEL_W;
        /** The value column, ending the same few pixels inside the list's right edge. */
        public static final int VALUE_W = W - VALUE_X - 8;
        public static final int SIDE_PAD = 6;
        /** The scale the hero title draws at, larger than the lists under it. */
        public static final float HERO_SCALE = 1.5f;

        private static final int WORST_LABEL_CHARS = 20;
        /** The longest processor name and clock This PC ever writes here, 35 characters. */
        private static final int WORST_VALUE_CHARS = 35;
        /** The hero shortens FreeBSD's release ("FreeBSD 14.1"); this stays the worst case either way. */
        private static final int HERO_TITLE_CHARS = 20;
        private static final int HERO_HOUSE_CHARS = 17;

        private GnomeAbout() {
        }

        public static int orbX() {
            return (W - ABOUT_ORB_BIG) / 2;
        }

        /** Where row {@code index} of the list starting at {@code listY} sits. */
        public static int rowY(final int listY, final int index) {
            return listY + LIST_PAD + index * ABOUT_ROW_H;
        }

        /**
         * The window as solids: the hero, then the two lists, each one box, with the hero and every row's
         * label and value recorded as text at their longest.
         */
        public static GuiLayout layout() {
            final GuiLayout l = new GuiLayout(W, H);
            l.box("orb", orbX(), ORB_Y, ABOUT_ORB_BIG, ABOUT_ORB_BIG);
            l.box("hero-text", 4, TITLE_Y, W - 8, SUBTITLE_Y - TITLE_Y + 8);
            final int heroTitleW = Math.round(HERO_TITLE_CHARS * GuiLayout.GLYPH_WIDTH * HERO_SCALE);
            l.text("hero-title", (W - heroTitleW) / 2, TITLE_Y, HERO_TITLE_CHARS, HERO_SCALE);
            l.text("hero-subtitle", (W - HERO_HOUSE_CHARS * (int) GuiLayout.GLYPH_WIDTH) / 2, SUBTITLE_Y,
                    HERO_HOUSE_CHARS, 1.0f);
            l.box("list1", 4, LIST1_Y, W - 8, LIST1_H);
            l.box("list2", 4, LIST2_Y, W - 8, LIST2_H);
            worstCaseRows(l, "list1", LIST1_Y, LIST1_ROWS);
            worstCaseRows(l, "list2", LIST2_Y, LIST2_ROWS);
            return l;
        }

        private static void worstCaseRows(final GuiLayout l, final String name, final int listY, final int rows) {
            // A value longer than its column is trimmed to it, so the column is the most it ever takes.
            final int valueChars = Math.min(WORST_VALUE_CHARS, (int) (VALUE_W / GuiLayout.GLYPH_WIDTH));
            for (int i = 0; i < rows; i++) {
                final int y = rowY(listY, i);
                l.text(name + "-label" + i, LABEL_X, y, WORST_LABEL_CHARS, 1.0f);
                l.text(name + "-value" + i, VALUE_X, y, valueChars, 1.0f);
            }
        }
    }

    /**
     * Cinnamon's System Info: the mark and the maker beside one list of every fact, no header over it.
     *
     * <p>This page is held, like every other fixed-size screen, to the project's 360 px honest ceiling
     * ({@code LayoutAuditTest.SCREEN_W_BUDGET}). The values draw at the labels' own size, as the real page
     * does; beside the logo well and a label column wide enough for the longest translated label, a line
     * longer than the value column (only the longest processor names) is trimmed to it, as the other two
     * About pages trim theirs.
     */
    public static final class CinnamonAbout {

        public static final int W = 360;
        public static final int H = 88;
        /** Wide enough to centre the longest house name shown here ("Daemon Foundation", 17 characters). */
        public static final int LOGO_W = 74;
        public static final int LOGO_ORB_Y = 8;
        public static final int LOGO_TITLE_Y = 47;
        public static final int LOGO_SUBTITLE_Y = 58;
        public static final int LIST_X = LOGO_W + 6;
        public static final int LIST_Y = 8;
        public static final int LIST_ROWS = 7;
        /** Wide enough for the longest translated label either language draws here, 19 characters. */
        public static final int LABEL_W = 120;
        public static final int VALUE_X = LIST_X + LABEL_W + 4;
        public static final int VALUE_W = W - VALUE_X - 6;

        private static final int WORST_LABEL_CHARS = 19;
        /** As many letters as the value column holds, which is where a longer value is trimmed. */
        private static final int VALUE_CHARS = (int) (VALUE_W / GuiLayout.GLYPH_WIDTH);
        /** The bare system name in the logo well ("FreeBSD", "Ubuntu", ...): short, never trimmed. */
        private static final int LOGO_TITLE_CHARS = 7;
        /** "Daemon Foundation", the longest house name; the well trims it rather than growing to fit it whole. */
        private static final int LOGO_HOUSE_CHARS = 17;

        private CinnamonAbout() {
        }

        public static int orbX() {
            return (LOGO_W - ABOUT_ORB_BIG) / 2;
        }

        public static int rowY(final int index) {
            return LIST_Y + index * ABOUT_ROW_H;
        }

        /**
         * The window as solids: the logo well on the left, the fact list on the right, with the logo's words
         * and every row's label and value recorded as text at their longest.
         */
        public static GuiLayout layout() {
            final GuiLayout l = new GuiLayout(W, H);
            l.box("logo", 0, LOGO_ORB_Y, LOGO_W, LOGO_SUBTITLE_Y - LOGO_ORB_Y + 8);
            l.box("list", LIST_X, LIST_Y, W - LIST_X - 4, LIST_ROWS * ABOUT_ROW_H);
            final int glyph = (int) GuiLayout.GLYPH_WIDTH;
            l.text("logo-title", (LOGO_W - LOGO_TITLE_CHARS * glyph) / 2, LOGO_TITLE_Y, LOGO_TITLE_CHARS, 1.0f);
            final int houseChars = Math.min(LOGO_HOUSE_CHARS, (LOGO_W - 4) / glyph);
            l.text("logo-subtitle", (LOGO_W - houseChars * glyph) / 2, LOGO_SUBTITLE_Y, houseChars, 1.0f);
            for (int i = 0; i < LIST_ROWS; i++) {
                final int y = rowY(i);
                l.text("list-label" + i, LIST_X, y, WORST_LABEL_CHARS, 1.0f);
                l.text("list-value" + i, VALUE_X, y, VALUE_CHARS, 1.0f);
            }
            return l;
        }
    }
}
