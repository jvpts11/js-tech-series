/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigTreeTest {

    @Test
    void put_makesTheSectionsOnTheWay() {
        final Map<String, Object> values = new LinkedHashMap<>();

        ConfigTree.put(values, List.of("boot", "menu", "wait"), 3);

        assertEquals(Map.of("boot", Map.of("menu", Map.of("wait", 3))), values);
        assertEquals(3, ConfigTree.get(values, List.of("boot", "menu", "wait")));
    }

    @Test
    void put_replacesAValueStandingWhereASectionGoes() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("boot", 7);

        ConfigTree.put(values, List.of("boot", "wait"), 3);

        assertEquals(Map.of("boot", Map.of("wait", 3)), values);
    }

    @Test
    void get_aPathThroughAValue_isNothing() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("boot", 7);

        assertNull(ConfigTree.get(values, List.of("boot", "wait")));
        assertNull(ConfigTree.get(values, List.of("missing")));
    }

    @Test
    void remove_givesBackWhatItTookOut() {
        final Map<String, Object> values = new LinkedHashMap<>();
        ConfigTree.put(values, List.of("boot", "wait"), 3);

        assertEquals(3, ConfigTree.remove(values, List.of("boot", "wait")));
        assertNull(ConfigTree.remove(values, List.of("boot", "wait")));
        assertNull(ConfigTree.remove(values, List.of("nowhere", "at_all")));
    }

    @Test
    void path_splitsOnTheDots() {
        assertEquals(List.of("a", "b", "c"), ConfigTree.path("a.b.c"));
    }
}
