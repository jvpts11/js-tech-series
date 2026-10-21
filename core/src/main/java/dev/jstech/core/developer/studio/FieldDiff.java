/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A block's fields before it was saved and after it was read back, field by field: what came back the same, what came
 * back different, and what was lost or appeared, in the order the fields were first written.
 */
public final class FieldDiff {

    private FieldDiff() {
    }

    /** Every field of either, the ones of {@code before} first in their order, each with both values. */
    public static List<Change> of(final Map<String, String> before, final Map<String, String> after) {
        final Set<String> names = new LinkedHashSet<>(before.keySet());
        names.addAll(after.keySet());
        final List<Change> out = new ArrayList<>(names.size());
        for (final String name : names) {
            final String was = before.getOrDefault(name, "");
            final String is = after.getOrDefault(name, "");
            out.add(new Change(name, was, is, !Objects.equals(before.get(name), after.get(name))));
        }
        return out;
    }

    /** How many of {@code changes} came back different. */
    public static long changed(final List<Change> changes) {
        return changes.stream().filter(Change::changed).count();
    }

    /**
     * One field.
     *
     * @param field   its name, its path in what was saved
     * @param before  its value before, empty when it was not there
     * @param after   its value read back, empty when it was lost
     * @param changed whether the two differ
     */
    public record Change(String field, String before, String after, boolean changed) {
    }
}
