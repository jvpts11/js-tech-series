/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TranscendenceFormulasTest {

    @Test
    void annihilation_ofAMilligramEachGives180Gigajoules() {
        assertEquals(1.797e11, TranscendenceFormulas.annihilation(1e-6), 1e9);
    }

    @Test
    void antiElementFactor_growsWithTheMass() {
        assertEquals(4.0, TranscendenceFormulas.antiElementFactor(4));
        assertEquals(207.0, TranscendenceFormulas.antiElementFactor(207));
    }

    @Test
    void singularity_givesAtMostFortyPercentOfTheMass() {
        assertEquals(0.4 * 1e-3 * TranscendenceFormulas.LIGHT * TranscendenceFormulas.LIGHT,
                TranscendenceFormulas.singularity(1e-3), 1.0);
    }
}
