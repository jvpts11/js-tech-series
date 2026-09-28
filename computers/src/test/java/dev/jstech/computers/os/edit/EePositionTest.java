/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EePositionTest {

    @Test
    void of_columnZero_readsAsZero() {
        assertEquals(0, EePosition.of(0, 0, 0).column());
    }

    @Test
    void of_columnOne_readsAsOne() {
        assertEquals(1, EePosition.of(0, 1, 0).column());
    }

    @Test
    void of_theEndOfALine_readsAsTheLinesLength() {
        assertEquals(28, EePosition.of(3, 28, 0).column());
    }

    @Test
    void of_lineAndFromTop_areCountedFromOne() {
        final EePosition at = EePosition.of(5, 10, 2);
        assertEquals(6, at.line(), "the sixth line, counting the first as one");
        assertEquals(4, at.fromTop(), "the fourth line the view shows, counting the first as one");
    }
}
