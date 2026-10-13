/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AlertSignBoardTest {

    @Test
    void showing_keepsASignForItsLifetimeOnly() {
        final AlertSignBoard board = new AlertSignBoard();
        board.raise("Rack alarm sounds", 1, 2, 3, true, 100);
        assertEquals(1, board.showing(100 + AlertSignBoard.LIFETIME_TICKS - 1).size());
        assertTrue(board.showing(100 + AlertSignBoard.LIFETIME_TICKS).isEmpty());
    }

    @Test
    void raise_putsTheNewestFirstAndKeepsAFew() {
        final AlertSignBoard board = new AlertSignBoard();
        for (int i = 0; i < 5; i++) {
            board.raise("alarm " + i, 0, 0, 0, false, i);
        }
        final List<AlertSignBoard.Sign> up = board.showing(5);
        assertEquals(AlertSignBoard.MOST, up.size());
        assertEquals("alarm 4", up.getFirst().label());
        assertEquals("alarm 2", up.getLast().label());
    }

    @Test
    void raise_bringsTheSameAlertBackToTheTopOnce() {
        final AlertSignBoard board = new AlertSignBoard();
        board.raise("Rack alarm sounds", 0, 0, 0, true, 0);
        board.raise("Reactor alarm sounds", 0, 0, 0, true, 10);
        board.raise("Rack alarm sounds", 0, 0, 0, true, 20);
        final List<AlertSignBoard.Sign> up = board.showing(20);
        assertEquals(2, up.size());
        assertEquals("Rack alarm sounds", up.getFirst().label());
        assertEquals(20, up.getFirst().raised());
    }

    @Test
    void lit_blinksThreeTimesThenStaysOn() {
        final AlertSignBoard.Sign sign = new AlertSignBoard.Sign("x", 0, 0, 0, true, 0);
        int offs = 0;
        boolean was = true;
        for (long t = 0; t < AlertSignBoard.LIFETIME_TICKS; t++) {
            final boolean now = AlertSignBoard.lit(sign, t);
            if (was && !now) {
                offs++;
            }
            was = now;
        }
        assertEquals(AlertSignBoard.BLINKS, offs);
        assertTrue(AlertSignBoard.lit(sign, 0));
        assertFalse(AlertSignBoard.lit(sign, AlertSignBoard.BLINK_TICKS));
        assertTrue(AlertSignBoard.lit(sign, AlertSignBoard.LIFETIME_TICKS - 1));
    }

    @Test
    void direction_pointsNowhereAheadAndToTheSideOtherwise() {
        assertEquals(0, AlertSignBoard.direction(0.9, 0.3));
        assertEquals(1, AlertSignBoard.direction(0.1, 0.8));
        assertEquals(-1, AlertSignBoard.direction(-0.7, -0.2));
    }
}
