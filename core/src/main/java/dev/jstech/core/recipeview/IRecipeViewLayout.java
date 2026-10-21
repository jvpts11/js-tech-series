/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipeview;

/**
 * How a kind lays out one recipe's page: its slots, lines, sprites and arrow, at their places.
 *
 * @param <T> what one recipe is
 */
@FunctionalInterface
public interface IRecipeViewLayout<T> {

    /** Lays {@code recipe} out on {@code page}. */
    void lay(T recipe, RecipeView.Builder page);
}
