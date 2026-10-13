/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MotionSpecTest {

    @Test
    void with_refusesANameThatIsAFieldOfTheMotion() {
        assertThrows(IllegalArgumentException.class, () -> MotionSpec.NONE.with("duration", 5.0));
        assertThrows(IllegalArgumentException.class, () -> MotionSpec.NONE.with("delay", 5.0));
    }

    @Test
    void constructor_refusesANegativeTime() {
        assertThrows(IllegalArgumentException.class,
                () -> new MotionSpec(MotionStyles.SCALE, -1, 0, IEasing.LINEAR, "", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new MotionSpec(MotionStyles.SCALE, 10, -1, IEasing.LINEAR, "", Map.of()));
    }

    @Test
    void moves_onlyWithAStyleAndATime() {
        assertFalse(MotionSpec.NONE.moves());
        assertFalse(MotionSpec.of(MotionStyles.SCALE, 0, IEasing.LINEAR, "").moves());
        assertFalse(MotionSpec.of(MotionStyles.NONE, 200, IEasing.LINEAR, "").moves());
        assertTrue(MotionSpec.of(MotionStyles.SCALE, 200, IEasing.LINEAR, "").moves());
    }

    @Test
    void with_addsANumberAndKeepsTheRest() {
        final MotionSpec spec = MotionSpec.of(MotionStyles.SCALE, 200, IEasing.LINEAR, "scale").with("from", 0.8);
        assertEquals(0.8, spec.param("from", 1.0));
        assertEquals(1.0, spec.param("to", 1.0));
        assertEquals("scale", spec.group());
        assertEquals(200, spec.duration());
    }

    @Test
    void after_waitsBeforeItStarts() {
        assertEquals(50, MotionSpec.of(MotionStyles.SLIDE, 200, IEasing.LINEAR, "").after(50).delay());
    }

    @Test
    void slowed_scalesTheTimeAndTheWaitTogether() {
        final MotionSpec spec = MotionSpec.of(MotionStyles.SCALE, 200, IEasing.LINEAR, "").after(40);
        final MotionSpec slow = spec.slowed(150);
        assertEquals(300, slow.duration());
        assertEquals(60, slow.delay());
        assertEquals(spec, spec.slowed(100));
    }

    @Test
    void slowed_toNothingStopsItMoving() {
        assertFalse(MotionSpec.of(MotionStyles.SCALE, 200, IEasing.LINEAR, "").slowed(0).moves());
        assertFalse(MotionSpec.of(MotionStyles.SCALE, 200, IEasing.LINEAR, "").slowed(-20).moves());
    }

    @Test
    void groups_splitsTheSwitchesOnSpaces() {
        assertEquals(List.of("effects", "map"),
                MotionSpec.of(MotionStyles.SCALE, 120, IEasing.LINEAR, " effects  map ").groups());
        assertEquals(List.of(), MotionSpec.NONE.groups());
    }
}
