/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.media.MediaFormat;
import org.junit.jupiter.api.Test;

class SetupTimingTest {

    private static int seconds(final int ticks) {
        return ticks / SetupTiming.TICKS_PER_SECOND;
    }

    @Test
    void ticks_readsTheProgramAtTheMediumsSpeed() {
        // 128 MB at a CD's 4 MB/s is 32 seconds; the same program on a DVD, 16 MB/s, is 8.
        assertEquals(32, seconds(SetupTiming.ticks(128, MediaFormat.CD, false)));
        assertEquals(8, seconds(SetupTiming.ticks(128, MediaFormat.DVD, false)));
        assertEquals(16, seconds(SetupTiming.ticks(16, MediaFormat.FLOPPY, false)));
    }

    @Test
    void ticks_neverGoesUnderTheFloorSoTheWindowIsSeen() {
        assertEquals(SetupTiming.MIN_SECONDS, seconds(SetupTiming.ticks(16, MediaFormat.USB, false)));
        assertEquals(SetupTiming.MIN_SECONDS, seconds(SetupTiming.ticks(0, MediaFormat.DVD, false)));
    }

    @Test
    void ticks_neverGoesOverTheCeilingSoNobodyWaitsForAGame() {
        assertEquals(SetupTiming.MAX_SECONDS, seconds(SetupTiming.ticks(512, MediaFormat.FLOPPY, false)));
        assertEquals(SetupTiming.MAX_SECONDS, seconds(SetupTiming.ticks(100_000, MediaFormat.USB, false)));
    }

    @Test
    void ticks_removingTakesAFifthOfInstalling() {
        final int install = SetupTiming.ticks(512, MediaFormat.DVD, false);
        final int remove = SetupTiming.ticks(512, MediaFormat.DVD, true);
        assertEquals(install / SetupTiming.REMOVE_DIVISOR, remove);
        // And never nothing: even a tiny program is seen going.
        assertTrue(SetupTiming.ticks(1, MediaFormat.USB, true) >= SetupTiming.TICKS_PER_SECOND);
    }

    @Test
    void ticks_treatsARateOfNothingAsTheSlowestMedium() {
        assertEquals(SetupTiming.ticks(16, MediaFormat.FLOPPY, false), SetupTiming.ticks(16, 0.0, false));
        assertEquals(SetupTiming.ticks(16, MediaFormat.FLOPPY, false), SetupTiming.ticks(16, -5.0, false));
    }

    @Test
    void networkTicks_readsAtTheNetworksRate() {
        assertEquals(16, seconds(SetupTiming.networkTicks(128, false)));
    }

    @Test
    void eraFactor_doublesWithEveryGenerationAndCutsTheTimeToMatch() {
        assertEquals(1, SetupTiming.eraFactor(dev.jstech.core.tier.HardwareEra.VINTAGE));
        assertEquals(2, SetupTiming.eraFactor(dev.jstech.core.tier.HardwareEra.LEGACY));
        assertEquals(4, SetupTiming.eraFactor(dev.jstech.core.tier.HardwareEra.STANDARD));
        assertEquals(1, SetupTiming.eraFactor(null));
        // A 128 MB program on a CD: 32 s on a Vintage machine, 8 s on a Standard one, never under the floor.
        assertEquals(32, seconds(SetupTiming.ticks(128, MediaFormat.CD, false, 1)));
        assertEquals(8, seconds(SetupTiming.ticks(128, MediaFormat.CD, false, 4)));
        assertEquals(SetupTiming.MIN_SECONDS, seconds(SetupTiming.ticks(16, MediaFormat.FLOPPY, false, 32)));
        assertEquals(4, seconds(SetupTiming.networkTicks(128, false, 4)));
    }

    @Test
    void rateOf_ordersTheMediaByAge() {
        assertTrue(SetupTiming.rateOf(MediaFormat.FLOPPY) < SetupTiming.rateOf(MediaFormat.CD));
        assertTrue(SetupTiming.rateOf(MediaFormat.CD) < SetupTiming.rateOf(MediaFormat.DVD));
        assertTrue(SetupTiming.rateOf(MediaFormat.DVD) < SetupTiming.rateOf(MediaFormat.USB));
    }

    /** One core at the reference clock is the machine everything else is measured against. */
    @Test
    void cpuFactor_readsAProcessorAgainstTheReferenceOne() {
        assertEquals(1.0, SetupTiming.cpuFactor(1, 2_000), 1e-9);
        assertEquals(2.0, SetupTiming.cpuFactor(2, 2_000), 1e-9);
        assertEquals(1.8, SetupTiming.cpuFactor(2, 1_800), 1e-9);
        assertEquals(14.0, SetupTiming.cpuFactor(8, 3_500), 1e-9);
    }

    /** However small the processor, it never drags an install below the floor on its own. */
    @Test
    void cpuFactor_neverFallsBelowItsFloor() {
        assertEquals(SetupTiming.CPU_MIN_FACTOR, SetupTiming.cpuFactor(1, 200), 1e-9);
        assertEquals(SetupTiming.CPU_MIN_FACTOR, SetupTiming.cpuFactor(0, 0), 1e-9);
        assertEquals(SetupTiming.CPU_MIN_FACTOR, SetupTiming.cpuFactor(-4, -100), 1e-9);
    }

    /** A missing disk tier reads as the slowest one there is rather than stopping the clock. */
    @Test
    void installRate_treatsNoDiskTierAsTheMechanicalOne() {
        assertEquals(SetupTiming.installRate(MediaFormat.CD, 1, 2, 2_000),
                SetupTiming.installRate(MediaFormat.CD, 0, 2, 2_000), 1e-9);
    }

    /**
     * The three parts each pull their own weight, and the disk is the one a player chooses on the page in
     * front of them: the same system off the same medium on the same processor lands four times sooner on a
     * solid-state disk than on a mechanical one.
     */
    @Test
    void installTicks_answersToTheMediumTheDiskAndTheProcessor() {
        // Frames XP, 1536 MB, off a CD on a two-core 1.8 GHz machine: 4 x 1 x 1.8 = 7.2 MB/s, past the ceiling.
        assertEquals(SetupTiming.MAX_SECONDS,
                seconds(SetupTiming.installTicks(1_536, MediaFormat.CD, 1, 2, 1_800)));
        // The same machine with a solid-state disk: four times the rate, and it comes in under the ceiling.
        assertEquals(53, seconds(SetupTiming.installTicks(1_536, MediaFormat.CD, 4, 2, 1_800)));
        // And on the disk after that, it is a glance: 4 x 16 x 1.8 = 115 MB/s.
        assertEquals(13, seconds(SetupTiming.installTicks(1_536, MediaFormat.CD, 16, 2, 1_800)));
    }

    /** Both ends stay where every other setup's do, so no install is missed or waited out forever. */
    @Test
    void installTicks_staysBetweenTheFloorAndTheCeiling() {
        assertEquals(SetupTiming.MIN_SECONDS,
                seconds(SetupTiming.installTicks(4, MediaFormat.USB, 16, 8, 3_500)));
        assertEquals(SetupTiming.MAX_SECONDS,
                seconds(SetupTiming.installTicks(20_480, MediaFormat.FLOPPY, 1, 1, 200)));
    }

    /**
     * A machine of the first age, installing the system of the first age off the medium of the first age.
     * Nothing about it is fast, and it is still over in seconds, because the system is four megabytes.
     */
    @Test
    void installTicks_leavesTheEarliestMachineWithAShortWait() {
        assertEquals(8, seconds(SetupTiming.installTicks(4, MediaFormat.FLOPPY, 1, 1, 200)));
    }
}
