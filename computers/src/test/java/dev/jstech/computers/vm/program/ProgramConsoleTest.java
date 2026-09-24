/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramConsoleTest {

    private static int characters(final List<String> lines) {
        return lines.stream().mapToInt(String::length).sum();
    }

    @Test
    void write_keepsTheNewestLinesUpToTheLineLimit() {
        final ProgramConsole console = new ProgramConsole();
        for (int i = 0; i < ProgramConsole.MOST_LINES + 50; i++) {
            console.write("line " + i);
        }
        final List<String> kept = console.lines();
        assertEquals(ProgramConsole.MOST_LINES, kept.size());
        assertEquals("line 50", kept.getFirst());
        assertEquals("line " + (ProgramConsole.MOST_LINES + 49), kept.getLast());
        assertEquals(ProgramConsole.MOST_LINES + 50, console.written());
    }

    @Test
    void write_cutsALineLongerThanALineMayBe() {
        final ProgramConsole console = new ProgramConsole();
        console.write("x".repeat(ProgramConsole.MOST_LINE_CHARACTERS * 3));
        final String kept = console.lines().getFirst();
        assertEquals(ProgramConsole.MOST_LINE_CHARACTERS, kept.length());
        assertTrue(kept.endsWith(ProgramConsole.CUT));
    }

    @Test
    void write_keepsALineThatIsExactlyAsLongAsALineMayBe() {
        final ProgramConsole console = new ProgramConsole();
        final String line = "y".repeat(ProgramConsole.MOST_LINE_CHARACTERS);
        console.write(line);
        assertEquals(line, console.lines().getFirst());
    }

    @Test
    void write_letsTheOldestLinesGoOnceTheCharactersRunOut() {
        final ProgramConsole console = new ProgramConsole();
        final int lines = ProgramConsole.MOST_CHARACTERS / ProgramConsole.MOST_LINE_CHARACTERS + 5;
        for (int i = 0; i < lines; i++) {
            console.write(Character.toString('a' + i).repeat(ProgramConsole.MOST_LINE_CHARACTERS));
        }
        final List<String> kept = console.lines();
        assertTrue(characters(kept) <= ProgramConsole.MOST_CHARACTERS, "kept " + characters(kept));
        assertEquals(ProgramConsole.MOST_CHARACTERS / ProgramConsole.MOST_LINE_CHARACTERS, kept.size());
        assertTrue(kept.getLast().startsWith(Character.toString('a' + lines - 1)), "the newest line stays");
        assertEquals(lines, console.written());
    }

    @Test
    void restore_holdsWhatWasSavedToTheSameLimits() {
        final List<Text> saved = new ArrayList<>();
        for (int i = 0; i < ProgramConsole.MOST_LINES + 10; i++) {
            saved.add(Text.literal("saved " + i));
        }
        saved.add(Text.literal("z".repeat(ProgramConsole.MOST_LINE_CHARACTERS + 1)));
        final ProgramConsole console = new ProgramConsole();

        console.restore(saved, 5);

        final List<String> kept = console.lines();
        assertEquals(ProgramConsole.MOST_LINES, kept.size());
        assertEquals(ProgramConsole.MOST_LINE_CHARACTERS, kept.getLast().length());
        assertEquals(ProgramConsole.MOST_LINES, console.written(), "never fewer written than kept");
    }

    @Test
    void clear_emptiesTheConsoleButKeepsTheCountOfWhatWasWritten() {
        final ProgramConsole console = new ProgramConsole();
        console.write("one");
        console.write("two");
        console.clear();
        assertEquals(List.of(), console.lines());
        assertEquals(2, console.written());
        console.write("three");
        assertEquals(List.of("three"), console.lines());
    }

    @Test
    void write_keepsASentenceForTheTerminalAndItsEnglishForTheProgram() {
        final ProgramConsole console = new ProgramConsole();
        final Text halted = TextKey.of("jsc.test.halted", "stopped at %s").with("7");
        console.write("printed");
        console.write(halted);
        assertEquals(List.of(Text.literal("printed"), halted), console.texts());
        assertEquals(List.of("printed", "stopped at 7"), console.lines());
    }

    @Test
    void written_countsPastWhatAnIntCanHold() {
        final ProgramConsole console = new ProgramConsole();
        console.restore(List.of(Text.literal("last")), Integer.MAX_VALUE);
        console.write("one more");
        assertEquals(Integer.MAX_VALUE + 1L, console.written());
    }
}
