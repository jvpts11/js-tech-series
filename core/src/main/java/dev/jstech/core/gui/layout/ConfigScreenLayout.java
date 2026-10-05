/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import dev.jstech.core.config.ConfigDraft;
import java.util.List;

/**
 * Where everything on the Core's settings screen sits: a header with the mod's name and the file's, a rail down the
 * left with the mod's files and their sections, the open section's settings in rows on the right, each a name over a
 * line of what it does and its control at the row's right end, and a footer with Defaults on the left and Cancel and
 * Done on the right. The screen draws from these, and a test lays the screen out at the smallest size the game allows,
 * with every kind of control, and checks nothing collides.
 *
 * <p>Every x and y is from the top left of the screen.
 */
public final class ConfigScreenLayout {

    public static final int HEADER = 18;
    public static final int FOOTER = 24;
    /** Wide enough for a section's name of some twenty letters, as the series' longest are. */
    public static final int RAIL_WIDTH = 150;
    public static final int PAD = 8;
    /** A rail entry: a file's heading or one of its sections. */
    public static final int RAIL_ITEM = 14;
    /** The open section's title, and the lines of what it is for under it. */
    public static final int TITLE_HEIGHT = 12;
    public static final int NOTE_LINE = 8;
    public static final int ROW_HEIGHT = 24;
    /** A control's height, and a step button's side. */
    public static final int CONTROL_HEIGHT = 14;
    public static final int SWITCH_WIDTH = 28;
    public static final int SWITCH_HEIGHT = 12;
    public static final int STEP = 14;
    public static final int NUMBER_FIELD = 50;
    public static final int CONTROL_GAP = 3;
    public static final int CHOICE_WIDTH = 78;
    public static final int TEXT_WIDTH = 90;
    public static final int FIXED_WIDTH = 90;
    public static final int BUTTON_HEIGHT = 16;
    public static final int DEFAULTS_WIDTH = 56;
    public static final int CANCEL_WIDTH = 50;
    public static final int DONE_WIDTH = 50;
    /** The room between a row's name and its control. */
    public static final int NAME_GAP = 8;
    /** The smallest screen the game lays out, in its own units. */
    public static final int SMALLEST_WIDTH = 320;
    public static final int SMALLEST_HEIGHT = 240;

    private ConfigScreenLayout() {
    }

    /** The left edge of the content beside the rail. */
    public static int contentLeft() {
        return RAIL_WIDTH + PAD;
    }

    /** How wide a section's name may run in the rail. */
    public static int railLabelWidth() {
        return RAIL_WIDTH - PAD * 2;
    }

    /** The right edge of the content, where every control ends. */
    public static int contentRight(final int width) {
        return width - PAD;
    }

    /** Where the first row starts, under the section's title and its note of so many lines. */
    public static int rowsTop(final int noteLines) {
        return HEADER + PAD + TITLE_HEIGHT + noteLines * NOTE_LINE + PAD / 2;
    }

    /** The bottom of the rows' room, above the footer. */
    public static int rowsBottom(final int height) {
        return height - FOOTER - 2;
    }

    /** How many rows fit on a screen that tall, under a note of so many lines. */
    public static int visibleRows(final int height, final int noteLines) {
        return Math.max(1, (rowsBottom(height) - rowsTop(noteLines)) / ROW_HEIGHT);
    }

    public static int rowY(final int noteLines, final int index) {
        return rowsTop(noteLines) + index * ROW_HEIGHT;
    }

    /** How wide a control of that kind is. */
    public static int controlWidth(final ConfigDraft.Control control) {
        return switch (control) {
            case TOGGLE -> SWITCH_WIDTH;
            case NUMBER -> STEP + CONTROL_GAP + NUMBER_FIELD + CONTROL_GAP + STEP;
            case CHOICE -> CHOICE_WIDTH;
            case TEXT -> TEXT_WIDTH;
            case FIXED -> FIXED_WIDTH;
        };
    }

    /** Where a control of that kind starts, at the right end of its row. */
    public static int controlX(final int width, final ConfigDraft.Control control) {
        return contentRight(width) - 4 - controlWidth(control);
    }

    /** The top of a control in the row at {@code rowY}, centred down the row. */
    public static int controlY(final int rowY, final ConfigDraft.Control control) {
        final int tall = control == ConfigDraft.Control.TOGGLE ? SWITCH_HEIGHT : CONTROL_HEIGHT;
        return rowY + (ROW_HEIGHT - tall) / 2;
    }

    /** How wide a row's name and its line may run before its control. */
    public static int nameWidth(final int width, final ConfigDraft.Control control) {
        return controlX(width, control) - NAME_GAP - (contentLeft() + 4);
    }

    public static int footerButtonY(final int height) {
        return height - FOOTER + (FOOTER - BUTTON_HEIGHT) / 2;
    }

    public static int doneX(final int width) {
        return width - PAD - DONE_WIDTH;
    }

    public static int cancelX(final int width) {
        return doneX(width) - 4 - CANCEL_WIDTH;
    }

    /** The screen with rows of those controls, as many as fit, under a note of so many lines. */
    public static GuiLayout layout(final int width, final int height, final int noteLines,
                                   final List<ConfigDraft.Control> rows) {
        final GuiLayout layout = new GuiLayout(width, height)
                .box("header", 0, 0, width, HEADER)
                .box("footer_defaults", PAD, footerButtonY(height), DEFAULTS_WIDTH, BUTTON_HEIGHT)
                .box("footer_cancel", cancelX(width), footerButtonY(height), CANCEL_WIDTH, BUTTON_HEIGHT)
                .box("footer_done", doneX(width), footerButtonY(height), DONE_WIDTH, BUTTON_HEIGHT);
        final int shown = Math.min(rows.size(), visibleRows(height, noteLines));
        for (int row = 0; row < shown; row++) {
            final ConfigDraft.Control control = rows.get(row);
            final int y = rowY(noteLines, row);
            layout.text("name_" + row, contentLeft() + 4, y + 3, nameWidth(width, control) / 6, 1.0F);
            if (control == ConfigDraft.Control.NUMBER) {
                final int x = controlX(width, control);
                final int top = controlY(y, control);
                layout.box("minus_" + row, x, top, STEP, CONTROL_HEIGHT);
                layout.box("field_" + row, x + STEP + CONTROL_GAP, top, NUMBER_FIELD, CONTROL_HEIGHT);
                layout.box("plus_" + row, x + STEP + CONTROL_GAP + NUMBER_FIELD + CONTROL_GAP, top, STEP,
                        CONTROL_HEIGHT);
            } else {
                layout.box("control_" + row, controlX(width, control), controlY(y, control), controlWidth(control),
                        control == ConfigDraft.Control.TOGGLE ? SWITCH_HEIGHT : CONTROL_HEIGHT);
            }
        }
        return layout;
    }
}
