/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaSizesTest {

    @Test
    void soldAs_isWhatEachFormatsBoxSays() {
        assertEquals("1.44 MB", MediaSizes.soldAs(MediaFormat.FLOPPY, null));
        assertEquals("700 MB", MediaSizes.soldAs(MediaFormat.CD, null));
        assertEquals("4.7 GB", MediaSizes.soldAs(MediaFormat.DVD, null));
        assertEquals("32 GB", MediaSizes.soldAs(MediaFormat.USB, null));
        assertEquals("25 GB", MediaSizes.soldAs(MediaFormat.BLU_RAY, null));
    }

    @Test
    void soldAs_ofAStickIsItsModelsDecimalGigabytes() {
        assertEquals("8 GB", MediaSizes.soldAs(MediaFormat.USB, UsbStick.POCKET_8G));
        assertEquals("16 GB", MediaSizes.soldAs(MediaFormat.USB, UsbStick.POCKET_16G));
        assertEquals("32 GB", MediaSizes.soldAs(MediaFormat.USB, UsbStick.POCKET_32G));
        assertEquals("64 GB", MediaSizes.soldAs(MediaFormat.USB, UsbStick.POCKET_64G));
    }

    @Test
    void grouped_writesTheExactBytesInThousands() {
        assertEquals("1,474,560", MediaSizes.grouped(1_474_560L));
        assertEquals("25,025,314,816", MediaSizes.grouped(MediaFormat.BLU_RAY.capacityBytes()));
        assertEquals("0", MediaSizes.grouped(0L));
    }

    @Test
    void label_keepsADecimalBelowTenAndRoundsDown() {
        assertEquals("1.2 MB", MediaSizes.label(1_310_720L));
        assertEquals("4.1 GB", MediaSizes.label(4_456_028_160L));
        assertEquals("512 MB", MediaSizes.label(512L * 1024L * 1024L));
        assertEquals("25 GB", MediaSizes.label(25L * 1024L * 1024L * 1024L + 1L));
        assertEquals("1.9 MB", MediaSizes.label(2L * 1024L * 1024L - 1L));
    }

    @Test
    void label_ofLittleOrNothingIsInKilobytes() {
        assertEquals("0 KB", MediaSizes.label(0L));
        assertEquals("0 KB", MediaSizes.label(-5L));
        assertEquals("0.5 KB", MediaSizes.label(512L));
        assertEquals("1 KB", MediaSizes.label(1024L));
    }

    @Test
    void sticks_areOfTheStandardButTheSixtyFourOfTheAdvanced() {
        assertEquals(HardwareEra.STANDARD, UsbStick.POCKET_8G.era());
        assertEquals(HardwareEra.STANDARD, UsbStick.POCKET_16G.era());
        assertEquals(HardwareEra.STANDARD, UsbStick.POCKET_32G.era());
        assertEquals(HardwareEra.ADVANCED, UsbStick.POCKET_64G.era());
        assertEquals(MediaFormat.USB.capacityBytes(), UsbStick.POCKET_32G.capacityBytes());
    }
}
