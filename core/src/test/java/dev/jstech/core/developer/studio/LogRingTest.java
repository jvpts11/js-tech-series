/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class LogRingTest {

    @Test
    void lines_keepsOnlyTheLastOfEachLog() {
        final LogRing ring = new LogRing(2);
        ring.add("a", new LogRing.Line(1, "info", "one"));
        ring.add("a", new LogRing.Line(2, "info", "two"));
        ring.add("a", new LogRing.Line(3, "warn", "three"));
        ring.add("b", new LogRing.Line(4, "info", "other"));
        assertEquals(List.of("two", "three"), ring.lines("a").stream().map(LogRing.Line::message).toList());
        assertEquals(1, ring.lines("b").size());
    }

    @Test
    void lines_isEmptyForALogThatWroteNothing() {
        assertEquals(List.of(), new LogRing(4).lines("none"));
    }

    @Test
    void new_refusesARingOfNoLines() {
        assertThrows(IllegalArgumentException.class, () -> new LogRing(0));
    }
}
