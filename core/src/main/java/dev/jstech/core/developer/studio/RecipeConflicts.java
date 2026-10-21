/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Recipes that take the same inputs: two of one kind whose grid, or whose list of inputs in any order, asks for the
 * same items, so only one of them can ever be what a player gets. Each recipe is read as its kind, its shape and, for
 * each place, the items it takes there by their ids.
 */
public final class RecipeConflicts {

    private RecipeConflicts() {
    }

    /** The groups of two or more of {@code recipes} that take the same inputs, in the order they were first seen. */
    public static List<List<String>> of(final List<Inputs> recipes) {
        final Map<String, List<String>> bySignature = new LinkedHashMap<>();
        for (final Inputs recipe : recipes) {
            bySignature.computeIfAbsent(recipe.signature(), any -> new ArrayList<>()).add(recipe.id());
        }
        return bySignature.values().stream().filter(ids -> ids.size() > 1).map(List::copyOf).toList();
    }

    /**
     * What one recipe takes.
     *
     * @param id      its id
     * @param kind    its kind, as {@code minecraft:crafting_shaped}
     * @param width   how many places across its grid has, or 0 for a recipe whose inputs go in any order
     * @param places  each place's items, by their ids, left to right and down; an empty one takes nothing
     */
    public record Inputs(String id, String kind, int width, List<List<String>> places) {

        public Inputs {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kind, "kind");
            places = places.stream().map(items -> items.stream().sorted().toList()).toList();
        }

        /* Its kind, its shape and its places, the places sorted when their order does not count. */
        String signature() {
            final List<String> written = new ArrayList<>(this.places.stream().map(String::valueOf).toList());
            if (this.width == 0) {
                written.removeIf("[]"::equals);
                written.sort(null);
            }
            return this.kind + "|" + this.width + "|" + written;
        }
    }
}
