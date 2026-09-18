/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.tty;

import org.jetbrains.annotations.Nullable;

/**
 * A tool running in front of a terminal: something that takes time, prints while it takes it, and gives the
 * prompt back when it is done.
 *
 * <p>Most commands answer at once and are finished. The ones worth watching are not like that. A fetch counts
 * its bytes down, an archive names every path as it lays it out, a compile scrolls for a minute, a package
 * manager stops to ask whether it should go ahead. Those hold the terminal the way a real one is held: the
 * prompt stays away, what is typed goes to the tool, and Ctrl+C is the one thing the terminal itself still
 * understands.
 *
 * <p>The machine moves one of these along on its own clock, a tick at a time, whether or not anybody is
 * looking. What it prints goes to whoever is; with nobody there it is told there is nowhere to print and does
 * the rest of its work without the cost of making lines no one will read.
 */
public interface ITtyProcess {

    /**
     * Starts the clock.
     *
     * @param now the tick the tool was started on
     */
    void begin(long now);

    /**
     * Moves the tool along to a tick, printing what happened on the way there.
     *
     * @param out where to print, or null when nobody is watching and nothing needs saying
     */
    void advance(long now, @Nullable ITtySink out);

    /** What the tool has stopped to ask, or null when it is not waiting on anybody. */
    @Nullable
    TtyQuestion asking();

    /**
     * What was typed at a tool that had asked. One that had not asked ignores it, the way a terminal drops what
     * is typed at a program that is not reading.
     */
    void answer(String line, long now, @Nullable ITtySink out);

    /** Ctrl+C: the tool stops where it stands, and whatever it had not finished stays not done. */
    void interrupt(long now, @Nullable ITtySink out);

    /** Whether the tool has finished, one way or the other, and the prompt may come back. */
    boolean over();
}
