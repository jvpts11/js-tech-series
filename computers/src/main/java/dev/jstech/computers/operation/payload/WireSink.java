/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtySink;
import java.util.ArrayList;
import java.util.List;

/**
 * What a running tool printed on one turn, gathered up as it goes on the wire.
 *
 * <p>A tool prints into this and the machine sends what came of it, so a line printed, a bar redrawn and
 * another line printed, all in one tick, reach the terminal in that order with each one saying for itself
 * whether it goes under the last or over it.
 */
public final class WireSink implements ITtySink {

    private final List<WireLine> lines = new ArrayList<>();

    @Override
    public void line(final CliLine line) {
        this.lines.add(WireLine.of(line));
    }

    @Override
    public void redraw(final CliLine line) {
        this.lines.add(WireLine.over(line));
    }

    /** Everything printed since this was made, in order. */
    public List<WireLine> lines() {
        return this.lines;
    }

    public boolean isEmpty() {
        return this.lines.isEmpty();
    }
}
