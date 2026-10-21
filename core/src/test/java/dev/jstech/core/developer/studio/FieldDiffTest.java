/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FieldDiffTest {

    @Test
    void of_marksOnlyTheFieldsThatCameBackDifferent() {
        final List<FieldDiff.Change> changes = FieldDiff.of(fields("Status", "working", "Progress", "0.62"),
                fields("Status", "working", "Progress", "0.0"));
        assertEquals(List.of(new FieldDiff.Change("Status", "working", "working", false),
                new FieldDiff.Change("Progress", "0.62", "0.0", true)), changes);
        assertEquals(1, FieldDiff.changed(changes));
    }

    @Test
    void of_listsAFieldLostAndOneThatAppearedAsChanged() {
        final List<FieldDiff.Change> changes = FieldDiff.of(fields("Sides", "back"), fields("Owner", "jvpts11"));
        assertEquals(List.of(new FieldDiff.Change("Sides", "back", "", true),
                new FieldDiff.Change("Owner", "", "jvpts11", true)), changes);
    }

    @Test
    void of_keepsTheOrderTheFieldsWereFirstWritten() {
        final List<FieldDiff.Change> changes = FieldDiff.of(fields("b", "1", "a", "2"), fields("a", "2", "b", "1"));
        assertEquals("b", changes.get(0).field());
        assertEquals("a", changes.get(1).field());
    }

    private static Map<String, String> fields(final String... pairs) {
        final Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            out.put(pairs[i], pairs[i + 1]);
        }
        return out;
    }
}
