/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.monitor.TouchGesture.Event;
import dev.jstech.computers.monitor.TouchGesture.Kind;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TouchGestureTest {

    private TouchGesture finger;

    @BeforeEach
    void setUp() {
        finger = new TouchGesture();
    }

    @Test
    void release_afterAShortStillTouchIsATapWhereItWentDown() {
        finger.press(40, 30);
        finger.held(41, 30);
        final List<Event> events = finger.release();
        assertEquals(List.of(new Event(Kind.PRESS, 40, 30, TouchGesture.LEFT),
                new Event(Kind.RELEASE, 40, 30, TouchGesture.LEFT)), events);
        assertFalse(finger.down());
    }

    @Test
    void held_stillForASecondIsTheRightButtonOnce() {
        finger.press(10, 10);
        for (int tick = 1; tick < TouchGesture.HOLD_TICKS; tick++) {
            assertTrue(finger.held(10, 10).isEmpty(), "nothing before the second is up, tick " + tick);
        }
        assertEquals(List.of(new Event(Kind.PRESS, 10, 10, TouchGesture.RIGHT),
                new Event(Kind.RELEASE, 10, 10, TouchGesture.RIGHT)), finger.held(10, 10));
        assertTrue(finger.held(10, 10).isEmpty(), "the right button is pressed once");
        assertTrue(finger.release().isEmpty(), "lifting after a hold does nothing more");
    }

    @Test
    void held_wanderingPastTheSlopStartsADragFromWhereItWentDown() {
        finger.press(10, 10);
        assertEquals(List.of(new Event(Kind.PRESS, 10, 10, TouchGesture.LEFT),
                new Event(Kind.DRAG, 20, 10, TouchGesture.LEFT)), finger.held(20, 10));
        assertEquals(List.of(new Event(Kind.DRAG, 25, 12, TouchGesture.LEFT)), finger.held(25, 12));
        assertTrue(finger.held(25, 12).isEmpty(), "a finger standing still in a drag moves nothing");
        assertEquals(List.of(new Event(Kind.RELEASE, 25, 12, TouchGesture.LEFT)), finger.release());
    }

    @Test
    void held_withinTheSlopStaysATap() {
        finger.press(10, 10);
        assertTrue(finger.held(10 + TouchGesture.SLOP, 10).isEmpty());
        assertEquals(Kind.PRESS, finger.release().getFirst().kind());
    }

    @Test
    void held_aDragNeverBecomesAHold() {
        finger.press(0, 0);
        finger.held(50, 0);
        for (int tick = 0; tick < TouchGesture.HOLD_TICKS * 2; tick++) {
            assertTrue(finger.held(50, 0).isEmpty());
        }
        assertEquals(List.of(new Event(Kind.RELEASE, 50, 0, TouchGesture.LEFT)), finger.release());
    }

    @Test
    void cancel_dropsTheFingerWithoutAnEvent() {
        finger.press(5, 5);
        finger.cancel();
        assertFalse(finger.down());
        assertTrue(finger.release().isEmpty());
    }

    @Test
    void release_withNoFingerDownDoesNothing() {
        assertTrue(finger.release().isEmpty());
        assertTrue(finger.held(1, 1).isEmpty());
    }
}
