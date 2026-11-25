/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TabScrollTest {

    /* The Injection Moulder's seven lists in Portuguese, as the game's font measures them with the least padding. */
    private static final int[] PORTUGUESE = {50, 29, 62, 67, 49, 34, 52};
    /* The strip's room with the two arrows taken out. */
    private static final int ROOM = 283 - 18;

    @Test
    void shown_countsTheTabsThatFitFromTheFirst() {
        assertEquals(5, TabScroll.shown(PORTUGUESE, ROOM, 0));
        assertEquals(5, TabScroll.shown(PORTUGUESE, ROOM, 2));
    }

    @Test
    void shown_isOneAtLeastWhenATabIsWiderThanTheRoom() {
        assertEquals(1, TabScroll.shown(new int[] {300, 10}, ROOM, 0));
    }

    @Test
    void lastFirst_isTheTabFromWhichEveryOneLeftFits() {
        assertEquals(2, TabScroll.lastFirst(PORTUGUESE, ROOM));
    }

    @Test
    void lastFirst_isTheFirstTabWhenTheyAllFit() {
        assertEquals(0, TabScroll.lastFirst(new int[] {40, 40, 40}, ROOM));
    }

    @Test
    void showing_keepsTheRowWhenTheTabIsShownAlready() {
        assertEquals(1, TabScroll.showing(PORTUGUESE, ROOM, 1, 3));
    }

    @Test
    void showing_movesLeftToATabBeforeTheRow() {
        assertEquals(0, TabScroll.showing(PORTUGUESE, ROOM, 2, 0));
    }

    @Test
    void showing_movesRightOnlyAsFarAsItTakes() {
        assertEquals(1, TabScroll.showing(PORTUGUESE, ROOM, 0, 5));
        assertEquals(2, TabScroll.showing(PORTUGUESE, ROOM, 0, 6));
    }

    @Test
    void showing_neverStartsPastTheLastFirst() {
        assertEquals(2, TabScroll.showing(PORTUGUESE, ROOM, 6, 6));
    }

    @Test
    void showing_isTheFirstTabOfAnEmptyRow() {
        assertEquals(0, TabScroll.showing(new int[0], ROOM, 3, 0));
    }
}
