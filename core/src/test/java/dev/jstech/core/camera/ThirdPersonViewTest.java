/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class ThirdPersonViewTest {

    @Test
    void behind_standsBackFromTheEyeEasedAsAChangeOfViewIs() {
        final ThirdPersonView view = ThirdPersonView.behind(6);
        assertEquals(6, view.distance());
        assertEquals(0, view.right());
        assertEquals(0, view.up());
        assertNull(view.pivot());
        assertEquals(ThirdPersonView.EASED, view.seconds());
    }

    @Test
    void shoulderAroundAndEasedOver_keepWhatTheyDoNotChange() {
        final Vector3d pivot = new Vector3d(1, 70, -3);
        final ThirdPersonView view = ThirdPersonView.behind(9).shoulder(1.2, -0.4).around(pivot).easedOver(0);
        assertEquals(9, view.distance());
        assertEquals(1.2, view.right());
        assertEquals(-0.4, view.up());
        assertEquals(pivot, view.pivot());
        assertEquals(0, view.seconds());
        assertNotSame(pivot, view.pivot(), "the place is its own, not the caller's to move");
    }

    @Test
    void view_refusesWhatIsNoView() {
        assertThrows(IllegalArgumentException.class, () -> ThirdPersonView.behind(0));
        assertThrows(IllegalArgumentException.class, () -> ThirdPersonView.behind(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> ThirdPersonView.behind(4).shoulder(Double.POSITIVE_INFINITY,
                0));
        assertThrows(IllegalArgumentException.class, () -> ThirdPersonView.behind(4).easedOver(-1));
        assertThrows(IllegalArgumentException.class, () -> ThirdPersonView.behind(4)
                .around(new Vector3d(Double.NaN, 0, 0)));
    }
}
