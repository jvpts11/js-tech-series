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

    /** The most typed lines kept waiting to be read. */
    static final int MOST_LINES = 16;

    private final Deque<String> lines = new ArrayDeque<>();

    /** Keeps a typed line for the program to read, unless the most it keeps are already waiting. */
    void offer(final String line) {
        if (this.lines.size() < MOST_LINES) {
            this.lines.addLast(line == null ? "" : line);
        }
    }

    /** The next line typed, or an empty string when none has been. */
    String take() {
        final String line = this.lines.pollFirst();
        return line == null ? "" : line;
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
