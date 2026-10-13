/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a recipe is called when neither its author nor its result gives it a name. */
@TextHolder
final class RecipeTexts {

    static final TextKey UNNAMED_RECIPE = TextKey.of("jsc.crafting.unnamed_recipe", "recipe");
    static final TextKey UNNAMED_PIPELINE = TextKey.of("jsc.crafting.unnamed_pipeline", "pipeline");

    private RecipeTexts() {
    }
}
