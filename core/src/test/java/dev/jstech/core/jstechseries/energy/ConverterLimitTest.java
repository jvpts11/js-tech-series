/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConverterLimitTest {

    @Test
    void ceiling_risesFromAHundredKilowattsToTwelveGigawatts() {
        assertEquals(100_000L, ConverterLimit.ceiling(VoltageClass.LOW));
        assertEquals(10_000_000L, ConverterLimit.ceiling(VoltageClass.MEDIUM));
        assertEquals(200_000_000L, ConverterLimit.ceiling(VoltageClass.HIGH));
        assertEquals(1_500_000_000L, ConverterLimit.ceiling(VoltageClass.EXTRA_HIGH));
        assertEquals(12_000_000_000L, ConverterLimit.ceiling(VoltageClass.ULTRA_HIGH));
        long below = 0L;
        for (final VoltageClass each : VoltageClass.values()) {
            assertTrue(ConverterLimit.ceiling(each) > below, each.path());
            below = ConverterLimit.ceiling(each);
        }
    }

    @Test
    void of_writesWattsInTheSmallestPrefixThatHoldsThemInFourDigits() {
        assertEquals(new ConverterLimit(100, ConverterLimit.KILO), ConverterLimit.of(100_000L));
        assertEquals(new ConverterLimit(10, ConverterLimit.MEGA), ConverterLimit.of(10_000_000L));
        assertEquals(new ConverterLimit(1500, ConverterLimit.MEGA), ConverterLimit.of(1_500_000_000L));
        assertEquals(new ConverterLimit(12, ConverterLimit.GIGA), ConverterLimit.of(12_000_000_000L));
        assertEquals(new ConverterLimit(9999, ConverterLimit.KILO), ConverterLimit.of(9_999_000L));
    }

    @Test
    void of_roundsDownWhatFourDigitsCannotHold() {
        assertEquals(new ConverterLimit(10, ConverterLimit.MEGA), ConverterLimit.of(10_000_500L));
        assertEquals(new ConverterLimit(0, ConverterLimit.KILO), ConverterLimit.of(999L));
        assertEquals(new ConverterLimit(0, ConverterLimit.KILO), ConverterLimit.of(-5L));
    }

    @Test
    void watts_multipliesTheDigitsByThePrefix() {
        assertEquals(500_000L, new ConverterLimit(500, ConverterLimit.KILO).watts());
        assertEquals(9_999_000_000_000L, new ConverterLimit(9999, ConverterLimit.GIGA).watts());
    }

    @Test
    void turned_movesOneDrumAndWrapsRoundAsAThumbwheel() {
        final ConverterLimit limit = new ConverterLimit(509, ConverterLimit.KILO);

        assertEquals(1509, limit.turned(0, true).digits());
        assertEquals(409, limit.turned(1, false).digits());
        assertEquals(500, limit.turned(3, true).digits());
        assertEquals(508, limit.turned(3, false).digits());
        assertEquals(9509, limit.turned(0, false).digits());
        assertEquals(ConverterLimit.KILO, limit.turned(2, true).power());
    }

    @Test
    void turned_refusesADrumThatIsNotThere() {
        final ConverterLimit limit = new ConverterLimit(1, ConverterLimit.KILO);

        assertThrows(IllegalArgumentException.class, () -> limit.turned(ConverterLimit.DRUMS, true));
        assertThrows(IllegalArgumentException.class, () -> limit.turned(-1, true));
    }

    @Test
    void nextPrefix_turnsFromKiloToMegaToGigaAndRound() {
        final ConverterLimit kilo = new ConverterLimit(42, ConverterLimit.KILO);

        assertEquals(ConverterLimit.MEGA, kilo.nextPrefix().power());
        assertEquals(ConverterLimit.GIGA, kilo.nextPrefix().nextPrefix().power());
        assertEquals(kilo, kilo.nextPrefix().nextPrefix().nextPrefix());
    }

    @Test
    void drumsTextAndPrefixSymbol_showWhatTheWheelsShow() {
        final ConverterLimit limit = new ConverterLimit(500, ConverterLimit.KILO);

        assertEquals("0500", limit.drumsText());
        assertEquals("k", limit.prefixSymbol());
        assertEquals("M", limit.nextPrefix().prefixSymbol());
        assertEquals("G", limit.nextPrefix().nextPrefix().prefixSymbol());
    }

    @Test
    void constructor_refusesWhatTheDrumsCannotShow() {
        assertThrows(IllegalArgumentException.class, () -> new ConverterLimit(10_000, ConverterLimit.KILO));
        assertThrows(IllegalArgumentException.class, () -> new ConverterLimit(-1, ConverterLimit.KILO));
        assertThrows(IllegalArgumentException.class, () -> new ConverterLimit(1, 4));
    }
}
