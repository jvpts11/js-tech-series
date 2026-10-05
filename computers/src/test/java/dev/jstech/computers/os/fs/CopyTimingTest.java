/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.media.MediaFormat;
import org.junit.jupiter.api.Test;

class CopyTimingTest {

    private static final double CLOSE = 1e-9;

    @Test
    void diskRate_isTheMechanicalDiskTimesTheTier() {
        assertEquals(20.0, CopyTiming.diskRate(1), CLOSE);
        assertEquals(80.0, CopyTiming.diskRate(4), CLOSE);
        assertEquals(320.0, CopyTiming.diskRate(16), CLOSE);
        assertEquals(20.0, CopyTiming.diskRate(0), CLOSE);
    }

    @Test
    void slowest_isTheRateTheCopyGoesAt() {
        assertEquals(1.0, CopyTiming.slowest(CopyTiming.diskRate(16), CopyTiming.mediaRate(MediaFormat.FLOPPY)),
                CLOSE);
        assertEquals(20.0, CopyTiming.slowest(CopyTiming.diskRate(1), CopyTiming.diskRate(4)), CLOSE);
        assertEquals(80.0, CopyTiming.slowest(320.0, 80.0, CopyTiming.cableRate(4)), CLOSE);
    }

    @Test
    void cableRate_isFourMegabytesAnItemTwentyTimesASecond() {
        assertEquals(80.0, CopyTiming.cableRate(1), CLOSE);
        assertEquals(0.0, CopyTiming.cableRate(-3), CLOSE);
    }

    @Test
    void ticks_isTheSizeAtTheRateRoundedUp() {
        assertEquals(20, CopyTiming.ticks(20, 20.0));
        assertEquals(1, CopyTiming.ticks(1, 320.0));
        assertEquals(200, CopyTiming.ticks(10, CopyTiming.mediaRate(MediaFormat.FLOPPY)));
    }

    @Test
    void ticks_ofNothingOrOverNoRoadIsNone() {
        assertEquals(0, CopyTiming.ticks(0, 20.0));
        assertEquals(0, CopyTiming.ticks(50, 0.0));
    }

    @Test
    void showsWindow_onlyForACopyThatOutlastsAMoment() {
        assertFalse(CopyTiming.showsWindow(CopyTiming.ticks(5, CopyTiming.diskRate(1))));
        assertFalse(CopyTiming.showsWindow(CopyTiming.MOMENT_TICKS));
        assertTrue(CopyTiming.showsWindow(CopyTiming.ticks(10, CopyTiming.mediaRate(MediaFormat.FLOPPY))));
    }
}
