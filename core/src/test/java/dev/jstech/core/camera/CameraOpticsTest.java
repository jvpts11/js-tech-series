/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CameraOpticsTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    void fieldOfView_narrowsAsALensDoes() {
        final double fov = CameraOptics.fieldOfView(70.0, 8.0);
        assertEquals(Math.tan(Math.toRadians(35.0)) / 8.0, Math.tan(Math.toRadians(fov / 2.0)), CLOSE,
                "a lens of 8 shows an eighth of the width");
    }

    @Test
    void fieldOfView_leavesItAsItIsWithoutMagnifying() {
        assertEquals(70.0, CameraOptics.fieldOfView(70.0, 1.0), CLOSE);
        assertEquals(70.0, CameraOptics.fieldOfView(70.0, 0.5), CLOSE, "a power below one is none");
    }

    @Test
    void sensitivity_turnsTheViewAsMuchSlower() {
        final double base = 0.5 * 0.6 + 0.2;
        final double through = CameraOptics.sensitivity(0.5, 8.0) * 0.6 + 0.2;
        assertEquals(base * base * base / 8.0, through * through * through, CLOSE,
                "the game's speed, the cube of the sensitivity's, is divided by the power");
    }

    @Test
    void sensitivity_leavesItAsItIsWithoutMagnifying() {
        assertEquals(0.5, CameraOptics.sensitivity(0.5, 1.0), CLOSE);
    }

    @Test
    void step_movesOnePowerAndHoldsAtTheEnds() {
        assertEquals(1, CameraOptics.step(0, 3, true));
        assertEquals(2, CameraOptics.step(2, 3, true), "held at the strongest");
        assertEquals(0, CameraOptics.step(0, 3, false), "held at the weakest");
        assertEquals(0, CameraOptics.step(0, 0, true), "no powers, nowhere to go");
    }
}
