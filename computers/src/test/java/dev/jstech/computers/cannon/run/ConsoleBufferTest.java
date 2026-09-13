/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsoleBufferTest {

    private static int characters(final List<String> lines) {
        return lines.stream().mapToInt(String::length).sum();
    }

    @Test
    void write_keepsTheNewestLinesUpToTheLineLimit() {
        final ConsoleBuffer console = new ConsoleBuffer();
        for (int i = 0; i < ConsoleBuffer.MOST_LINES + 50; i++) {
            console.write("line " + i);
        }
        final List<String> kept = console.lines();
        assertEquals(ConsoleBuffer.MOST_LINES, kept.size());
        assertEquals("line 50", kept.getFirst());
        assertEquals("line " + (ConsoleBuffer.MOST_LINES + 49), kept.getLast());
        assertEquals(ConsoleBuffer.MOST_LINES + 50, console.written());
    }

    @Test
    void write_cutsALineLongerThanALineMayBe() {
        final ConsoleBuffer console = new ConsoleBuffer();
        console.write("x".repeat(ConsoleBuffer.MOST_LINE_CHARACTERS * 3));
        final String kept = console.lines().getFirst();
        assertEquals(ConsoleBuffer.MOST_LINE_CHARACTERS, kept.length());
        assertTrue(kept.endsWith(ConsoleBuffer.CUT));
    }

    @Test
    void write_keepsALineThatIsExactlyAsLongAsALineMayBe() {
        final ConsoleBuffer console = new ConsoleBuffer();
        final String line = "y".repeat(ConsoleBuffer.MOST_LINE_CHARACTERS);
        console.write(line);
        assertEquals(line, console.lines().getFirst());
    }

    @Test
    void write_letsTheOldestLinesGoOnceTheCharactersRunOut() {
        final ConsoleBuffer console = new ConsoleBuffer();
        final int lines = ConsoleBuffer.MOST_CHARACTERS / ConsoleBuffer.MOST_LINE_CHARACTERS + 5;
        for (int i = 0; i < lines; i++) {
            console.write(Character.toString('a' + i).repeat(ConsoleBuffer.MOST_LINE_CHARACTERS));
        }
        final List<String> kept = console.lines();
        assertTrue(characters(kept) <= ConsoleBuffer.MOST_CHARACTERS, "kept " + characters(kept));
        assertEquals(ConsoleBuffer.MOST_CHARACTERS / ConsoleBuffer.MOST_LINE_CHARACTERS, kept.size());
        assertTrue(kept.getLast().startsWith(Character.toString('a' + lines - 1)), "the newest line stays");
        assertEquals(lines, console.written());
    }

    @Test
    void restore_holdsWhatWasSavedToTheSameLimits() {
        final List<String> saved = new ArrayList<>();
        for (int i = 0; i < ConsoleBuffer.MOST_LINES + 10; i++) {
            saved.add("saved " + i);
        }
        saved.add("z".repeat(ConsoleBuffer.MOST_LINE_CHARACTERS + 1));
        final ConsoleBuffer console = new ConsoleBuffer();

        console.restore(saved, 5);

        final List<String> kept = console.lines();
        assertEquals(ConsoleBuffer.MOST_LINES, kept.size());
        assertEquals(ConsoleBuffer.MOST_LINE_CHARACTERS, kept.getLast().length());
        assertEquals(ConsoleBuffer.MOST_LINES, console.written(), "never fewer written than kept");
    }

    @Test
    void clear_emptiesTheConsoleButKeepsTheCountOfWhatWasWritten() {
        final ConsoleBuffer console = new ConsoleBuffer();
        console.write("one");
        console.write("two");
        console.clear();
        assertEquals(List.of(), console.lines());
        assertEquals(2, console.written());
        console.write("three");
        assertEquals(List.of("three"), console.lines());
    }
}
