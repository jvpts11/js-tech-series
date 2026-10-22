/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class CameraMotionsTest {

    private static final double EPSILON = 1.0e-9;
    private static final double FRAME = 1.0 / 60.0;

    @Test
    void noise_staysBetweenMinusOneAndOneAndWandersSmoothly() {
        double last = CameraMotions.noise(0, 3);
        double most = 0;
        for (int i = 1; i <= 10_000; i++) {
            final double at = CameraMotions.noise(i * 0.01, 3);
            assertTrue(at >= -1 && at <= 1, "noise " + at);
            most = Math.max(most, Math.abs(at - last));
            last = at;
        }
        assertTrue(most < 0.05, "a hundredth of a step moves it little: " + most);
    }

    @Test
    void noise_isTheSameForTheSameSeedAndAnotherForAnother() {
        assertEquals(CameraMotions.noise(4.37, 11), CameraMotions.noise(4.37, 11), EPSILON);
        assertNotEquals(CameraMotions.noise(4.37, 11), CameraMotions.noise(4.37, 12));
    }

    @Test
    void shakeDegrees_growsAsTheSquareOfTheStrength() {
        assertEquals(CameraMotions.MOST_DEGREES, CameraMotions.shakeDegrees(1), EPSILON);
        assertEquals(CameraMotions.MOST_DEGREES / 4, CameraMotions.shakeDegrees(0.5), EPSILON);
        assertEquals(CameraMotions.MOST_DEGREES, CameraMotions.shakeDegrees(3), EPSILON, "held to 1");
    }

    @Test
    void settle_getsThereWithoutSwingingPast() {
        double value = 0;
        double speed = 0;
        double most = 0;
        for (int frame = 0; frame < 120; frame++) {
            final double[] next = CameraMotions.settle(value, speed, 20, 0.3, FRAME);
            value = next[0];
            speed = next[1];
            most = Math.max(most, value);
        }
        assertEquals(20, value, 0.01, "two seconds on, it is there");
        assertTrue(most <= 20 + 1.0e-6, "and never went past: " + most);
    }

    @Test
    void shake_feltLessFartherAndDiesAway() {
        final CameraShake shake = CameraShake.of(0.8, 2).from(new Vector3d(0, 64, 0), 20);
        assertEquals(0.8, shake.felt(0), EPSILON);
        assertEquals(0.2, shake.felt(10), EPSILON, "half way out, a quarter as strong");
        assertEquals(0, shake.felt(25), EPSILON, "none past its reach");
        assertEquals(1, shake.left(0), EPSILON);
        assertEquals(0.25, shake.left(1), EPSILON);
        assertEquals(0, shake.left(2), EPSILON);
        assertEquals(0.8, CameraShake.of(0.8, 2).felt(1000), EPSILON, "one from nowhere is felt the same everywhere");
    }

    @Test
    void shake_refusesNoTime() {
        assertThrows(IllegalArgumentException.class, () -> CameraShake.of(0.5, 0));
        assertThrows(IllegalArgumentException.class, () -> CameraShake.of(0.5, 1).from(new Vector3d(), 0));
    }
}
