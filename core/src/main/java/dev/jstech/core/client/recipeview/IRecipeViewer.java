/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import net.minecraft.world.item.ItemStack;

/**
 * A recipe viewer as the Core's screens ask it: JEI and EMI each have one, which the Core's bridge to it adds to
 * {@link RecipeViewers} when the viewer starts, so a screen opens recipes and reads what the pointer is over in the
 * viewer's lists without naming either viewer's classes, which are only there when it is installed.
 */
public interface IRecipeViewer {

    /** Opens the viewer on the recipes that make {@code stack}; whether it was running to open. */
    boolean showRecipesOf(ItemStack stack);

    /** Opens the viewer on the recipes {@code stack} goes into; whether it was running to open. */
    boolean showUsesOf(ItemStack stack);

    /** The item under the pointer in the viewer's own lists and recipe screens, or an empty stack. */
    ItemStack hovered();
}
