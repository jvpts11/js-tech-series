/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import dev.jstech.core.client.gui.logic.TextDocument;

/**
 * One pass of search-and-replace through a file, the way nano makes it: from the cursor to the end, round to
 * the top, and back to where it started, stopping at each place to be told yes, no, or all.
 *
 * <p>It goes round once and no further. What it has already put in is never searched again, so replacing a
 * word with a longer one that contains it ends, which a search that simply kept going would not.
 */
public final class NanoReplace {

    private final TextDocument doc;
    private final String needle;
    private final String with;
    private final int startLine;
    private int startCol;
    private int line;
    private int col;
    private boolean wrapped;
    private int done;

    /** Starts where that document's cursor is. */
    public NanoReplace(final TextDocument doc, final String needle, final String with) {
        this.doc = doc;
        this.needle = needle;
        this.with = with;
        this.startLine = doc.cursorLine();
        this.startCol = Math.min(doc.cursorCol(), doc.line(doc.cursorLine()).length());
        this.line = this.startLine;
        this.col = this.startCol;
    }

    /**
     * Moves to the next place the text occurs and selects it, the cursor at its start.
     *
     * @return false when the pass has been all the way round and there is nowhere left
     */
    public boolean next() {
        if (this.needle.isEmpty()) {
            return false;
        }
        while (true) {
            final String text = this.doc.line(this.line);
            final boolean lastStretch = this.wrapped && this.line == this.startLine;
            final int limit = lastStretch ? Math.min(this.startCol, text.length()) : text.length();
            final int hit = this.col <= text.length() ? text.indexOf(this.needle, this.col) : -1;
            if (hit >= 0 && hit + this.needle.length() <= limit) {
                this.col = hit;
                this.doc.select(this.line, hit + this.needle.length(), this.line, hit);
                return true;
            }
            if (lastStretch) {
                this.doc.clearSelection();
                return false;
            }
            this.line++;
            this.col = 0;
            if (this.line >= this.doc.lineCount()) {
                this.line = 0;
                this.wrapped = true;
            }
        }
    }

    /** Replaces the place {@link #next} stopped at, and carries on after what was put there. */
    public void replace() {
        this.doc.insertText(this.with);
        if (this.wrapped && this.line == this.startLine) {
            // The stretch still to be searched ends where the pass began, which has just moved.
            this.startCol += this.with.length() - this.needle.length();
        }
        this.col += this.with.length();
        this.done++;
    }

    /** Leaves the place {@link #next} stopped at as it is, and carries on after it. */
    public void skip() {
        this.doc.clearSelection();
        this.col += this.needle.length();
    }

    /** Replaces the place {@link #next} stopped at and every one after it, to the end of the pass. */
    public void all() {
        do {
            replace();
        } while (next());
    }

    /** How many places have been replaced so far. */
    public int done() {
        return this.done;
    }
}
