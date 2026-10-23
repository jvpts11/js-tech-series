/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScienceFormulasTest {

    @Test
    void researchGroup_takesTheFirstAndNinetyPercentOfTheRest() {
        assertEquals(10.0 + 0.9 * 10.0 + 0.9 * 5.0, ScienceFormulas.researchGroup(10.0, 10.0, 5.0), 1e-12);
        assertEquals(0.0, ScienceFormulas.researchGroup());
    }

    @Test
    void synchrotron_reachesFurtherWithFieldAndRadius() {
        assertEquals(2.0 * ScienceFormulas.synchrotronMomentum(1.0, 10.0),
                ScienceFormulas.synchrotronMomentum(2.0, 10.0), 1e-12);
        assertEquals(0.299_79 * 4.0 * 1_000.0, ScienceFormulas.synchrotronMomentum(4.0, 1_000.0), 1e-9);
    }

    @Test
    void decayHeat_isAboutSevenPercentAtOnceAndFallsOverHours() {
        assertEquals(0.066, ScienceFormulas.decayHeat(1.0, 0.0), 1e-12);
        final double hour = ScienceFormulas.decayHeat(1.0, 3_600.0);
        assertTrue(hour > 0.01 && hour < 0.015, "an hour on: " + hour);
        assertTrue(ScienceFormulas.decayHeat(1.0, 86_400.0) < hour);
    }

    @Test
    void enrichment_stopsAtResearchReactorsTwentyPercent() {
        assertTrue(ScienceFormulas.enrichmentAllowed(0.04));
        assertTrue(ScienceFormulas.enrichmentAllowed(0.20));
        assertFalse(ScienceFormulas.enrichmentAllowed(0.9));
    }
}
