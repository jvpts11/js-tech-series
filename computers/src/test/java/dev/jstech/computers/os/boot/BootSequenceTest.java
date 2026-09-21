/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootSequenceTest {

    private static BootSequence fourSteps() {
        return new BootSequence.Builder()
                .title("MC-NET 1.0")
                .subtitle("(C) 1992 Nouvell Networks Inc.")
                .line("network link", "done")
                .line("mainframe", "CORE")
                .line("index", "1,204 item types")
                .line("services", "IQL Engine")
                .build();
    }

    @Test
    void shownAt_walksTheStepsAcrossTheTimeTheSystemTakes() {
        final BootSequence sequence = fourSteps();
        assertEquals(0, sequence.shownAt(0, 200));
        assertEquals(1, sequence.shownAt(50, 200));
        assertEquals(2, sequence.shownAt(100, 200));
        assertEquals(4, sequence.shownAt(200, 200));
    }

    @Test
    void shownAt_pastTheEnd_staysAtTheLastStep() {
        assertEquals(4, fourSteps().shownAt(10_000, 200));
    }

    @Test
    void shownAt_aSequenceWithNoSteps_showsNone() {
        assertEquals(0, BootSequence.NONE.shownAt(100, 200));
    }

    @Test
    void shownAt_noLength_showsNone() {
        assertEquals(0, fourSteps().shownAt(100, 0));
    }

    @Test
    void aSequenceWithNothingInIt_isEmpty() {
        assertTrue(BootSequence.NONE.isEmpty());
        assertFalse(fourSteps().isEmpty());
    }

    @Test
    void aLineWithNothingBesideIt_readsAsOneLabel() {
        final BootSequence.Line line = BootSequence.Line.of("Starting MC-DOS...");
        assertEquals("Starting MC-DOS...", line.label());
        assertEquals("", line.value());
    }

    @Test
    void moreStepsThanASequenceHolds_areCutToWhatItHolds() {
        final List<BootSequence.Line> many = new ArrayList<>();
        for (int i = 0; i < BootSequence.MOST_LINES + 8; i++) {
            many.add(BootSequence.Line.of("step " + i));
        }
        assertEquals(BootSequence.MOST_LINES, new BootSequence("t", "s", many).lines().size());
    }

    @Test
    void nothingHandedIn_readsAsEmptyRatherThanNothingAtAll() {
        final BootSequence sequence = new BootSequence(null, null, null);
        assertEquals("", sequence.title());
        assertEquals("", sequence.subtitle());
        assertTrue(sequence.lines().isEmpty());
    }
}
