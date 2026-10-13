/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliRun;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.text.ITextLanguage;
import java.util.ArrayList;
import java.util.List;

/**
 * The line being typed at a terminal, laid out on the same grid as everything above it.
 *
 * <p>At a real terminal the prompt and what follows it are just the last line on the glass: the same cells,
 * the same size, and when they run past the right-hand edge they carry on at the start of the next row. A
 * long question a tool has stopped to ask therefore takes two rows and the answer is typed after it, instead
 * of being drawn in some other size over whatever is beside the glass.
 *
 * <p>It is cut where a row ends and nowhere else. What has been printed is broken at a space because it
 * reads better; what is being typed is not, because then a character would jump to the next row as the word
 * it belongs to grew, and the cursor with it.
 */
public final class TermInput {

    private TermInput() {
    }

    /**
     * The line as rows of cells, the cell the cursor is in, and the cell the other end of a selection is in.
     *
     * @param rows         one row or more, never none, so there is always somewhere to draw the cursor
     * @param cursorRow    which of them the cursor is on
     * @param cursorColumn how many cells across that row it sits
     * @param markRow      the row the far end of what is picked out is on, the cursor's own when nothing is
     * @param markColumn   how many cells across that row it sits
     */
    public record Laid(List<TermRow> rows, int cursorRow, int cursorColumn, int markRow, int markColumn) {

        public Laid {
            rows = List.copyOf(rows);
        }

        /** What is picked out of the line being typed, which is nothing until Shift and an arrow pick some. */
        public TermSelection selection() {
            return new TermSelection(this.markRow, this.markColumn, this.cursorRow, this.cursorColumn);
        }
    }

    /**
     * Lays out what stands before the typing and the typing after it.
     *
     * @param before  the prompt, or the question a tool has stopped to ask; a space is put after it, and any
     *                it ended with is dropped first, so the typing starts one cell on however it was written
     * @param typed      what has been typed, or empty for an answer that is kept off the glass
     * @param typedStyle the colour it is typed in
     * @param cursor  how many characters of {@code typed} come before the cursor
     * @param columns how many cells a row of this glass has
     * @param language the language the prompt or the question is read in
     */
    public static Laid lay(final CliLine before, final String typed, final CliStyle typedStyle, final int cursor,
                           final int columns, final ITextLanguage language) {
        return lay(before, typed, typedStyle, cursor, cursor, columns, language);
    }

    /**
     * The same, for a line with something picked out in it.
     *
     * @param mark how many characters of {@code typed} come before the far end of what is picked out; the same
     *             as {@code cursor} when nothing is
     */
    public static Laid lay(final CliLine before, final String typed, final CliStyle typedStyle, final int cursor,
                           final int mark, final int columns, final ITextLanguage language) {
        final int wide = Math.max(1, columns);
        final List<TermBuffer.Cell> cells = new ArrayList<>(TermBuffer.cellsOf(before.resolve(language)));
        while (!cells.isEmpty() && cells.get(cells.size() - 1).ch() == ' ') {
            cells.remove(cells.size() - 1);
        }
        if (!cells.isEmpty()) {
            cells.add(new TermBuffer.Cell(' ', CliStyle.PLAIN));
        }
        final int start = cells.size();
        cells.addAll(TermBuffer.cellsOf(List.of(new CliRun(typed, typedStyle))));
        final List<TermRow> rows = new ArrayList<>();
        for (int from = 0; from < cells.size(); from += wide) {
            rows.add(TermBuffer.rowOf(cells, from, Math.min(cells.size(), from + wide)));
        }
        final int at = start + Math.max(0, Math.min(cursor, cells.size() - start));
        final int far = start + Math.max(0, Math.min(mark, cells.size() - start));
        /* A cursor past the last cell of a full row is at the start of the next one, which may not exist yet. */
        while (rows.size() <= at / wide) {
            rows.add(new TermRow(List.of()));
        }
        return new Laid(rows, at / wide, at % wide, far / wide, far % wide);
    }
}
