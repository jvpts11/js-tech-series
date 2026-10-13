/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.StereoSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpeakerSidesTest {

    /*
     * A monitor whose screen faces north: whoever sits at it looks south, and their left is east, one step of x.
     */
    private static final int LEFT_X = 1;
    private static final int LEFT_Z = 0;

    @Test
    void sideOf_speakerAlonePlaysBothSides() {
        assertEquals(StereoSide.BOTH, SpeakerSides.sideOf(1, LEFT_X, LEFT_Z, 1, 0));
    }

    @Test
    void sideOf_speakerOnTheViewersLeftPlaysTheLeft() {
        assertEquals(StereoSide.LEFT, SpeakerSides.sideOf(2, LEFT_X, LEFT_Z, 1, 0));
    }

    @Test
    void sideOf_speakerOnTheViewersRightPlaysTheRight() {
        assertEquals(StereoSide.RIGHT, SpeakerSides.sideOf(2, LEFT_X, LEFT_Z, -2, 1));
    }

    @Test
    void sideOf_speakerSquareBehindTheMonitorPlaysBothSides() {
        assertEquals(StereoSide.BOTH, SpeakerSides.sideOf(2, LEFT_X, LEFT_Z, 0, 3));
    }

    @Test
    void sideOf_turnedMonitorTurnsTheSides() {
        // Screen facing east: whoever sits at it looks west, and their left is south, one step of z.
        assertEquals(StereoSide.LEFT, SpeakerSides.sideOf(2, 0, 1, 0, 2));
        assertEquals(StereoSide.RIGHT, SpeakerSides.sideOf(2, 0, 1, 0, -2));
    }
}
