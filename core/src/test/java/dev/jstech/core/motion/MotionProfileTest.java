/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MotionProfileTest {

    @Test
    void spec_ofAKindTheProfileDoesNotNameIsStill() {
        assertSame(MotionSpec.NONE, MotionProfile.STILL.spec(MotionKinds.WINDOW_OPEN));
    }

    @Test
    void json_thenRead_givesTheSameProfile() {
        final MotionProfile profile = MotionProfile.builder()
                .kind(MotionKinds.WINDOW_OPEN, MotionSpec.of(MotionStyles.SCALE, 150, IEasing.named("ease-out-expo"),
                        "animations").with("from", 0.01).with("from_y", 0.05).with("pivot_y", 1.0))
                .kind(MotionKinds.MENU_SHOW, MotionSpec.of(MotionStyles.SLIDE, 200, IEasing.named("steps(4)"), "")
                        .with("distance", 1.0).after(30))
                .build();
        assertEquals(profile, MotionProfile.read(profile.json()));
    }

    @Test
    void read_takesWhatAPackWritesByHand() {
        final MotionProfile read = MotionProfile.read(file("""
                {"window_close": {"style": "scale", "duration": 200, "easing": "cubic-bezier(1, 0, 1, 1)",
                                  "group": "scale", "to": 0.8, "note": "ignored"}}"""));
        final MotionSpec spec = read.spec(MotionKinds.WINDOW_CLOSE);
        assertEquals(MotionStyles.SCALE, spec.style());
        assertEquals(200, spec.duration());
        assertEquals("scale", spec.group());
        assertEquals(0.8, spec.param("to", 1.0));
        assertEquals(1, spec.params().size());
    }

    @Test
    void read_fillsWhatAnEntryLeavesOut() {
        final MotionSpec spec = MotionProfile.read(file("{\"menu_show\": {\"style\": \"slide\"}}"))
                .spec(MotionKinds.MENU_SHOW);
        assertEquals(0, spec.duration());
        assertEquals(0, spec.delay());
        assertSame(IEasing.LINEAR, spec.easing());
        assertEquals("", spec.group());
    }

    @Test
    void read_holdsANegativeTimeAtNought() {
        assertEquals(0, MotionProfile.read(file("{\"menu_show\": {\"style\": \"slide\", \"duration\": -5}}"))
                .spec(MotionKinds.MENU_SHOW).duration());
    }

    @Test
    void read_refusesWhatIsNoMotionFile() {
        assertThrows(IllegalArgumentException.class,
                () -> MotionProfile.read(file("{\"menu_show\": {\"duration\": 100}}")));
        assertThrows(IllegalArgumentException.class, () -> MotionProfile.read(file("{\"menu_show\": 3}")));
        assertThrows(IllegalArgumentException.class,
                () -> MotionProfile.read(file("{\"menu_show\": {\"style\": \"slide\", \"easing\": \"wobbly\"}}")));
        assertThrows(IllegalArgumentException.class, () -> MotionProfile.read(file("[1, 2]")));
    }

    private static byte[] file(final String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
