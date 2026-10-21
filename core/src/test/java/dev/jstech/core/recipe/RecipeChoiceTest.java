/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeChoiceTest {

    private static final List<String> FIT = List.of("othermod:workbench", "minecraft:crafting_table");

    @Test
    void keyOf_isTheSameWhateverTheOrder() {
        assertEquals(RecipeChoice.keyOf(FIT), RecipeChoice.keyOf(List.of("minecraft:crafting_table",
                "othermod:workbench")));
        assertEquals("minecraft:crafting_table,othermod:workbench", RecipeChoice.keyOf(FIT));
    }

    @Test
    void chosen_isTheRememberedThenTheGamesThenTheFirst() {
        assertEquals(0, RecipeChoice.chosen(FIT, "othermod:workbench", "minecraft:crafting_table"));
        assertEquals(1, RecipeChoice.chosen(FIT, null, "minecraft:crafting_table"));
        assertEquals(1, RecipeChoice.chosen(FIT, "gone:recipe", "minecraft:crafting_table"));
        assertEquals(0, RecipeChoice.chosen(FIT, null, null));
    }

    @Test
    void step_goesRoundBothWays() {
        assertEquals(1, RecipeChoice.step(0, 3, true));
        assertEquals(0, RecipeChoice.step(2, 3, true));
        assertEquals(2, RecipeChoice.step(0, 3, false));
        assertEquals(0, RecipeChoice.step(0, 0, true));
    }
}
