/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.tty;

import dev.jstech.computers.program.cli.CliLine;

/**
 * Where a running tool prints: below everything else, or over the line it printed last.
 *
 * <p>The second is what a bar, a counter and a spinner are. They do not fill the screen with copies of
 * themselves, they redraw the one line they are on, and a terminal that could not do that could not show a
 * download.
 */
public interface ITtySink {

    /** A line below everything printed so far. */
    void line(CliLine line);

    /** The same line again, changed: drawn over the last one printed rather than under it. */
    void redraw(CliLine line);
}
