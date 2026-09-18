/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.tty;

import dev.jstech.computers.program.cli.CliLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TtyScriptProcessTest {

    private Glass glass;

    @BeforeEach
    void setUp() {
        this.glass = new Glass();
    }

    private static TtyScriptProcess started(final TtyScript script) {
        final TtyScriptProcess process = new TtyScriptProcess(script);
        process.begin(0);
        return process;
    }

    @Test
    void advance_aToolThatOnlySpeaks_saysItAllAtOnceAndIsOver() {
        final TtyScriptProcess tool = started(TtyScript.script().say("one").say("two").done());
        tool.advance(0, this.glass);
        assertEquals(List.of("one", "two"), this.glass.shown);
        assertTrue(tool.over());
    }

    @Test
    void advance_aPause_holdsEverythingAfterItUntilItsTimeIsUp() {
        final TtyScriptProcess tool = started(TtyScript.script().say("start").pause(20).say("end").done());
        tool.advance(0, this.glass);
        tool.advance(19, this.glass);
        assertEquals(List.of("start"), this.glass.shown);
        assertFalse(tool.over());
        tool.advance(20, this.glass);
        assertEquals(List.of("start", "end"), this.glass.shown);
        assertTrue(tool.over());
    }

    /** Moved along once after an hour it comes to the same place as moved along every tick. */
    @Test
    void advance_afterALongGap_comesToTheSamePlace() {
        final TtyScriptProcess tool = started(
                TtyScript.script().pause(10).say("a").pause(10).say("b").pause(10).say("c").done());
        tool.advance(25, this.glass);
        assertEquals(List.of("a", "b"), this.glass.shown);
        tool.advance(100_000, this.glass);
        assertEquals(List.of("a", "b", "c"), this.glass.shown);
        assertTrue(tool.over());
    }

    /** A bar is one line: drawn once below what was there, then over itself, and left at its last drawing. */
    @Test
    void advance_aBar_isDrawnOnceAndThenOverItself() {
        final TtyScriptProcess tool = started(TtyScript.script().say("before")
                .redraw(10, progress -> CliLine.plain((int) (progress * 100) + "%")).say("after").done());
        tool.advance(0, this.glass);
        tool.advance(5, this.glass);
        tool.advance(10, this.glass);
        assertEquals(List.of("before", "100%", "after"), this.glass.shown);
        assertEquals(2, this.glass.redraws, "the first drawing was a line, the two after it went over it");
    }

    @Test
    void advance_aFlood_poursItsLinesOutOverItsTime() {
        final TtyScriptProcess tool = started(
                TtyScript.script().flood(10, 100, index -> CliLine.plain("path " + index)).done());
        for (int tick = 0; tick <= 5; tick++) {
            tool.advance(tick, this.glass);
        }
        assertEquals(50, this.glass.shown.size());
        assertEquals("path 49", this.glass.shown.getLast());
        for (int tick = 6; tick <= 10; tick++) {
            tool.advance(tick, this.glass);
        }
        assertEquals(100, this.glass.shown.size());
        assertEquals("path 99", this.glass.shown.getLast());
        assertTrue(tool.over());
    }

    /** Nobody watched the start of it, so what they are given is what is going by now, not the backlog. */
    @Test
    void advance_aFloodNobodyWasWatching_doesNotPourTheBacklogOut() {
        final TtyScriptProcess tool = started(
                TtyScript.script().flood(100, 4000, index -> CliLine.plain("path " + index)).done());
        tool.advance(50, null);
        tool.advance(51, this.glass);
        assertEquals(TtyScript.MAX_LINES_PER_TICK, this.glass.shown.size());
        assertEquals("path 2039", this.glass.shown.getLast());
    }

    /** A flood asked for more than a terminal may be sent pours out what fits in its time. */
    @Test
    void flood_isNeverFasterThanTheTerminalMayBeSent() {
        final TtyScript script = TtyScript.script().flood(10, 1_000_000, index -> CliLine.plain("x")).done();
        final TtyScript.Flood flood = (TtyScript.Flood) script.steps().getFirst();
        assertEquals(10 * TtyScript.MAX_LINES_PER_TICK, flood.count());
    }

    @Test
    void advance_withNobodyWatching_stillDoesWhatTheToolWasRunFor() {
        final boolean[] fetched = {false};
        final TtyScriptProcess tool = started(TtyScript.script().say("fetching")
                .flood(10, 100, index -> CliLine.plain("x")).effect(() -> fetched[0] = true).done());
        tool.advance(5, null);
        assertFalse(fetched[0], "half way through it has fetched nothing");
        tool.advance(10, null);
        assertTrue(fetched[0]);
        assertTrue(tool.over());
    }

    /** Ctrl+C half way leaves what the tool was run for not done, which is the point of doing it last. */
    @Test
    void interrupt_halfWay_leavesTheWorkNotDone() {
        final boolean[] fetched = {false};
        final TtyScriptProcess tool = started(
                TtyScript.script().say("fetching").pause(100).effect(() -> fetched[0] = true).done());
        tool.advance(50, this.glass);
        tool.interrupt(50, this.glass);
        tool.advance(10_000, this.glass);
        assertFalse(fetched[0]);
        assertTrue(tool.over());
        assertEquals(List.of("fetching", "^C"), this.glass.shown);
    }

    @Test
    void asking_stopsTheToolUntilItIsAnswered() {
        final TtyScriptProcess tool = started(TtyScript.script().say("3 packages")
                .ask(CliLine.plain("Proceed? [Y/n] "), answer -> null).say("installing").done());
        tool.advance(0, this.glass);
        tool.advance(5_000, this.glass);
        assertNotNull(tool.asking());
        assertEquals("Proceed? [Y/n] ", tool.asking().text().text());
        assertEquals(List.of("3 packages"), this.glass.shown);
        tool.answer("y", 5_000, this.glass);
        assertNull(tool.asking());
        assertEquals(List.of("3 packages", "Proceed? [Y/n] y", "installing"), this.glass.shown);
        assertTrue(tool.over());
    }

    /** The wait for an answer was nobody's work: the clock starts again from it. */
    @Test
    void answer_startsTheClockAgain() {
        final TtyScriptProcess tool = started(TtyScript.script()
                .ask(CliLine.plain("go? "), answer -> null).pause(20).say("done").done());
        tool.advance(0, this.glass);
        tool.answer("y", 1_000, this.glass);
        tool.advance(1_019, this.glass);
        assertFalse(tool.over(), "twenty ticks from the answer, not from the start");
        tool.advance(1_020, this.glass);
        assertTrue(tool.over());
    }

    @Test
    void answer_canLeadSomewhereElse() {
        final boolean[] merged = {false};
        final TtyScriptProcess tool = started(TtyScript.script()
                .ask(CliLine.plain("merge? "), answer -> answer.startsWith("n")
                        ? TtyScript.script().say("Quitting.").stop().done() : null)
                .effect(() -> merged[0] = true).done());
        tool.advance(0, this.glass);
        tool.answer("n", 0, this.glass);
        assertTrue(tool.over());
        assertFalse(merged[0]);
        assertEquals(List.of("merge? n", "Quitting."), this.glass.shown);
    }

    /** A password is typed, taken and never shown, not even as dots. */
    @Test
    void answer_toAQuestionAskedUnseen_isKeptOffTheGlass() {
        final String[] taken = {""};
        final TtyScriptProcess tool = started(TtyScript.script()
                .askUnseen(CliLine.plain("New password: "), answer -> {
                    taken[0] = answer;
                    return null;
                }).done());
        tool.advance(0, this.glass);
        assertTrue(tool.asking().masked());
        tool.answer("hunter2", 0, this.glass);
        assertEquals("hunter2", taken[0]);
        assertEquals(List.of("New password: "), this.glass.shown);
    }

    @Test
    void answer_toAToolThatDidNotAsk_isDropped() {
        final TtyScriptProcess tool = started(TtyScript.script().pause(10).say("done").done());
        tool.advance(0, this.glass);
        tool.answer("hello", 0, this.glass);
        assertTrue(this.glass.shown.isEmpty());
        assertFalse(tool.over());
    }

    @Test
    void ticks_addUpEverythingThatTakesTime() {
        assertEquals(35, TtyScript.script().say("x").pause(5).redraw(10, p -> CliLine.plain(""))
                .flood(20, 5, i -> CliLine.plain("")).done().ticks());
    }

    /** What a terminal would show, kept the way a terminal keeps it. */
    private static final class Glass implements ITtySink {

        private final List<String> shown = new ArrayList<>();
        private int redraws;

        @Override
        public void line(final CliLine line) {
            this.shown.add(line.text());
        }

        @Override
        public void redraw(final CliLine line) {
            this.redraws++;
            this.shown.set(this.shown.size() - 1, line.text());
        }
    }
}
