/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootTimingTest {

    /** The sizes the catalogue gives these systems, so the numbers here are the ones a player meets. */
    private static final int MC_DOS_MB = 4;
    private static final int FRAMES_95_MB = 48;
    private static final int FRAMES_XP_MB = 1_536;
    private static final int FRAMES_11_MB = 20_480;
    private static final int DEBIAN_MB = 4_096;

    private static final int HDD = 1;
    private static final int SSD = 4;
    private static final int NVME = 16;

    private static int seconds(final int ticks) {
        return ticks / SetupTiming.TICKS_PER_SECOND;
    }

    @Test
    void postTicks_growWithWhatIsSeated() {
        final int bare = BootTiming.postTicks(1, 1, HardwareEra.VINTAGE);
        final int full = BootTiming.postTicks(4, 6, HardwareEra.VINTAGE);
        assertTrue(full > bare, "a machine with more in it takes longer to find it all");
    }

    /**
     * Each generation does the same work faster, and the floor never turns that around.
     *
     * <p>The second half is the part worth holding: a floor set too high would make a Legacy machine's
     * self-test longer than a modern one's, and the ordering the whole timing exists to express would run
     * backwards at the top end where nobody would think to look for it.
     */
    @Test
    void postTicks_shrinkWithTheGeneration() {
        final int vintage = BootTiming.postTicks(2, 2, HardwareEra.VINTAGE);
        final int legacy = BootTiming.postTicks(2, 2, HardwareEra.LEGACY);
        final int standard = BootTiming.postTicks(2, 2, HardwareEra.STANDARD);
        assertTrue(vintage > legacy && legacy > standard, "each generation does the same work faster");
    }

    @Test
    void postTicks_aVintageMachine_takesAboutAsLongAsItUsedTo() {
        // One module and one disk: 3.0 + 0.4 + 0.25 seconds, which is where the old fixed self-test sat.
        assertEquals(73, BootTiming.postTicks(1, 1, HardwareEra.VINTAGE));
    }

    /**
     * A modern machine does the work so fast that what it takes is the floor, not the work.
     *
     * <p>Two modules, a disk and a graphics card is 4.3 seconds of finding on the slowest machine there is,
     * and a modern one does it four times over, which lands under the least a self-test may take. The floor
     * is what it takes, and the floor is there so the lines can be read rather than glimpsed.
     */
    @Test
    void postTicks_aModernMachine_sitsAtTheFloor() {
        assertEquals((int) (BootTiming.POST_MIN_SECONDS * SetupTiming.TICKS_PER_SECOND),
                BootTiming.postTicks(2, 2, HardwareEra.STANDARD));
    }

    @Test
    void postTicks_neverGoUnderTheirFloor() {
        assertEquals((int) (BootTiming.POST_MIN_SECONDS * SetupTiming.TICKS_PER_SECOND),
                BootTiming.postTicks(2, 2, HardwareEra.SINGULARITY));
    }

    @Test
    void postTicks_aMachineStuffedWithParts_stopsAtTheCeiling() {
        assertEquals((int) (BootTiming.POST_MAX_SECONDS * SetupTiming.TICKS_PER_SECOND),
                BootTiming.postTicks(64, 64, HardwareEra.VINTAGE));
    }

    @Test
    void bootTicks_theSmallSystems_areAtTheFloor() {
        assertEquals((int) (BootTiming.BOOT_MIN_SECONDS * SetupTiming.TICKS_PER_SECOND),
                BootTiming.bootTicks(MC_DOS_MB, HDD, HardwareEra.VINTAGE, false));
        assertEquals((int) (BootTiming.BOOT_MIN_SECONDS * SetupTiming.TICKS_PER_SECOND),
                BootTiming.bootTicks(FRAMES_95_MB, HDD, HardwareEra.LEGACY, false));
    }

    @Test
    void bootTicks_aBigSystemOnAMechanicalDisk_reachesTheCeiling() {
        assertEquals((int) (BootTiming.BOOT_MAX_SECONDS * SetupTiming.TICKS_PER_SECOND),
                BootTiming.bootTicks(FRAMES_11_MB, HDD, HardwareEra.STANDARD, false));
    }

    @Test
    void bootTicks_theSameSystemOnAFasterDisk_isAWholeDifferentMachine() {
        final int mechanical = BootTiming.bootTicks(FRAMES_11_MB, HDD, HardwareEra.STANDARD, false);
        final int solid = BootTiming.bootTicks(FRAMES_11_MB, NVME, HardwareEra.STANDARD, false);
        assertEquals(20, seconds(mechanical));
        assertEquals(3, seconds(solid));
    }

    @Test
    void bootTicks_anOlderSystemOnAnOlderMachine_isTheWaitItWas() {
        assertEquals(5, seconds(BootTiming.bootTicks(FRAMES_XP_MB, HDD, HardwareEra.LEGACY, false)));
        assertEquals(13, seconds(BootTiming.bootTicks(DEBIAN_MB, HDD, HardwareEra.LEGACY, false)));
        assertEquals(2, seconds(BootTiming.bootTicks(DEBIAN_MB, SSD, HardwareEra.STANDARD, false)));
    }

    @Test
    void bootTicks_withNoMemoryToSpare_takeLonger() {
        final int roomy = BootTiming.bootTicks(FRAMES_XP_MB, HDD, HardwareEra.LEGACY, false);
        final int tight = BootTiming.bootTicks(FRAMES_XP_MB, HDD, HardwareEra.LEGACY, true);
        assertTrue(tight > roomy, "a machine with nothing to spare takes longer to come up");
    }

    @Test
    void tightRam_isWhatLeavesTheSystemNoHeadroom() {
        assertTrue(BootTiming.tightRam(64, 64), "exactly what the system holds is not room to work in");
        assertFalse(BootTiming.tightRam(128, 64), "twice what it holds is");
        assertFalse(BootTiming.tightRam(4, 0), "a system that says nothing of its memory asks nothing");
    }

    @Test
    void shutdownTicks_areAShareOfComingUp() {
        final int boot = BootTiming.bootTicks(FRAMES_XP_MB, HDD, HardwareEra.LEGACY, false);
        assertTrue(BootTiming.shutdownTicks(boot) < boot, "going down is quicker than coming up");
    }

    @Test
    void shutdownTicks_staysBetweenItsOwnLimits() {
        assertEquals((int) (BootTiming.SHUTDOWN_MIN_SECONDS * SetupTiming.TICKS_PER_SECOND),
                BootTiming.shutdownTicks(0));
        assertEquals((int) (BootTiming.SHUTDOWN_MAX_SECONDS * SetupTiming.TICKS_PER_SECOND),
                BootTiming.shutdownTicks(100_000));
    }
}
