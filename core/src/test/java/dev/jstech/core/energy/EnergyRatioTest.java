/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EnergyRatioTest {

    @Test
    void one_convertsOneForOne() {
        assertEquals(1234L, EnergyRatio.ONE.toFe(1234L));
        assertEquals(1234L, EnergyRatio.ONE.fromFe(1234L));
    }

    @Test
    void constructor_reducesToLowestTerms() {
        assertEquals(new EnergyRatio(2L, 5L), new EnergyRatio(4L, 10L));
        assertEquals(2L, new EnergyRatio(4L, 10L).units());
    }

    @Test
    void toFe_convertsByTheRatio() {
        // Two of the unit are worth five FE.
        final EnergyRatio ratio = new EnergyRatio(2L, 5L);

        assertEquals(25L, ratio.toFe(10L));
        assertEquals(4L, ratio.fromFe(10L));
    }

    @Test
    void conversions_roundDownSoNoEnergyIsMade() {
        final EnergyRatio ratio = new EnergyRatio(3L, 1L);

        assertEquals(0L, ratio.toFe(2L));
        assertEquals(1L, ratio.toFe(5L));
        assertEquals(3L, ratio.fromFe(1L));
    }

    @Test
    void conversions_saturateRatherThanOverflow() {
        final EnergyRatio ratio = new EnergyRatio(1L, 1_000L);

        assertEquals(Long.MAX_VALUE, ratio.toFe(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE / 1_000L, ratio.fromFe(Long.MAX_VALUE));
        assertEquals(9_000_000_000_000L, ratio.toFe(9_000_000_000L));
    }

    @Test
    void conversions_refuseANegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> EnergyRatio.ONE.toFe(-1L));
    }

    @Test
    void constructor_refusesAnAmountOfNothing() {
        assertThrows(IllegalArgumentException.class, () -> new EnergyRatio(0L, 1L));
        assertThrows(IllegalArgumentException.class, () -> new EnergyRatio(1L, -2L));
    }
}
