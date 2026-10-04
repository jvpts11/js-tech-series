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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MotionTest {

    private static final double CLOSE = 1e-9;

    @Test
    void elapsed_countsFromTheStartAfterTheWait() {
        final Motion motion = new Motion(spec(MotionStyles.SCALE).after(100), 1000.0);
        assertEquals(0.0, motion.elapsed(1050.0), CLOSE);
        assertEquals(0.5, motion.elapsed(1200.0), CLOSE);
        assertEquals(1.0, motion.elapsed(5000.0), CLOSE);
    }

    @Test
    void done_onceItsTimeHasPassed() {
        final Motion motion = new Motion(spec(MotionStyles.SCALE), 0.0);
        assertFalse(motion.done(199.0));
        assertTrue(motion.done(200.0));
    }

    @Test
    void finished_isOverAtAnyTime() {
        assertTrue(Motion.FINISHED.done(0.0));
        assertEquals(1.0, Motion.FINISHED.progress(-5.0), CLOSE);
    }

    @Test
    void scale_growsFromItsStartToItsEnd() {
        final Motion motion = new Motion(spec(MotionStyles.SCALE).with("from", 0.8).with("to", 1.0), 0.0);
        assertEquals(0.8, motion.scale(0.0), CLOSE);
        assertEquals(0.9, motion.scale(100.0), CLOSE);
        assertEquals(1.0, motion.scale(200.0), CLOSE);
    }

    @Test
    void scaleY_followsTheAcrossSizeUnlessItGrowsUnevenly() {
        final Motion even = new Motion(spec(MotionStyles.SCALE).with("from", 0.5), 0.0);
        assertEquals(0.5, even.scaleY(0.0), CLOSE);
        final Motion uneven = new Motion(spec(MotionStyles.SCALE).with("from", 0.01).with("from_y", 0.05), 0.0);
        assertEquals(0.05, uneven.scaleY(0.0), CLOSE);
        assertEquals(1.0, uneven.scaleY(200.0), CLOSE);
    }

    @Test
    void offset_slidesInFromItsDistanceOrOutToIt() {
        final Motion in = new Motion(spec(MotionStyles.SLIDE).with("distance", 1.0), 0.0);
        assertEquals(1.0, in.offset(0.0), CLOSE);
        assertEquals(0.0, in.offset(200.0), CLOSE);
        final Motion out = new Motion(spec(MotionStyles.SLIDE).with("distance", 1.0).with("out", 1), 0.0);
        assertEquals(0.0, out.offset(0.0), CLOSE);
        assertEquals(1.0, out.offset(200.0), CLOSE);
    }

    @Test
    void toward_goesToThePlaceThatStandsForItOrComesBack() {
        final Motion going = new Motion(spec(MotionStyles.ZOOM).with("out", 1), 0.0);
        assertTrue(going.outward());
        assertEquals(0.0, going.toward(0.0), CLOSE);
        assertEquals(1.0, going.toward(200.0), CLOSE);
        final Motion coming = new Motion(spec(MotionStyles.ZOOM), 0.0);
        assertFalse(coming.outward());
        assertEquals(1.0, coming.toward(0.0), CLOSE);
        assertEquals(0.0, coming.toward(200.0), CLOSE);
    }

    @Test
    void is_tellsTheStyle() {
        assertTrue(new Motion(spec(MotionStyles.OUTLINE), 0.0).is(MotionStyles.OUTLINE));
        assertFalse(new Motion(spec(MotionStyles.OUTLINE), 0.0).is(MotionStyles.ZOOM));
    }

    private static MotionSpec spec(final String style) {
        return MotionSpec.of(style, 200, IEasing.LINEAR, "");
    }
}
