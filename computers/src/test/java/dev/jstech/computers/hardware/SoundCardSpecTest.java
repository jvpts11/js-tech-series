/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.hardware.SoundCardSpec.SampleRate;
import dev.jstech.computers.hardware.SoundCardSpec.Synthesis;
import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SoundCardSpecTest {

    @Test
    void kind_isSound() {
        assertEquals(ExpansionCardKind.SOUND, card(9, 8, 5).kind());
    }

    @Test
    void sampleRate_hertzAreTheTwoRatesOfThoseCards() {
        assertEquals(22_050, SampleRate.KHZ_22.hertz());
        assertEquals(44_100, SampleRate.KHZ_44.hertz());
    }

    @Test
    void constructor_refusesNoVoices() {
        assertThrows(IllegalArgumentException.class, () -> card(0, 8, 5));
    }

    @Test
    void constructor_refusesASampleSizeNoCardHad() {
        assertThrows(IllegalArgumentException.class, () -> card(9, 12, 5));
    }

    @Test
    void constructor_refusesANegativeDraw() {
        assertThrows(IllegalArgumentException.class, () -> card(9, 8, -1));
    }

    @Test
    void constructor_refusesNoBus() {
        assertThrows(NullPointerException.class, () -> new SoundCardSpec(HardwareEra.VINTAGE, null, 5,
                Synthesis.FM, 9, 8, false, SampleRate.KHZ_22));
    }

    private static SoundCardSpec card(final int voices, final int sampleBits, final int tdpWatts) {
        return new SoundCardSpec(HardwareEra.VINTAGE, PcieGeneration.ISA, tdpWatts, Synthesis.FM, voices,
                sampleBits, false, SampleRate.KHZ_22);
    }
}
