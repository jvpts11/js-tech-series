/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.term;

import dev.jstech.computers.gui.term.TermRow;
import dev.jstech.computers.gui.term.TermSelection;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

/**
 * Picking text out of a terminal with the pointer, and putting it on the clipboard.
 *
 * <p>One of these serves every terminal there is: the prompt that fills a monitor, the window on a desktop, and
 * a session opened on another machine. They differ in where their glass sits on the screen, which is the caller's
 * half; what a press, a drag and a second or third click mean is the same everywhere, and lives here.
 *
 * <p>The clipboard is the real one. What is copied out of a machine in the world can be pasted outside the game,
 * and what was copied anywhere can be pasted at a prompt, which is what makes a listing worth copying at all.
 */
public final class TermSelector {

    private TermSelection selection = TermSelection.NONE;
    private boolean dragging;
    private long lastClickAt;
    private int lastRow = -1;
    private int lastColumn = -1;
    private int clicks;

    /** How long after a click another counts as the second or third of the same one, in milliseconds. */
    private static final long SAME_CLICK_MS = 400L;

    /**
     * A press at that cell of the buffer: the start of a drag, or the second or third click of one before it.
     *
     * @param rows   the rows the glass is showing, from the buffer
     * @param row    which of them was pressed, counting from the top of the buffer
     * @param column how many cells across that row
     */
    public void pressed(final List<TermRow> rows, final int row, final int column) {
        final long now = Util.getMillis();
        final boolean again = now - this.lastClickAt < SAME_CLICK_MS && row == this.lastRow
                && Math.abs(column - this.lastColumn) <= 1;
        this.clicks = again ? this.clicks + 1 : 1;
        this.lastClickAt = now;
        this.lastRow = row;
        this.lastColumn = column;
        this.dragging = this.clicks == 1;
        this.selection = switch (this.clicks) {
            case 1 -> TermSelection.at(row, column);
            case 2 -> TermSelection.wordAt(rows, row, column);
            default -> TermSelection.lineAt(rows, row);
        };
    }

    /** The pointer has moved to that cell with the button still down. */
    public void draggedTo(final int row, final int column) {
        if (this.dragging) {
            this.selection = this.selection.reachingTo(row, column);
        }
    }

    /** The button is up; what was picked out stays picked out until something else is. */
    public void released() {
        this.dragging = false;
    }

    /** Puts what is picked out on the clipboard, and says whether there was anything to put there. */
    public boolean copy(final List<TermRow> rows) {
        final String text = this.selection.textOf(rows);
        if (text.isEmpty()) {
            return false;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        return true;
    }

    /** What is on the clipboard, or empty when there is nothing on it. */
    public static String clipboard() {
        final String text = Minecraft.getInstance().keyboardHandler.getClipboard();
        return text == null ? "" : text;
    }

    public TermSelection selection() {
        return this.selection;
    }

    public boolean isEmpty() {
        return this.selection.isEmpty();
    }

    /** Lets go of what was picked out, which anything that writes to the glass does. */
    public void clear() {
        this.selection = TermSelection.NONE;
        this.dragging = false;
        this.clicks = 0;
    }
}
