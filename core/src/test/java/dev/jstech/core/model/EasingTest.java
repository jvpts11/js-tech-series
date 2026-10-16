/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EasingTest {

    private static final double EPSILON = 1.0e-6;

    @Test
    void apply_everyEasingStartsAtTheStartAndEndsAtTheEnd() {
        for (final Easing easing : Easing.values()) {
            if (easing == Easing.STEP || easing == Easing.CATMULLROM || easing == Easing.BEZIER) {
                continue;
            }
            assertEquals(0, easing.apply(0, Double.NaN), EPSILON, easing.serializedName());
            assertEquals(1, easing.apply(1, Double.NaN), EPSILON, easing.serializedName());
        }
    }

    @Test
    void apply_curvesAsTheirNamesSay() {
        assertEquals(0.25, Easing.EASE_IN_QUAD.apply(0.5, Double.NaN), EPSILON);
        assertEquals(0.75, Easing.EASE_OUT_QUAD.apply(0.5, Double.NaN), EPSILON);
        assertTrue(Easing.EASE_IN_BACK.apply(0.2, Double.NaN) < 0, "a back curve first goes the other way");
        assertTrue(Easing.EASE_OUT_ELASTIC.apply(0.1, Double.NaN) > 1, "an elastic one past its end");
    }

    @Test
    void byName_readsBlockbenchsNamesAndFallsBackToStraight() {
        assertEquals(Easing.EASE_IN_OUT_SINE, Easing.byName("easeInOutSine"));
        assertEquals(Easing.CATMULLROM, Easing.byName("catmullrom"));
        assertEquals(Easing.LINEAR, Easing.byName("nonsense"));
        assertEquals(Easing.LINEAR, Easing.byName(null));
    }
}
