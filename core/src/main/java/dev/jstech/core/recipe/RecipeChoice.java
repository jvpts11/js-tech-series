/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Which of the recipes that fit is the one used: the one the player chose for the same recipes, kept under the
 * recipes' ids whatever order they come in; or the one the game would have used. Stepping through them goes round.
 *
 * <p>Pure: it is handed recipes by their ids as text.
 */
public final class RecipeChoice {

    private static final String BETWEEN = ",";

    private RecipeChoice() {
    }

    /** What a choice between {@code recipes} is kept under: their ids, sorted, so their order does not count. */
    public static String keyOf(final List<String> recipes) {
        final List<String> sorted = new ArrayList<>(recipes);
        sorted.sort(null);
        return String.join(BETWEEN, sorted);
    }

    /**
     * The place in {@code recipes} of the one used: {@code remembered} when it is among them, else {@code otherwise}
     * (the one the game would have used), else the first.
     */
    public static int chosen(final List<String> recipes, @Nullable final String remembered,
                             @Nullable final String otherwise) {
        if (remembered != null && recipes.contains(remembered)) {
            return recipes.indexOf(remembered);
        }
        if (otherwise != null && recipes.contains(otherwise)) {
            return recipes.indexOf(otherwise);
        }
        return 0;
    }

    /** The place after {@code at} among {@code count}, or before it, going round. */
    public static int step(final int at, final int count, final boolean forward) {
        if (count <= 0) {
            return 0;
        }
        return Math.floorMod(at + (forward ? 1 : -1), count);
    }
}
