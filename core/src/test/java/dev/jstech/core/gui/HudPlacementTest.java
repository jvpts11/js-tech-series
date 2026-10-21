/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudPlacementTest {

    private static final int WIDE = 480;
    private static final int TALL = 270;
    private static final HudStack.Size SIZE = new HudStack.Size(100, 40);

    @Test
    void at_holdsAnElementFromTheCornerNearestItsMiddle() {
        assertEquals(HudStack.Corner.TOP_LEFT, HudPlacement.at(10, 10, SIZE, WIDE, TALL, 1, false).corner());
        assertEquals(HudStack.Corner.TOP_RIGHT, HudPlacement.at(370, 10, SIZE, WIDE, TALL, 1, false).corner());
        assertEquals(HudStack.Corner.BOTTOM_LEFT, HudPlacement.at(10, 220, SIZE, WIDE, TALL, 1, false).corner());
        assertEquals(HudStack.Corner.BOTTOM_RIGHT, HudPlacement.at(370, 220, SIZE, WIDE, TALL, 1, false).corner());
    }

    @Test
    void topLeft_givesBackWhereTheElementWasLeft() {
        final HudPlacement placed = HudPlacement.at(360, 200, SIZE, WIDE, TALL, 1, false);
        assertEquals(new HudStack.Point(360, 200), placed.topLeft(WIDE, TALL, SIZE));
        assertEquals(20, placed.x(), "twenty pixels in from the right side");
        assertEquals(30, placed.y(), "thirty up from the bottom");
    }

    @Test
    void topLeft_staysByItsCornerWhenTheScreenGrows() {
        final HudPlacement placed = HudPlacement.at(360, 200, SIZE, WIDE, TALL, 1, false);
        assertEquals(new HudStack.Point(520, 290), placed.topLeft(640, 360, SIZE));
    }

    @Test
    void topLeft_keepsTheElementOnASmallerScreen() {
        final HudPlacement placed = new HudPlacement(HudStack.Corner.TOP_LEFT, 450, 260, 1, false);
        final HudStack.Point point = placed.topLeft(WIDE, TALL, SIZE);
        assertTrue(point.x() + SIZE.width() <= WIDE && point.y() + SIZE.height() <= TALL, "on the screen: " + point);
    }

    @Test
    void at_keepsAnElementDraggedOffTheScreenOnIt() {
        final HudPlacement placed = HudPlacement.at(-30, 900, SIZE, WIDE, TALL, 1, false);
        assertEquals(new HudStack.Point(0, TALL - SIZE.height()), placed.topLeft(WIDE, TALL, SIZE));
    }

    @Test
    void scale_isKeptWithinTheSizesAnElementMayHave() {
        assertEquals(HudPlacement.LEAST_SCALE, new HudPlacement(HudStack.Corner.TOP_LEFT, 0, 0, 0.1F, false).scale());
        assertEquals(HudPlacement.MOST_SCALE, new HudPlacement(HudStack.Corner.TOP_LEFT, 0, 0, 9, false).scale());
        assertEquals(new HudStack.Size(150, 60),
                new HudPlacement(HudStack.Corner.TOP_LEFT, 0, 0, 1.5F, false).scaled(SIZE));
    }
}
