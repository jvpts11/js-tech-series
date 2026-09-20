/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JobWhenTest {

    /** The world's clock at that hour of the first day. */
    private static long clockAt(final int hour) {
        return (long) Math.floorMod(hour - JobWhen.DAY_STARTS_AT, 24) * JobWhen.HOUR_TICKS;
    }

    @Test
    void atOnce_isDueTheMomentItIsAsked() {
        assertTrue(JobWhen.AT_ONCE.once());
        assertTrue(JobWhen.AT_ONCE.dueAt(0L, -1L));
        assertTrue(JobWhen.AT_ONCE.dueAt(clockAt(13), 0L));
    }

    @Test
    void hourOf_readsTheWorldsOwnClock() {
        assertEquals(6, JobWhen.hourOf(0L), "a day here starts at six in the morning");
        assertEquals(7, JobWhen.hourOf(JobWhen.HOUR_TICKS));
        assertEquals(18, JobWhen.hourOf(12L * JobWhen.HOUR_TICKS));
        assertEquals(6, JobWhen.hourOf((long) JobWhen.DAY_TICKS), "and comes round again the next day");
    }

    @Test
    void dueAt_isDueOnlyInItsOwnHour() {
        final JobWhen six = JobWhen.at(6);

        assertTrue(six.dueAt(clockAt(6), -1L));
        assertFalse(six.dueAt(clockAt(7), -1L));
        assertFalse(six.dueAt(clockAt(5), -1L));
    }

    @Test
    void dueAt_doesNotRunTwiceInTheSameHour() {
        final JobWhen six = JobWhen.at(6);
        final long ran = clockAt(6);

        assertFalse(six.dueAt(ran + 200L, ran), "still the same hour");
        assertTrue(six.dueAt(ran + JobWhen.DAY_TICKS, ran), "and due again the next day");
    }

    @Test
    void dueAt_keepsToTheDaysItNames() {
        final JobWhen mondays = JobWhen.at(6, List.of(0));

        assertTrue(mondays.dueAt(clockAt(6), -1L), "the first day of the week");
        assertFalse(mondays.dueAt(JobWhen.DAY_TICKS + clockAt(6), -1L), "but not the second");
    }

    @Test
    void daysOf_readsTheLettersADayIsWrittenWith() {
        assertEquals(List.of(0, 2, 4), JobWhen.daysOf("M,W,F"));
        assertEquals(List.of(0, 2, 4), JobWhen.daysOf("m, w, f"));
        assertEquals(List.of(3, 5), JobWhen.daysOf("Th,Sa"), "two letters are read before one");
        assertEquals(List.of(), JobWhen.daysOf("nonsense"));
    }

    @Test
    void hourOf_readsATimeWrittenEitherWay() {
        assertEquals(6, JobWhen.hourOf("06:00"));
        assertEquals(6, JobWhen.hourOf("6"));
        assertEquals(23, JobWhen.hourOf("23:30"));
        assertEquals(-1, JobWhen.hourOf("25:00"));
        assertEquals(-1, JobWhen.hourOf("soon"));
    }

    @Test
    void label_readsAsAPersonWouldSayIt() {
        assertEquals("running", JobWhen.AT_ONCE.label());
        assertEquals("06:00", JobWhen.at(6).label());
        assertEquals("06:00 M W F", JobWhen.at(6, List.of(0, 2, 4)).label());
    }
}
