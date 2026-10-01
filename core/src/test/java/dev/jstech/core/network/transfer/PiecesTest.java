/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PiecesTest {

    @Test
    void count_isOneAtLeastAndRoundsUp() {
        assertEquals(1, Pieces.count(0, 32));
        assertEquals(1, Pieces.count(32, 32));
        assertEquals(2, Pieces.count(33, 32));
        assertEquals(4, Pieces.count(100, 32));
    }

    @Test
    void length_isAPieceOrWhatIsLeft() {
        assertEquals(32, Pieces.length(0, 100, 32));
        assertEquals(4, Pieces.length(3, 100, 32));
        assertEquals(0, Pieces.length(4, 100, 32));
        assertEquals(64L, Pieces.start(2, 32));
    }

    @Test
    void count_refusesAPieceOfNothing() {
        assertThrows(IllegalArgumentException.class, () -> Pieces.count(10, 0));
    }
}
