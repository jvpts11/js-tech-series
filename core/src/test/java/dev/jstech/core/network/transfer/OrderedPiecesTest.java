/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class OrderedPiecesTest {

    @Test
    void accept_takesThePiecesInOrder() throws IOException {
        final OrderedPieces pieces = new OrderedPieces(3, 100);
        pieces.accept(0, 40);
        pieces.accept(1, 40);
        assertFalse(pieces.complete());
        pieces.accept(2, 20);

        assertTrue(pieces.complete());
        assertEquals(100, pieces.received());
    }

    @Test
    void accept_refusesOneOutOfPlaceOrOneTooMany() throws IOException {
        final OrderedPieces pieces = new OrderedPieces(2, 100);
        assertThrows(IOException.class, () -> pieces.accept(1, 10), "piece 1 before piece 0");
        pieces.accept(0, 10);
        pieces.accept(1, 10);
        assertThrows(IOException.class, () -> pieces.accept(2, 10), "a third of two");
    }

    @Test
    void accept_refusesMoreBytesThanTheRunHolds() {
        final OrderedPieces pieces = new OrderedPieces(2, 50);
        assertThrows(IOException.class, () -> pieces.accept(0, 51));
    }

    @Test
    void constructor_refusesARunOfNoPieces() {
        assertThrows(IllegalArgumentException.class, () -> new OrderedPieces(0, 10));
    }
}
