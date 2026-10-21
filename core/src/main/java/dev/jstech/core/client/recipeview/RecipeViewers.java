/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.item.ItemStack;

/**
 * The recipe viewers running on this game, JEI's and EMI's when they are installed. Each one is added by the Core's
 * bridge to it as the viewer starts; the Core's screens ask them through here, the first one added answering first.
 * With none installed every question is answered with nothing, and nothing opens.
 */
public final class RecipeViewers {

    private static final CopyOnWriteArrayList<IRecipeViewer> VIEWERS = new CopyOnWriteArrayList<>();

    private RecipeViewers() {
    }

    /** Adds a viewer, once however many times its bridge starts. */
    public static void add(final IRecipeViewer viewer) {
        VIEWERS.addIfAbsent(viewer);
    }

    /** Whether a viewer runs on this game, so a click on a recipe has somewhere to open it. */
    public static boolean any() {
        return !VIEWERS.isEmpty();
    }

    /** Opens a viewer on the recipes that make {@code stack}; whether one opened. */
    public static boolean showRecipesOf(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (final IRecipeViewer viewer : VIEWERS) {
            if (viewer.showRecipesOf(stack)) {
                return true;
            }
        }
        return false;
    }

    /** Opens a viewer on the recipes {@code stack} goes into; whether one opened. */
    public static boolean showUsesOf(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (final IRecipeViewer viewer : VIEWERS) {
            if (viewer.showUsesOf(stack)) {
                return true;
            }
        }
        return false;
    }

    /** The item under the pointer in a viewer's lists and recipe screens, or an empty stack. */
    public static ItemStack hovered() {
        for (final IRecipeViewer viewer : VIEWERS) {
            final ItemStack under = viewer.hovered();
            if (!under.isEmpty()) {
                return under;
            }
        }
        return ItemStack.EMPTY;
    }
}
