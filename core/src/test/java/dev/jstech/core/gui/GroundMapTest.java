/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Text asks the grounds painted on a frame what it is written on, and gets the one painted there last. */
class GroundMapTest {

    private static final int NONE = 0x12345678;
    private static final int WINDOW = 0xFF0B0E13;
    private static final int PANEL = 0xFF11161D;
    private static final int BUTTON = 0xFF1A2937;

    private GroundMap grounds;

    @BeforeEach
    void setUp() {
        grounds = new GroundMap();
    }

    @Test
    void at_nothingDeclared_isTheFallback() {
        assertEquals(NONE, grounds.at(5, 5, NONE));
    }

    @Test
    void at_insideAGround_isItsColour_andOutsideIsTheFallback() {
        grounds.declare(10, 10, 20, 20, PANEL);
        assertEquals(PANEL, grounds.at(10, 10, NONE));
        assertEquals(PANEL, grounds.at(19.5f, 19.5f, NONE));
        assertEquals(NONE, grounds.at(20, 15, NONE));
        assertEquals(NONE, grounds.at(9.9f, 15, NONE));
    }

    @Test
    void at_nestedGrounds_isTheOnePaintedLast() {
        grounds.declare(0, 0, 100, 100, WINDOW);
        grounds.declare(10, 10, 60, 60, PANEL);
        grounds.declare(20, 20, 40, 30, BUTTON);
        assertEquals(BUTTON, grounds.at(25, 25, NONE));
        assertEquals(PANEL, grounds.at(15, 15, NONE));
        assertEquals(WINDOW, grounds.at(80, 80, NONE));
    }

    @Test
    void declare_cornersInEitherOrder_coverTheSameRectangle() {
        grounds.declare(20, 20, 10, 10, PANEL);
        assertEquals(PANEL, grounds.at(15, 15, NONE));
    }

    @Test
    void declare_emptyOrClear_isNoGround() {
        grounds.declare(10, 10, 10, 20, PANEL);
        grounds.declare(10, 10, 20, 10, PANEL);
        grounds.declare(10, 10, 20, 20, 0x00FFFFFF);
        assertEquals(0, grounds.size());
        assertEquals(NONE, grounds.at(15, 15, NONE));
    }

    /** A veil half way to white over a black window is seen as the grey between them. */
    @Test
    void declare_seeThrough_isSeenOverWhatLiesUnderItsMiddle() {
        grounds.declare(0, 0, 100, 100, 0xFF000000);
        grounds.declare(0, 0, 100, 100, 0x80FFFFFF);
        assertEquals(0xFF808080, grounds.at(50, 50, NONE));
    }

    /** A clear colour can stand for "nothing here", since no ground it keeps is ever see-through. */
    @Test
    void at_everyGroundKept_isOpaque() {
        grounds.declare(0, 0, 10, 10, 0x40112233);
        grounds.declare(20, 20, 30, 30, PANEL);
        assertEquals(0xFF, grounds.at(5, 5, 0) >>> 24);
        assertEquals(0xFF, grounds.at(25, 25, 0) >>> 24);
        assertEquals(0, grounds.at(15, 15, 0));
    }

    @Test
    void declare_seeThroughOverNothing_isTakenAsItsOwnColour() {
        grounds.declare(0, 0, 10, 10, 0x80336699);
        assertEquals(0xFF336699, grounds.at(5, 5, NONE));
    }

    @Test
    void declare_pastCapacity_forgetsTheOldestAndKeepsTheNewest() {
        grounds.declare(0, 0, 10, 10, WINDOW);
        for (int i = 0; i < GroundMap.CAPACITY; i++) {
            grounds.declare(100 + i, 0, 101 + i, 1, PANEL);
        }
        assertEquals(GroundMap.CAPACITY, grounds.size());
        assertEquals(NONE, grounds.at(5, 5, NONE));
        assertEquals(PANEL, grounds.at(100.5f + GroundMap.CAPACITY - 1, 0.5f, NONE));
    }

    @Test
    void clear_forgetsEveryGround() {
        grounds.declare(0, 0, 10, 10, PANEL);
        grounds.clear();
        assertEquals(0, grounds.size());
        assertEquals(NONE, grounds.at(5, 5, NONE));
    }
}
