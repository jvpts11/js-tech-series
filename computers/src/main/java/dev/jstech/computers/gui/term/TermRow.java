/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import dev.jstech.computers.program.cli.CliSpan;
import java.util.List;

/**
 * One row of a terminal's glass: runs of one colour each, no wider together than the glass is.
 *
 * <p>A row and not a line. A line is what a tool printed; a row is what is left of it once the glass has had
 * its say about how wide it is, and a long line is several of these.
 */
public record TermRow(List<CliSpan> runs) {

    public TermRow {
        runs = List.copyOf(runs);
    }

    /** How many cells the row fills. */
    public int length() {
        int cells = 0;
        for (final CliSpan run : this.runs) {
            cells += run.text().length();
        }
        return cells;
    }

    /** What the row says, colours aside. */
    public String text() {
        final StringBuilder out = new StringBuilder();
        for (final CliSpan run : this.runs) {
            out.append(run.text());
        }
        return out.toString();
    }
}
