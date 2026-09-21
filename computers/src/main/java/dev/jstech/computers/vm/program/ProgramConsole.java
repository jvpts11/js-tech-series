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
 * What a process has written, kept for the terminal that shows it and for the save it goes into.
 *
 * <p>The output is the machine's rather than the program's: a program that prints a string and lets
 * it go no longer holds that string, but the console still does. So it cannot be left to grow with
 * whatever a program writes, and it is held to three limits of its own. A line longer than a terminal
 * could ever show is cut, the oldest lines go once there are too many, and they go as well once all
 * the lines together are too long, so a handful of huge lines cannot take the room of two hundred
 * short ones. What a program said that long ago is not what anyone reads anyway, and what is kept is
 * written into the save of the machine it runs on.
 */
public final class ProgramConsole {

    /** The most lines kept. */
    public static final int MOST_LINES = 200;

    /** The most characters one line keeps; the rest of a longer line is cut. */
    public static final int MOST_LINE_CHARACTERS = 4_096;

    /** The most characters all the kept lines hold together. */
    public static final int MOST_CHARACTERS = 64_000;

    /** What a cut line ends with, so it reads as cut rather than as all there was. */
    static final String CUT = "...";

    private final Deque<String> lines = new ArrayDeque<>();
    private int characters;
    private long written;

    /** Writes a line, cut to size, and lets the oldest go while the limits are passed. */
    public void write(final String line) {
        this.keep(line);
        this.written++;
    }

    /**
     * Writes text a program printed, which is as many lines as it has line breaks in it.
     *
     * <p>A break at the very end only ends the last line, the way it does at any terminal, so
     * {@code "done\n"} is the one line {@code done} and {@code "\n"} is an empty line. Without this a break
     * inside a line was kept as a character no terminal knows how to show.
     */
    public void writeLines(final String text) {
        if (text.indexOf('\n') < 0) {
            this.write(text);
            return;
        }
        final String whole = text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
        for (final String line : whole.split("\n", -1)) {
            this.write(line);
        }
    }

    /** Empties what is kept; the count of what was ever written stays, since those lines were written. */
    public void clear() {
        this.lines.clear();
        this.characters = 0;
    }

    /** Puts back what a process had kept before it was put away, held to the same limits. */
    public void restore(final List<String> saved, final long written) {
        this.clear();
        for (final String line : saved) {
            this.keep(line);
        }
        this.written = Math.max(written, this.lines.size());
    }

    /** What is kept, oldest first. */
    public List<String> lines() {
        return List.copyOf(this.lines);
    }

    /**
     * How many lines have been written since the process started, the ones already dropped included.
     *
     * <p>A terminal showing what a program prints needs to know what it has not shown yet, and the count
     * of what is kept cannot say that once the oldest lines start falling off the end. A long, because a
     * program that stays up can print more lines than an int counts.
     */
    public long written() {
        return this.written;
    }

    private void keep(final String line) {
        final String fitted = line.length() <= MOST_LINE_CHARACTERS ? line
                : line.substring(0, MOST_LINE_CHARACTERS - CUT.length()) + CUT;
        this.lines.addLast(fitted);
        this.characters += fitted.length();
        while (this.lines.size() > MOST_LINES || this.characters > MOST_CHARACTERS) {
            this.characters -= this.lines.removeFirst().length();
        }
    }
}
