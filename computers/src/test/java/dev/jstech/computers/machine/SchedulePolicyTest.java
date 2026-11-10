/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SystemFacts;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulePolicyTest {

    @Test
    void of_takesTheModsWayOnOneOfTheModsKernels() {
        assertEquals(SchedulePolicy.MOD, SchedulePolicy.of(SystemFacts.Kernel.of("linux"), true));
        assertEquals(SchedulePolicy.MOD, SchedulePolicy.of(null, false));
    }

    @Test
    void of_readsTheSchedulerAKernelOfItsOwnChose() {
        final SchedulePolicy cooperative = SchedulePolicy.of(kernel("cooperative", 64), false);
        final SchedulePolicy none = SchedulePolicy.of(kernel("none", 64), false);

        assertEquals(SchedulePolicy.Kind.COOPERATIVE, cooperative.kind());
        assertEquals(SchedulePolicy.Kind.NONE, none.kind());
        assertTrue(cooperative.highTwice(), "a high program gets two slices on a kernel a player made");
        assertFalse(SchedulePolicy.MOD.highTwice(), "and runs like medium on the mod's");
    }

    @Test
    void of_holdsTheSliceBetweenItsLeastAndMost() {
        assertEquals(SystemFacts.LEAST_SLICE, SchedulePolicy.of(kernel("preemptive", 2), false).slice());
        assertEquals(SystemFacts.MOST_SLICE, SchedulePolicy.of(kernel("preemptive", 9_000), false).slice());
        assertEquals(100, SchedulePolicy.of(kernel("preemptive", 100), false).slice());
    }

    @Test
    void of_keepsWhetherTheSystemChoosesEachSlice() {
        assertTrue(SchedulePolicy.of(kernel("preemptive", 64), true).chooses());
    }

    /* A kernel of its own with that scheduler and slice, and the rest as a kernel of its own starts. */
    private static SystemFacts.Kernel kernel(final String scheduler, final int slice) {
        return new SystemFacts.Kernel("", scheduler, slice, "posix", List.of("text"), "end", "posix");
    }
}
