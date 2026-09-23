/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SourceAdvantageTest {

    @Test
    void of_takesTheShareOffToTheNearestWhole() {
        assertEquals(90, SourceAdvantage.of(100));
        assertEquals(14, SourceAdvantage.of(16));
        assertEquals(230, SourceAdvantage.of(256));
    }

    @Test
    void of_leavesAFigureTooSmallToLoseAWholeOneAsItWas() {
        assertEquals(4, SourceAdvantage.of(4));
        assertEquals(1, SourceAdvantage.of(1));
    }

    @Test
    void of_leavesNothingAtNothing() {
        assertEquals(0, SourceAdvantage.of(0));
        assertEquals(0L, SourceAdvantage.of(0L));
    }

    @Test
    void of_leavesAPackageBuiltElsewhereAsItAsks() {
        assertEquals(256, SourceAdvantage.of(256, false));
        assertEquals(230, SourceAdvantage.of(256, true));
        assertEquals(4_096L, SourceAdvantage.of(4_096L, false));
        assertEquals(3_686L, SourceAdvantage.of(4_096L, true));
    }
}
