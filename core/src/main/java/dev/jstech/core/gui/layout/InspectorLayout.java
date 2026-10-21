/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where the developer's inspector of a screen puts its parts: a panel beside the screen with its title and the key
 * that opens it, what its colours mean, the tree of what the screen is built of, the problems found, and the buttons
 * that draw the screen at another GUI scale, in another language and in another of its states. Pure, so a unit test
 * checks that nothing overlaps.
 */
public final class InspectorLayout {

    /** The panel's width with room for it, the least it narrows to, the room between it and the screen, inside. */
    public static final int WIDTH = 152;
    public static final int MIN_WIDTH = 140;
    public static final int GAP = 6;
    public static final int PAD = 6;
    /** Where the title, the legend and the tree start, and how tall a row of the tree is. */
    public static final int TITLE_Y = 5;
    public static final int LEGEND_Y = 16;
    public static final int TREE_Y = 27;
    public static final int ROW = 10;
    /** How far in each level of the tree goes, and the room of a row's triangle and of its square. */
    public static final int INDENT = 8;
    public static final int TRIANGLE = 6;
    public static final int SQUARE = 4;
    /** The problems: the room above their heading, its height, and a line's. */
    public static final int PROBLEMS_GAP = 6;
    public static final int HEADING = 12;
    public static final int PROBLEM = 11;
    /** A problem's mark, on its line, on a row of the tree and on the screen. */
    public static final int MARK = 7;
    /** The buttons at the foot: their height and the room between them. */
    public static final int BUTTON_HEIGHT = 14;
    public static final int BUTTON_GAP = 2;
    /** The smallest the panel stands from the screen's edge. */
    public static final int EDGE = 4;

    private InspectorLayout() {
    }

    /**
     * The panel's width beside a screen whose window starts at {@code screenLeft}: narrower when the room beside it
     * is short, so it sits beside the screen as the recipe book does rather than over it, down to its least.
     */
    public static int width(final int screenLeft) {
        return Math.max(MIN_WIDTH, Math.min(WIDTH, screenLeft - GAP - EDGE));
    }

    /** The panel's left beside a screen whose window starts at {@code screenLeft}, kept on the screen. */
    public static int left(final int screenLeft) {
        return Math.max(EDGE, screenLeft - GAP - width(screenLeft));
    }

    /** How tall the panel is with {@code rows} rows of tree, {@code problems} problems and {@code buttons} buttons. */
    public static int height(final int rows, final int problems, final int buttons) {
        return TREE_Y + rows * ROW + PROBLEMS_GAP + HEADING + Math.max(1, problems) * PROBLEM + PAD
                + buttons * (BUTTON_HEIGHT + BUTTON_GAP) + PAD;
    }

    /** The top of the problems' heading, from the panel's. */
    public static int problemsY(final int rows) {
        return TREE_Y + rows * ROW + PROBLEMS_GAP;
    }

    /** The top of the first button, from the panel's. */
    public static int buttonsY(final int rows, final int problems) {
        return problemsY(rows) + HEADING + Math.max(1, problems) * PROBLEM + PAD;
    }

    /** The panel {@code width} wide at its fullest, as the overlap check reads it. */
    public static GuiLayout layout(final int width, final int rows, final int problems, final int buttons) {
        final GuiLayout layout = new GuiLayout(width, height(rows, problems, buttons));
        layout.text("title", PAD, TITLE_Y, 9, 1f);
        layout.text("legend", PAD, LEGEND_Y, 28, 0.75f);
        layout.box("tree", PAD, TREE_Y, width - 2 * PAD, rows * ROW);
        layout.text("problems", PAD, problemsY(rows), 8, 1f);
        for (int i = 0; i < buttons; i++) {
            layout.box("button " + i, PAD, buttonsY(rows, problems) + i * (BUTTON_HEIGHT + BUTTON_GAP),
                    width - 2 * PAD, BUTTON_HEIGHT);
        }
        return layout;
    }
}
