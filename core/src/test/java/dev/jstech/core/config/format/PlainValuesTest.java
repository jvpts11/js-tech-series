/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlainValuesTest {

    @Test
    void value_widensSmallWholesAndFloats() {
        assertEquals(7, PlainValues.value((byte) 7));
        assertEquals(7, PlainValues.value((short) 7));
        assertEquals(0.5, PlainValues.value(0.5f));
    }

    @Test
    void value_copiesMapsAndCollectionsAndDropsNothingness() {
        assertEquals(Map.of("1", List.of("a")), PlainValues.value(Map.of(1, Set.of("a"))));
        assertNull(PlainValues.value(null));
    }

    @Test
    void shapedLike_aNumberWhereABooleanBelongs_isThatBoolean() {
        assertEquals(true, PlainValues.shapedLike((byte) 1, false));
        assertEquals(false, PlainValues.shapedLike(0, true));
    }

    @Test
    void shapedLike_reachesIntoMapsAndLists() {
        assertEquals(Map.of("on", true, "count", 3),
                PlainValues.shapedLike(Map.of("on", 1, "count", 3), Map.of("on", false, "count", 0)));
        assertEquals(List.of(true, false), PlainValues.shapedLike(List.of(1, 0), List.of(false)));
    }

    @Test
    void shapedLike_leavesWhatTheShapeDoesNotKnow() {
        assertEquals("text", PlainValues.shapedLike("text", false));
        assertEquals(List.of(1), PlainValues.shapedLike(List.of(1), List.of()));
    }
}
