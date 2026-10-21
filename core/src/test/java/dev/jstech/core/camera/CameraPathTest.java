/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.model.Easing;
import java.util.List;
import org.junit.jupiter.api.Test;

class CameraPathTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    void at_passesThroughEveryKeyframe() {
        final CameraPath path = path();
        for (final CameraPath.Keyframe keyframe : path.keyframes()) {
            final CameraPath.Sample sample = path.at(keyframe.time());
            assertEquals(keyframe.x(), sample.x(), CLOSE);
            assertEquals(keyframe.y(), sample.y(), CLOSE);
            assertEquals(keyframe.z(), sample.z(), CLOSE);
        }
    }

    @Test
    void at_holdsAtItsEnds() {
        final CameraPath path = path();
        assertEquals(path.at(-5).y(), path.at(0).y(), CLOSE);
        assertEquals(path.at(500).z(), path.at(path.length()).z(), CLOSE);
        assertEquals(40, path.length());
    }

    @Test
    void at_turnsTheShortWayRound() {
        final CameraPath path = new CameraPath(List.of(
                new CameraPath.Keyframe(0, 0, 0, 0, 170, 0, 0, 0, Easing.LINEAR),
                new CameraPath.Keyframe(10, 0, 0, 0, -170, 0, 0, 0, Easing.LINEAR)), true);
        assertEquals(180.0, path.at(5).yaw(), CLOSE, "from 170 to -170 through 180, not through 0");
    }

    @Test
    void at_keepsThePlayersFieldOfViewUntilBothEndsNameOne() {
        final CameraPath path = path();
        assertEquals(0.0, path.at(5).fov(), CLOSE, "the first two name none");
        assertTrue(path.at(30).fov() > 50.0 && path.at(30).fov() < 70.0, "between two that do, it goes over");
    }

    @Test
    void ordered_refusesKeyframesOutOfOrder() {
        assertFalse(CameraPath.ordered(List.of(new CameraPath.Keyframe(20, 0, 0, 0, 0, 0, 0, 0, Easing.LINEAR),
                new CameraPath.Keyframe(10, 1, 1, 1, 0, 0, 0, 0, Easing.LINEAR))));
        assertTrue(CameraPath.ordered(path().keyframes()));
    }

    @Test
    void constructor_refusesASingleKeyframe() {
        assertThrows(IllegalArgumentException.class, () -> new CameraPath(List.of(
                new CameraPath.Keyframe(0, 0, 0, 0, 0, 0, 0, 0, Easing.LINEAR)), true));
    }

    private static CameraPath path() {
        return new CameraPath(List.of(
                new CameraPath.Keyframe(0, 0, 2, -6, 0, 10, 0, 0, Easing.LINEAR),
                new CameraPath.Keyframe(10, 3, 4, -3, 30, 15, 0, 0, Easing.LINEAR),
                new CameraPath.Keyframe(25, 6, 6, 0, 60, 20, 0, 70, Easing.EASE_IN_OUT_SINE),
                new CameraPath.Keyframe(40, 6, 9, 4, 90, 30, 0, 50, Easing.LINEAR)), true);
    }
}
