/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;

/**
 * The recipes a manual shows, in words. A page lists the recipes of a type by its id, and only the running game reads
 * what they are from the world it is in, so the search is told their names by it: what each recipe makes, what goes
 * into it, and what works it.
 */
public interface IRecipeWords {

    /** No recipes known, as a book searched outside the game has. */
    IRecipeWords NONE = (type, output) -> List.of();

    /** The recipes of {@code type}, those making {@code output} when one is named, in the order a page shows them. */
    List<RecipeWords> of(String type, String output);

    /**
     * One recipe in words.
     *
     * @param makes the names of what it makes, the first drawn beside it in a search
     * @param takes the names of what goes into it, every choice of each input
     * @param kind  what works it, a machine's or a crafting table's name
     * @param icon  the id of the item it makes first, drawn beside it in a search, or empty
     */
    record RecipeWords(List<String> makes, List<String> takes, String kind, String icon) {

        public RecipeWords {
            makes = List.copyOf(makes);
            takes = List.copyOf(takes);
        }
    }
}
