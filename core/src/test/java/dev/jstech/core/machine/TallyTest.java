/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TallyTest {

    private static final String SCRAP = "jscore:copper_scrap";

    @Test
    void add_givesAQuarterEveryFourthTimeExactly() {
        final Tally tally = new Tally();
        final int[] given = new int[8];
        for (int time = 0; time < given.length; time++) {
            given[time] = tally.add(SCRAP, 0.25);
        }
        assertEquals("[0, 0, 0, 1, 0, 0, 0, 1]", Arrays.toString(given));
    }

    @Test
    void add_countsTenTenthsAsAWhole() {
        final Tally tally = new Tally();
        int total = 0;
        for (int time = 0; time < 10; time++) {
            total += tally.add(SCRAP, 0.1);
        }
        assertEquals(1, total);
        assertEquals(0.0, tally.held(SCRAP), 1e-9);
    }

    @Test
    void add_givesMoreThanOneForAShareAboveTheWhole() {
        final Tally tally = new Tally();
        assertEquals(1, tally.add(SCRAP, 1.5));
        assertEquals(2, tally.add(SCRAP, 1.5));
    }

    @Test
    void add_keepsEachThingsCountApart() {
        final Tally tally = new Tally();
        tally.add(SCRAP, 0.5);
        assertEquals(0, tally.add("jscore:sulfur_dust", 0.5));
        assertEquals(1, tally.add(SCRAP, 0.5));
    }

    @Test
    void peek_tellsWithoutChangingTheCount() {
        final Tally tally = new Tally();
        tally.add(SCRAP, 0.75);
        assertEquals(1, tally.peek(SCRAP, 0.25));
        assertEquals(0.75, tally.held(SCRAP), 1e-9);
    }

    @Test
    void restore_takesUpWhatWasKept() {
        final Tally tally = new Tally();
        tally.add(SCRAP, 0.625);
        final Map<String, Double> kept = tally.snapshot();
        final Tally loaded = new Tally();
        loaded.restore(kept);
        assertEquals(1, loaded.add(SCRAP, 0.375));
        assertTrue(loaded.snapshot().isEmpty());
    }

    @Test
    void add_refusesLessThanNothing() {
        assertThrows(IllegalArgumentException.class, () -> new Tally().add(SCRAP, -0.1));
    }
}
