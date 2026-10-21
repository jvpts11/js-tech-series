/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where the editor of screens puts its parts: the name of the file at the top left and its buttons at the top right,
 * the panel of the chosen component on the left (its kind, its line, its name, what it is bound to, one more of its
 * fields, its style, its box, Duplicate and Delete), the list of kinds to add on the right, and what the mouse and
 * the keys do along the foot. Pure, so a unit test checks that nothing overlaps.
 */
public final class ScreenEditorLayout {

    /**
     * The bar: its top, its buttons' height, the room between them, each one's least width in order, and the room
     * either side of a label that is wider than that.
     */
    public static final int BAR_Y = 4;
    public static final int BUTTON_HEIGHT = 14;
    public static final int BUTTON_GAP = 3;
    public static final int[] BAR_WIDTHS = {38, 38, 58, 44, 38, 38};
    public static final int LABEL_ROOM = 5;
    /**
     * The panel of the chosen component: where, how wide with room for it and the least it narrows to, the room
     * between it and the screen, its room inside and its rows.
     */
    public static final int PANEL_X = 4;
    public static final int PANEL_Y = 30;
    public static final int PANEL_WIDTH = 160;
    public static final int MIN_WIDTH = 120;
    public static final int GAP = 6;
    public static final int PAD = 6;
    public static final int TITLE_Y = 5;
    public static final int FILE_Y = 15;
    public static final int FIELDS_Y = 25;
    public static final int FIELD_STEP = 22;
    public static final int LABEL_TO_FIELD = 7;
    public static final int FIELD_HEIGHT = 13;
    public static final int FIELDS = 3;
    public static final int STYLE_Y = FIELDS_Y + FIELDS * FIELD_STEP + 3;
    public static final int POSITION_Y = STYLE_Y + BUTTON_HEIGHT + 4;
    public static final int ACTIONS_Y = POSITION_Y + 10;
    public static final int PANEL_HEIGHT = ACTIONS_Y + BUTTON_HEIGHT + PAD;
    /** The list of kinds to add: how wide, how tall a row is, and how many it shows. */
    public static final int ADD_WIDTH = 150;
    public static final int ADD_ROW = 11;
    public static final int ADD_ROWS = 8;
    public static final int ADD_HEIGHT = PAD + FIELD_HEIGHT + 4 + ADD_ROWS * ADD_ROW + 10 + PAD;
    /** How far from the screen's foot the line of what to do is. */
    public static final int HINTS_UP = 12;

    private ScreenEditorLayout() {
    }

    /** The width of the bar's button {@code index} whose label is {@code labelWidth} wide: its least, or the label's. */
    public static int barWidth(final int index, final int labelWidth) {
        return Math.max(BAR_WIDTHS[index], labelWidth + 2 * LABEL_ROOM);
    }

    /**
     * The left of the bar's first button on a screen {@code width} wide, its buttons {@code widths} wide in order, the
     * bar ending at its right edge.
     */
    public static int barLeft(final int width, final int[] widths) {
        int wide = 0;
        for (final int each : widths) {
            wide += each + BUTTON_GAP;
        }
        return width - PAD - wide + BUTTON_GAP;
    }

    /** The left of the bar's button {@code index} on a screen {@code width} wide, its buttons {@code widths} wide. */
    public static int barX(final int width, final int[] widths, final int index) {
        int x = barLeft(width, widths);
        for (int i = 0; i < index; i++) {
            x += widths[i] + BUTTON_GAP;
        }
        return x;
    }

    /** The top of field {@code index} of the panel, from the panel's. */
    public static int fieldY(final int index) {
        return FIELDS_Y + index * FIELD_STEP + LABEL_TO_FIELD;
    }

    /**
     * The panel's width beside a screen whose window starts at {@code screenLeft}: narrower when the room beside it
     * is short, so the screen being edited is never under it, down to its least.
     */
    public static int panelWidth(final int screenLeft) {
        return Math.max(MIN_WIDTH, Math.min(PANEL_WIDTH, screenLeft - GAP - PANEL_X));
    }

    /**
     * The list of kinds' width on a screen {@code width} wide whose window ends at {@code screenRight}: narrower when
     * the room beside the window is short, down to the panel's least.
     */
    public static int addWidth(final int width, final int screenRight) {
        return Math.max(MIN_WIDTH, Math.min(ADD_WIDTH, width - screenRight - GAP - PAD));
    }

    /** The panel of the chosen component {@code width} wide, as the overlap check reads it. */
    public static GuiLayout panel(final int width) {
        final GuiLayout layout = new GuiLayout(width, PANEL_HEIGHT);
        layout.text("kind", PAD, TITLE_Y, 16, 1f);
        // The file and line are cut to the room inside the panel, as the editor draws them.
        layout.textBox("file", PAD, FILE_Y, Math.min(Math.round(34 * GuiLayout.GLYPH_WIDTH * 0.75f),
                width - 2 * PAD), Math.round(GuiLayout.LINE_HEIGHT * 0.75f));
        for (int i = 0; i < FIELDS; i++) {
            layout.text("label " + i, PAD, FIELDS_Y + i * FIELD_STEP, 10, 0.75f);
            layout.box("field " + i, PAD, fieldY(i), width - 2 * PAD, FIELD_HEIGHT);
        }
        layout.box("style", PAD, STYLE_Y, width - 2 * PAD, BUTTON_HEIGHT);
        layout.text("position", PAD, POSITION_Y, 24, 0.75f);
        final int half = (width - 2 * PAD - BUTTON_GAP) / 2;
        layout.box("duplicate", PAD, ACTIONS_Y, half, BUTTON_HEIGHT);
        layout.box("delete", PAD + half + BUTTON_GAP, ACTIONS_Y, half, BUTTON_HEIGHT);
        return layout;
    }

    /**
     * The bar on a screen {@code width} wide, its buttons {@code widths} wide, as the overlap check reads it; the name
     * of the file is cut to the room before the first button, as the editor draws it.
     */
    public static GuiLayout bar(final int width, final int[] widths) {
        final GuiLayout layout = new GuiLayout(width, BAR_Y + BUTTON_HEIGHT + BAR_Y);
        layout.textBox("editing", PAD, BAR_Y + 3, Math.max(0, barLeft(width, widths) - 2 * PAD),
                Math.round(GuiLayout.LINE_HEIGHT));
        for (int i = 0; i < widths.length; i++) {
            layout.box("button " + i, barX(width, widths, i), BAR_Y, widths[i], BUTTON_HEIGHT);
        }
        return layout;
    }
}
