/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Lines typed at the terminal a process is in front of, in the order they came, waiting for the program to read them.
 *
 * <p>Bounded: a terminal keeps what was typed ahead, not everything ever typed, so a line typed while as many as it
 * keeps are already waiting is let go. What is waiting goes into the save with the rest of the process, so a line
 * typed just before the world was put away is still there to be read when it comes back.
 */
final class ProgramInput {

    private final Deque<String> lines = new ArrayDeque<>();

    /** The most typed lines kept waiting to be read. */
    static final int MOST_LINES = 16;
    /** What the rest of a line, part of which has been read, ends in; a line typed whole never holds one. */
    private static final String REST = "\n";

    /** Keeps a typed line for the program to read, unless the most it keeps are already waiting. */
    void offer(final String line) {
        if (this.lines.size() < MOST_LINES) {
            this.lines.addLast(line == null ? "" : line);
        }
    }

    /**
     * The next line typed, or an empty string when none has been. A line some of which has already been read by a
     * character or a value is the rest of it.
     */
    String take() {
        final String line = this.lines.pollFirst();
        if (line == null) {
            return "";
        }
        return line.endsWith(REST) ? line.substring(0, line.length() - 1) : line;
    }

    /**
     * The next character typed, the end of its line included, as C's getchar reads it; the rest of the line waits for
     * the next read. A line is taken as ending in a line break, which is how it is kept once part of it has been read,
     * so what is left of a line is told apart from a line that was typed whole.
     */
    char takeChar() {
        final String head = this.lines.pollFirst();
        final String line = head == null ? REST : head.endsWith(REST) ? head : head + REST;
        if (line.length() > 1) {
            this.lines.addFirst(line.substring(1));
        }
        return line.charAt(0);
    }

    /**
     * The next line typed as what is left to read of it, for a read of one value: the line ends in a line break, as
     * the rest of a line is kept.
     */
    String takeRest() {
        final String head = this.lines.pollFirst();
        return head == null ? REST : head.endsWith(REST) ? head : head + REST;
    }

    /** Puts back what a read of one value left of a line, unless nothing but spaces is left of it. */
    void putBack(final String rest) {
        if (!rest.isBlank()) {
            this.lines.addFirst(rest.endsWith(REST) ? rest : rest + REST);
        }
    }

    /** Whether a typed line is waiting to be read. */
    boolean has() {
        return !this.lines.isEmpty();
    }

    /** The lines still waiting, oldest first, for the save. */
    List<String> lines() {
        return List.copyOf(this.lines);
    }

    /** Puts back the lines that were waiting when the process was put away, held to the same limit. */
    void restore(final List<String> saved) {
        this.lines.clear();
        for (final String line : saved) {
            this.offer(line);
        }
    }
}
