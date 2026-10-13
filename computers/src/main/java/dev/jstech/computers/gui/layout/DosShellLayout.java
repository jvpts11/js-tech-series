/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

/**
 * Where everything of the MC-DOS Shell stands on its text screen, in cells: the title, the menu bar, the path and the
 * drives across the top, the key line at the bottom, and between them the directory tree and the file list over the
 * program list and the Active Task List, each area with its title bar. Laid out to whatever the glass holds, the
 * areas taking the rows there are; at 80 by 25 it is the shell's own screen.
 *
 * @param columns     the cells across
 * @param rows        the rows down
 * @param split       the column the right-hand areas start at; the one before it is the rule between them
 * @param topTitle    the row of the tree's and the file list's title bars, or -1 when they are not shown
 * @param topRows     how many rows the tree and the file list have under their titles
 * @param bottomTitle the row of the program list's and the task list's title bars, or -1 when not shown
 * @param bottomRows  how many rows those lists have under their titles
 * @param tasks       whether the Active Task List is shown beside the program list
 */
public record DosShellLayout(int columns, int rows, int split, int topTitle, int topRows, int bottomTitle,
                             int bottomRows, boolean tasks) {

    /** The title bar, the menu bar, the path and the drives, from the top. */
    public static final int TITLE_ROW = 0;
    public static final int MENU_ROW = 1;
    public static final int PATH_ROW = 2;
    public static final int DRIVES_ROW = 3;
    /** Where the areas start, under the drives. */
    public static final int AREAS_TOP = 4;
    /** How far apart the drives stand along their row. */
    public static final int DRIVE_STEP = 6;
    /** Where the first menu title stands, and the gap between one title and the next. */
    public static final int MENU_LEFT = 1;
    public static final int MENU_GAP = 2;

    /* The share of the rows the tree and file list take when the program lists are shown under them: nine of twenty. */
    private static final int TOP_SHARE = 9;
    private static final int SHARE_OF = 20;

    /** What the screen shows, as the View menu has it. */
    public enum View {
        /** The tree and the file list over the program list and the tasks: the shell as it opens. */
        PROGRAMS_AND_FILES,
        /** The tree and the file list alone, down to the key line. */
        FILES,
        /** The program list and the tasks alone. */
        PROGRAMS
    }

    /** The layout for a screen of that many cells, showing what {@code view} shows, the tasks beside or not. */
    public static DosShellLayout of(final int columns, final int rows, final View view, final boolean tasks) {
        final int across = Math.max(40, columns);
        final int down = Math.max(12, rows);
        // From the areas' first row to the row above the key line.
        final int room = down - 1 - AREAS_TOP;
        final int split = across / 2;
        return switch (view) {
            case FILES -> new DosShellLayout(across, down, split, AREAS_TOP, room - 1, -1, 0, tasks);
            case PROGRAMS -> new DosShellLayout(across, down, split, -1, 0, AREAS_TOP, room - 1, tasks);
            case PROGRAMS_AND_FILES -> {
                final int top = Math.max(3, room * TOP_SHARE / SHARE_OF);
                yield new DosShellLayout(across, down, split, AREAS_TOP, top - 1, AREAS_TOP + top, room - top - 1,
                        tasks);
            }
        };
    }

    /** The key line, the last row. */
    public int keyRow() {
        return rows - 1;
    }

    /** The first row of the tree and the file list. */
    public int topFirst() {
        return topTitle + 1;
    }

    /** The first row of the program list and the tasks. */
    public int bottomFirst() {
        return bottomTitle + 1;
    }

    /** How wide the tree is, its scroll bar in its last column. */
    public int leftWidth() {
        return split - 1;
    }

    /** How wide the file list is, its scroll bar in the screen's last column. */
    public int rightWidth() {
        return columns - split;
    }

    /** How wide the program list is: half the screen beside the tasks, or all of it without them. */
    public int programsWidth() {
        return tasks ? split - 1 : columns;
    }

    /** Whether the tree and the file list are shown. */
    public boolean showsFiles() {
        return topTitle >= 0;
    }

    /** Whether the program list is shown. */
    public boolean showsPrograms() {
        return bottomTitle >= 0;
    }

    /** Whether the cell ({@code column}, {@code row}) is in the tree, title bar aside. */
    public boolean inTree(final int column, final int row) {
        return showsFiles() && row >= topFirst() && row < topFirst() + topRows && column < leftWidth();
    }

    /** Whether the cell is in the file list. */
    public boolean inFiles(final int column, final int row) {
        return showsFiles() && row >= topFirst() && row < topFirst() + topRows && column >= split;
    }

    /** Whether the cell is in the program list. */
    public boolean inPrograms(final int column, final int row) {
        return showsPrograms() && row >= bottomFirst() && row < bottomFirst() + bottomRows
                && column < programsWidth();
    }

    /** Whether the cell is in the Active Task List. */
    public boolean inTasks(final int column, final int row) {
        return showsPrograms() && tasks && row >= bottomFirst() && row < bottomFirst() + bottomRows
                && column >= split;
    }
}
