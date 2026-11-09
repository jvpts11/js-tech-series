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
 * Where everything sits on the Network Manager's Services tab, which scrolls: the card of the engine that plans the
 * network's work at the top, the line of the role the engine plays, then the Subframes with who takes the network over
 * should its orchestrator fall, the engines installed and the other services, each a heading over its rows. Every y
 * here is measured down the scrolled content, from its top.
 *
 * <p>The card's first line names the engine with its state; the second says which Mainframe it runs on and carries
 * the buttons at its right end; under them, the facts in two columns, then the two that need a whole line.
 *
 * <p>Pure, and the tab and its test read the same numbers, so a row that would overlap another is caught by
 * arithmetic rather than by a player finding it.
 */
public final class NetworkServicesLayout {

    /** The Network Manager's window at the size the desktop opens it, wide enough for every tab's name. */
    public static final int DEFAULT_W = 352;
    public static final int DEFAULT_H = 210;
    /** The narrowest it goes: the card's three buttons still leave the host line room beside them. */
    public static final int MIN_W = 300;
    /** What the window keeps for its own padding, left and right together. */
    public static final int WINDOW_PAD = 12;
    /** What the tab keeps at its right for the scroll thumb. */
    public static final int THUMB_ROOM = 5;
    /** The room the tab lays itself out in when the window is at the size it opens at, and at its narrowest. */
    public static final int DEFAULT_CONTENT_W = DEFAULT_W - WINDOW_PAD - THUMB_ROOM;
    public static final int MIN_CONTENT_W = MIN_W - WINDOW_PAD - THUMB_ROOM;

    public static final int PAD = 4;
    public static final int LINE_H = 10;
    public static final int ROW_H = 11;
    public static final int BUTTON_H = 12;
    public static final int BUTTON_GAP = 3;
    /** The square of colour before an engine's or a Subframe's name. */
    public static final int SQUARE = 6;
    /** How far a name sits right of its square. */
    public static final int NAME_X = PAD + SQUARE + 4;

    /** The card's lines: the name, the host with the buttons, the facts. */
    public static final int CARD_NAME_Y = PAD;
    public static final int CARD_ACTIONS_Y = CARD_NAME_Y + LINE_H + 2;
    public static final int CARD_FACTS_Y = CARD_ACTIONS_Y + BUTTON_H + PAD;
    /** Two lines of paired facts and two whole lines (the indexes and the capabilities). */
    public static final int FACT_ROWS = 4;
    /** The widest label a fact has, in letters, which is where every value starts. */
    public static final int FACT_LABEL_CHARS = 13;
    public static final int CARD_H = CARD_FACTS_Y + FACT_ROWS * LINE_H + PAD;
    /** The lines the card's note takes when no engine runs, in the facts' place. */
    public static final int OFF_NOTE_LINES = FACT_ROWS;

    public static final int SECTION_GAP = 6;
    public static final int HEADING_H = LINE_H + 2;
    public static final int COLUMNS_H = 12;
    /** The line of the engine's role, with its two boxes at the right end. */
    public static final int ROLE_Y = CARD_H + 2;
    public static final int ROLE_BOX_GAP = 8;
    /** The lines under the Subframes: who takes the network over, and where the role is set. */
    public static final int SUCCESSION_LINES = 2;
    /** How wide the role's boxes are drawn, Orchestrator then Subframe: a box, its gap and the longest word. */
    public static final int[] ROLE_BOX_WIDTHS = {72, 52};

    private NetworkServicesLayout() {
    }

    /**
     * Where each block below the card starts, down the content.
     *
     * @param subframesHeading the heading over the Subframes
     * @param subframeColumns  their column names
     * @param subframeRows     their first row, or the line that says there are none
     * @param succession       the lines that say who takes the network over and where the role is set
     * @param installedHeading the heading over the engines installed
     * @param installedColumns their column names
     * @param installedRows    their first row
     * @param servicesHeading  the heading over the other services
     * @param serviceRows      their first row
     * @param height           the whole content's height
     */
    public record Sections(int subframesHeading, int subframeColumns, int subframeRows, int succession,
                           int installedHeading, int installedColumns, int installedRows, int servicesHeading,
                           int serviceRows, int height) {
    }

    /**
     * The blocks below the card and the role for {@code engines} engines installed, {@code subframes} Subframes (none
     * still takes a line, to say so) and {@code services} other services.
     */
    public static Sections sections(final int engines, final int subframes, final int services) {
        final int subframesHeading = ROLE_Y + LINE_H + SECTION_GAP;
        final int subframeColumns = subframesHeading + HEADING_H;
        final int subframeRows = subframeColumns + COLUMNS_H;
        final int succession = subframeRows + Math.max(1, subframes) * ROW_H + 2;
        final int installedHeading = succession + SUCCESSION_LINES * LINE_H + SECTION_GAP;
        final int installedColumns = installedHeading + HEADING_H;
        final int installedRows = installedColumns + COLUMNS_H;
        final int servicesHeading = installedRows + Math.max(1, engines) * ROW_H + SECTION_GAP;
        final int serviceRows = servicesHeading + HEADING_H;
        return new Sections(subframesHeading, subframeColumns, subframeRows, succession, installedHeading,
                installedColumns, installedRows, servicesHeading, serviceRows, serviceRows + services * ROW_H + PAD);
    }

    /** Where a list's second column starts in content {@code width} wide: the vendor, or a Subframe's engine. */
    public static int secondColumnX(final int width) {
        return width * 2 / 5;
    }

    /** Where a Subframe's engine starts in content {@code width} wide, after the machine's name. */
    public static int subframeEngineX(final int width) {
        return width * 3 / 10;
    }

    /** Where what a Subframe does starts, after its engine. */
    public static int subframeWorkX(final int width) {
        return width * 11 / 20;
    }

    /** The left edge of each of the role's two boxes, {@code widths} wide in order, laid from the line's right end. */
    public static int[] roleBoxesX(final int width, final int... widths) {
        final int[] xs = new int[widths.length];
        int right = width - PAD;
        for (int i = widths.length - 1; i >= 0; i--) {
            xs[i] = right - widths[i];
            right = xs[i] - ROLE_BOX_GAP;
        }
        return xs;
    }

    /** Where a list's third column starts: the version. */
    public static int versionX(final int width) {
        return width * 2 / 3;
    }

    /** Where the second fact of a line starts. */
    public static int secondFactX(final int width) {
        return width / 2;
    }

    /** Where a fact's value starts after its label, from the start of the pair. */
    public static int factValueOffset() {
        return (int) (FACT_LABEL_CHARS * GuiLayout.GLYPH_WIDTH);
    }

    /** The left edge of each of the card's buttons, {@code widths} wide in order, laid from the card's right end. */
    public static int[] buttonsX(final int width, final int... widths) {
        final int[] xs = new int[widths.length];
        int right = width - PAD;
        for (int i = widths.length - 1; i >= 0; i--) {
            xs[i] = right - widths[i];
            right = xs[i] - BUTTON_GAP;
        }
        return xs;
    }

    /**
     * Everything solid on the tab and every line of text, for content {@code width} wide with those blocks, the
     * card's buttons {@code buttonWidths} wide and its host line {@code hostChars} letters long.
     */
    public static GuiLayout layout(final int width, final int engines, final int subframes, final int services,
                                   final int[] buttonWidths, final int hostChars) {
        final Sections at = sections(engines, subframes, services);
        final GuiLayout layout = new GuiLayout(width, at.height());
        layout.box("cardSquare", PAD, CARD_NAME_Y + 1, SQUARE, SQUARE);
        final int[] xs = buttonsX(width, buttonWidths);
        for (int i = 0; i < xs.length; i++) {
            layout.box("button" + i, xs[i], CARD_ACTIONS_Y, buttonWidths[i], BUTTON_H);
        }
        final int hostRoom = (xs.length == 0 ? width - PAD : xs[0] - BUTTON_GAP) - NAME_X;
        layout.text("host", NAME_X, CARD_ACTIONS_Y + 2, Math.min(hostChars, (int) (hostRoom / GuiLayout.GLYPH_WIDTH)),
                1.0f);
        final int valueChars = (int) ((secondFactX(width) - PAD - factValueOffset()) / GuiLayout.GLYPH_WIDTH) - 1;
        for (int row = 0; row < 2; row++) {
            final int y = CARD_FACTS_Y + row * LINE_H;
            layout.text("fact label " + row + "a", PAD, y, FACT_LABEL_CHARS - 1, 1.0f);
            layout.text("fact value " + row + "a", PAD + factValueOffset(), y, valueChars, 1.0f);
            layout.text("fact label " + row + "b", secondFactX(width), y, FACT_LABEL_CHARS - 1, 1.0f);
            layout.text("fact value " + row + "b", secondFactX(width) + factValueOffset(), y, valueChars, 1.0f);
        }
        // The role's two boxes, each a box and its word, at the right end of the line under the card.
        final int[] roleXs = roleBoxesX(width, ROLE_BOX_WIDTHS);
        for (int i = 0; i < roleXs.length; i++) {
            layout.box("role" + i, roleXs[i], ROLE_Y, ROLE_BOX_WIDTHS[i], LINE_H);
        }
        layout.text("roleLabel", PAD, ROLE_Y + 1, (int) ((roleXs[0] - ROLE_BOX_GAP - PAD) / GuiLayout.GLYPH_WIDTH),
                1.0f);
        layout.box("subframeColumns", 0, at.subframeColumns(), width, COLUMNS_H - 1);
        for (int i = 0; i < Math.max(1, subframes); i++) {
            layout.box("subframeRow" + i, 0, at.subframeRows() + i * ROW_H, width, ROW_H);
        }
        for (int i = 0; i < SUCCESSION_LINES; i++) {
            layout.textBox("succession" + i, PAD, at.succession() + i * LINE_H, width - 2 * PAD, LINE_H);
        }
        layout.box("installedColumns", 0, at.installedColumns(), width, COLUMNS_H - 1);
        for (int i = 0; i < Math.max(1, engines); i++) {
            layout.box("engineRow" + i, 0, at.installedRows() + i * ROW_H, width, ROW_H);
        }
        for (int i = 0; i < services; i++) {
            layout.box("serviceRow" + i, 0, at.serviceRows() + i * ROW_H, width, ROW_H);
        }
        return layout;
    }
}
