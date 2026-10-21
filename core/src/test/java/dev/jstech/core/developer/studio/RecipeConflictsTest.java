/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeConflictsTest {

    private static final String SHAPED = "minecraft:crafting_shaped";
    private static final String SHAPELESS = "minecraft:crafting_shapeless";

    @Test
    void of_findsTwoGridsThatAskForTheSameItems() {
        final List<RecipeConflicts.Inputs> recipes = List.of(
                new RecipeConflicts.Inputs("a:table", SHAPED, 2, List.of(List.of("planks"), List.of("planks"),
                        List.of("planks"), List.of("planks"))),
                new RecipeConflicts.Inputs("b:bench", SHAPED, 2, List.of(List.of("planks"), List.of("planks"),
                        List.of("planks"), List.of("planks"))),
                new RecipeConflicts.Inputs("a:slab", SHAPED, 3, List.of(List.of("planks"), List.of("planks"),
                        List.of("planks"))));
        assertEquals(List.of(List.of("a:table", "b:bench")), RecipeConflicts.of(recipes));
    }

    @Test
    void of_readsAListOfInputsInAnyOrder() {
        final List<RecipeConflicts.Inputs> recipes = List.of(
                new RecipeConflicts.Inputs("a:mix", SHAPELESS, 0, List.of(List.of("sand"), List.of("gravel"))),
                new RecipeConflicts.Inputs("b:mix", SHAPELESS, 0, List.of(List.of("gravel"), List.of("sand"))));
        assertEquals(List.of(List.of("a:mix", "b:mix")), RecipeConflicts.of(recipes));
    }

    @Test
    void of_seesAPlaceThatTakesSeveralItemsInAnyOrderOfThem() {
        final List<RecipeConflicts.Inputs> recipes = List.of(
                new RecipeConflicts.Inputs("a:x", SHAPELESS, 0, List.of(List.of("oak", "birch"))),
                new RecipeConflicts.Inputs("b:x", SHAPELESS, 0, List.of(List.of("birch", "oak"))));
        assertEquals(1, RecipeConflicts.of(recipes).size());
    }

    @Test
    void of_keepsKindsApart() {
        final List<RecipeConflicts.Inputs> recipes = List.of(
                new RecipeConflicts.Inputs("a:x", SHAPELESS, 0, List.of(List.of("sand"))),
                new RecipeConflicts.Inputs("b:x", "minecraft:smelting", 0, List.of(List.of("sand"))));
        assertEquals(List.of(), RecipeConflicts.of(recipes));
    }
}
