/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallaxWindowTest {

    /* An advancement tab's window across a sky of 400 by 200. */
    private static final int SKY = 400;
    private static final int WINDOW = 234;
    private static final double START = 5;

    @Test
    void offset_showsThePicturesMiddleWhereTheContentStarted() {
        assertEquals(83, ParallaxWindow.offset(START, START, SKY, WINDOW));
    }

    @Test
    void offset_slidesAgainstTheDragAtHalfItsSpeed() {
        assertEquals(73, ParallaxWindow.offset(START + 20, START, SKY, WINDOW));
        assertEquals(93, ParallaxWindow.offset(START - 20, START, SKY, WINDOW));
    }

    @Test
    void offset_neverLeavesThePicture() {
        assertEquals(0, ParallaxWindow.offset(START + 10_000, START, SKY, WINDOW));
        assertEquals(SKY - WINDOW, ParallaxWindow.offset(START - 10_000, START, SKY, WINDOW));
    }

    @Test
    void offset_isZeroWhenThePictureIsNoLongerThanTheWindow() {
        assertEquals(0, ParallaxWindow.offset(START + 30, START, WINDOW, WINDOW));
        assertEquals(0, ParallaxWindow.offset(START - 30, START, 200, WINDOW));
    }
}
