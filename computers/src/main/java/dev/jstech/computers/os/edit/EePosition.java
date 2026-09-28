/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

/**
 * The three numbers {@code ee}'s position row names, worked out once from where the caret and the view
 * stand: the line and how far down the view has scrolled are one-based, the way the row reads them, and the
 * column is not, since it is a count of what is to the caret's left rather than a rank among the columns.
 *
 * @param line    the line the caret is on, counting the first as one
 * @param column  how many characters stand to the caret's left on that line
 * @param fromTop how many lines down the view the caret's line is, counting the first shown as one
 */
public record EePosition(int line, int column, int fromTop) {

    /** Works the three out from the caret's line and column and the view's scroll, all counted from zero. */
    public static EePosition of(final int cursorLine, final int cursorCol, final int scroll) {
        return new EePosition(cursorLine + 1, cursorCol, cursorLine - scroll + 1);
    }
}
