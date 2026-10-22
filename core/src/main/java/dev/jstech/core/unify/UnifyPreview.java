/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What unification does to a world's recipes, or would do turned on: for each tag unified, the item kept and those it
 * stands for, how many recipes name one of them, and one of those recipes with the item it names before and after.
 * With no game types, so a unit test reads it from a plan and the recipes' changes.
 *
 * @param on          whether unification is on, so this is what it did as the data loaded, not what it would do
 * @param steppedAside whether it steps aside for another mod that unifies
 * @param prefer      the mods whose items are kept first, in order
 * @param drops       whether what drops is unified too
 * @param tags        each tag unified, those that change the most recipes first
 */
public record UnifyPreview(boolean on, boolean steppedAside, List<String> prefer, boolean drops, List<Tag> tags) {

    public UnifyPreview {
        prefer = List.copyOf(prefer);
        tags = List.copyOf(tags);
    }

    /**
     * The preview of {@code plan} over {@code tags}, each tag's id with its items' ids, and {@code changes}, each
     * recipe's id with the ids of the items it named that were (or would be) replaced.
     */
    public static UnifyPreview of(final boolean on, final boolean steppedAside, final List<String> prefer,
                                  final boolean drops, final Map<String, ? extends Collection<String>> tags,
                                  final UnifyPlan plan, final Map<String, Set<String>> changes) {
        final List<Tag> out = new ArrayList<>();
        for (final Map.Entry<String, String> chosen : new TreeMap<>(plan.chosen()).entrySet()) {
            final TreeSet<String> replaced = new TreeSet<>();
            final Collection<String> items = tags.get(chosen.getKey());
            for (final String item : items == null ? List.<String>of() : items) {
                if (chosen.getValue().equals(plan.replace().get(item))) {
                    replaced.add(item);
                }
            }
            if (replaced.isEmpty()) {
                continue;
            }
            int recipes = 0;
            String example = "";
            String before = "";
            for (final Map.Entry<String, Set<String>> recipe : new TreeMap<>(changes).entrySet()) {
                final TreeSet<String> named = new TreeSet<>(recipe.getValue());
                named.retainAll(replaced);
                if (named.isEmpty()) {
                    continue;
                }
                recipes++;
                if (example.isEmpty()) {
                    example = recipe.getKey();
                    before = named.first();
                }
            }
            out.add(new Tag(chosen.getKey(), chosen.getValue(), List.copyOf(replaced), recipes, example, before));
        }
        out.sort(Comparator.comparingInt(Tag::recipes).reversed().thenComparing(Tag::tag));
        return new UnifyPreview(on, steppedAside, prefer, drops, out);
    }

    /**
     * One tag unified.
     *
     * @param tag      its id
     * @param kept     the item that stands for the others
     * @param replaced the items it stands for
     * @param recipes  how many recipes name one of them
     * @param example  one of those recipes, by id, or none
     * @param before   the item that recipe names, before it is replaced by {@code kept}
     */
    public record Tag(String tag, String kept, List<String> replaced, int recipes, String example, String before) {

        public Tag {
            replaced = List.copyOf(replaced);
        }
    }
}
