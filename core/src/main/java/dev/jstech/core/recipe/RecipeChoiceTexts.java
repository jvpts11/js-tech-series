/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the chooser of recipes says, beside the result of a crafting grid or a furnace. */
@TextHolder
public final class RecipeChoiceTexts {

    public static final TextKey FIT = TextKey.of("jscore.recipe_choice.fit", "%s recipes fit");
    public static final TextKey NEXT = TextKey.of("jscore.recipe_choice.next", "Click: the next one");
    public static final TextKey BEFORE = TextKey.of("jscore.recipe_choice.before", "Right-click: the one before");
    public static final TextKey KEPT_GRID = TextKey.of("jscore.recipe_choice.kept_grid", "Kept for this grid");
    public static final TextKey KEPT_FURNACE = TextKey.of("jscore.recipe_choice.kept_furnace",
            "Kept for %s in this furnace");

    private RecipeChoiceTexts() {
    }
}
