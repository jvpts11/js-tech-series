/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PictureRowsTest {

    @Test
    void at_isThePictureShownOnTheRowOnceTheListHasMovedOn() {
        assertEquals(1, PictureRows.first(1, 4), "the second chosen, the list shows it first");
        assertEquals(1, PictureRows.at(1, 4, 0), "its top row is the second picture, not the first");
        assertEquals(2, PictureRows.at(1, 4, 1));
        assertEquals(3, PictureRows.at(3, 4, 1), "the last two shown, the last one under the other");
    }

    @Test
    void at_isNoneWherePictureOrRowRunsOut() {
        assertEquals(-1, PictureRows.at(0, 1, 1), "one picture fills one row");
        assertEquals(-1, PictureRows.at(0, 4, 2), "two rows are shown");
        assertEquals(-1, PictureRows.at(0, 4, -1));
        assertEquals(0, PictureRows.at(0, 1, 0));
    }
}
