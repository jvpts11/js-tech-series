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

class RhythmTest {

    private static final double CLOSE = 1e-9;

    @Test
    void on_aVgaCursorTurnsOverEverySixteenFramesAtSeventyHertz() {
        final MotionSpec vga = spec(MotionStyles.BLINK, 229);
        assertTrue(Rhythm.on(vga, 0.0));
        assertTrue(Rhythm.on(vga, 228.0));
        assertFalse(Rhythm.on(vga, 229.0));
        assertFalse(Rhythm.on(vga, 457.0));
        assertTrue(Rhythm.on(vga, 458.0));
    }

    @Test
    void on_keepsEachConsoleToItsOwnBeat() {
        // fbcon turns over every 200 ms, dtterm every 250, the Windows console every 530.
        assertFalse(Rhythm.on(spec(MotionStyles.BLINK, 200), 210.0));
        assertTrue(Rhythm.on(spec(MotionStyles.BLINK, 250), 210.0));
        assertTrue(Rhythm.on(spec(MotionStyles.BLINK, 530), 520.0));
        assertFalse(Rhythm.on(spec(MotionStyles.BLINK, 530), 540.0));
    }

    @Test
    void on_aBlinkThatDoesNotMoveIsAlwaysOn() {
        assertTrue(Rhythm.on(MotionSpec.NONE, 300.0));
        assertTrue(Rhythm.on(spec(MotionStyles.BLINK, 250).slowed(0), 300.0));
    }

    @Test
    void frame_goesThroughEveryPictureOnceAPass() {
        final MotionSpec throbber = spec(MotionStyles.LOOP, 800).with("frames", 8);
        assertEquals(0, Rhythm.frame(throbber, 0.0));
        assertEquals(0, Rhythm.frame(throbber, 99.0));
        assertEquals(1, Rhythm.frame(throbber, 100.0));
        assertEquals(7, Rhythm.frame(throbber, 799.0));
        assertEquals(0, Rhythm.frame(throbber, 800.0));
    }

    @Test
    void frame_ofALoopThatDoesNotMoveIsTheFirst() {
        assertEquals(0, Rhythm.frame(spec(MotionStyles.LOOP, 800).with("frames", 8).slowed(0), 450.0));
    }

    @Test
    void pass_startsAgainAtTheEndOfEachPass() {
        final MotionSpec bar = spec(MotionStyles.SEGMENT, 1000);
        assertEquals(0.25, Rhythm.pass(bar, 250.0), CLOSE);
        assertEquals(0.25, Rhythm.pass(bar, 1250.0), CLOSE);
        assertEquals(0.0, Rhythm.pass(bar, 2000.0), CLOSE);
    }

    @Test
    void bounce_goesThereAndComesBack() {
        final MotionSpec block = spec(MotionStyles.BOUNCE, 1000);
        assertEquals(0.0, Rhythm.bounce(block, 0.0), CLOSE);
        assertEquals(0.5, Rhythm.bounce(block, 500.0), CLOSE);
        assertEquals(1.0, Rhythm.bounce(block, 1000.0), CLOSE);
        assertEquals(0.5, Rhythm.bounce(block, 1500.0), CLOSE);
        assertEquals(0.0, Rhythm.bounce(block, 2000.0), CLOSE);
    }

    private static MotionSpec spec(final String style, final int ms) {
        return MotionSpec.of(style, ms, IEasing.LINEAR, "");
    }
}
