/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProgramLoadingTest {

    @Test
    void loadRate_isTheDiskAtTheProcessorsPace() {
        // A mechanical disk at 20 MB/s behind two cores at two gigahertz, twice the reference processor.
        assertEquals(40.0, ProgramLoading.loadRate(1, 2, 2_000), 1e-9);
        // A disk four times as fast in the same machine loads four times as fast.
        assertEquals(160.0, ProgramLoading.loadRate(4, 2, 2_000), 1e-9);
    }

    @Test
    void startMillis_takesASecondForALightProgramOnAQuickMachine() {
        // A tool of a megabyte on a solid-state disk behind four fast cores is up in hardly more than a second.
        final long millis = ProgramLoading.startMillis(1, ProgramLoading.loadRate(16, 4, 3_500));
        assertTrue(millis >= ProgramLoading.LEAST_MILLIS && millis < 1_010L, "about a second; got " + millis);
    }

    @Test
    void startMillis_growsWithTheProgramsWeight() {
        // 96 MB at 40 MB/s: a second and 2.4 more.
        assertEquals(3_400L, ProgramLoading.startMillis(96, 40.0));
        assertTrue(ProgramLoading.startMillis(48, 40.0) < ProgramLoading.startMillis(96, 40.0),
                "a lighter program comes up sooner on the same machine");
    }

    @Test
    void startMillis_growsOnASlowerMachine() {
        assertTrue(ProgramLoading.startMillis(48, ProgramLoading.loadRate(1, 1, 1_000))
                        > ProgramLoading.startMillis(48, ProgramLoading.loadRate(4, 2, 3_000)),
                "the same program comes up later on a slower disk and processor");
    }

    @Test
    void startMillis_neverPassesFiveSeconds() {
        assertEquals(ProgramLoading.MOST_MILLIS, ProgramLoading.startMillis(4_096, 10.0));
    }

    @Test
    void startMillis_readsARateOfNothingAsTheReferencePace() {
        assertEquals(ProgramLoading.startMillis(40, ProgramLoading.REFERENCE_MB_PER_SECOND),
                ProgramLoading.startMillis(40, 0.0));
    }
}
