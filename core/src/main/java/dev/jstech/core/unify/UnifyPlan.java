/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Which item stands for which: for each tag of a material's form held by more than one item, the item of the first
 * preferred mod that has one in it, or the first by id when none does, stands for the tag's other items. The tags are
 * taken in the order of their ids, so the same tags always make the same plan; an item that already stands for a tag
 * is never replaced by a later one, and an item kept is never replaced at all.
 *
 * @param replace each item replaced, by id, and the id of the item that stands for it
 * @param chosen  each tag unified, by id, and the id of the item that stands for it
 */
public record UnifyPlan(Map<String, String> replace, Map<String, String> chosen) {

    /** A plan that replaces nothing. */
    public static final UnifyPlan NONE = new UnifyPlan(Map.of(), Map.of());

    public UnifyPlan {
        replace = Map.copyOf(replace);
        chosen = Map.copyOf(chosen);
    }

    /**
     * The plan for {@code tags}, each tag's id with the ids of the items in it.
     *
     * @param prefer the mods whose items stand first, by namespace, first to last
     * @param keep   the items never replaced, by id
     */
    public static UnifyPlan of(final Map<String, ? extends Collection<String>> tags, final List<String> prefer,
                               final Set<String> keep) {
        final Map<String, String> replace = new HashMap<>();
        final Map<String, String> chosen = new TreeMap<>();
        final Set<String> standing = new HashSet<>();
        for (final Map.Entry<String, ? extends Collection<String>> tag : new TreeMap<>(tags).entrySet()) {
            final TreeSet<String> items = new TreeSet<>(tag.getValue());
            if (items.size() < 2) {
                continue;
            }
            final String first = choose(items, prefer);
            final String stands = replace.getOrDefault(first, first);
            chosen.put(tag.getKey(), stands);
            standing.add(stands);
            for (final String item : items) {
                if (!item.equals(stands) && !keep.contains(item) && !standing.contains(item)
                        && !replace.containsKey(item)) {
                    replace.put(item, stands);
                }
            }
        }
        return new UnifyPlan(replace, chosen);
    }

    /** Whether it replaces nothing. */
    public boolean isEmpty() {
        return this.replace.isEmpty();
    }

    /* The item of the first preferred mod among {@code items}, sorted by id, or the first of them. */
    private static String choose(final TreeSet<String> items, final List<String> prefer) {
        for (final String mod : prefer) {
            final String start = mod + ":";
            for (final String item : items) {
                if (item.startsWith(start)) {
                    return item;
                }
            }
        }
        return items.first();
    }
}
