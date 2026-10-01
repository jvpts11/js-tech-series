/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigKeyRangeTest {

    @Test
    void clamp_belowTheLowerEnd_isTheLowerEnd() {
        assertEquals(10L, new ConfigKeyRange<>(10L, 100L).clamp(5L));
    }

    @Test
    void clamp_aboveTheUpperEnd_isTheUpperEnd() {
        assertEquals(100L, new ConfigKeyRange<>(10L, 100L).clamp(150L));
    }

    @Test
    void clamp_inside_isTheValue() {
        assertEquals(50L, new ConfigKeyRange<>(10L, 100L).clamp(50L));
    }

    @Test
    void contains_includesBothEnds() {
        final ConfigKeyRange<Long> range = new ConfigKeyRange<>(10L, 100L);

        assertTrue(range.contains(10L));
        assertTrue(range.contains(100L));
        assertFalse(range.contains(101L));
    }

    @Test
    void constructor_refusesALowerEndAboveTheUpper() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigKeyRange<>(100L, 10L));
    }
}
