/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import dev.jstech.core.recipeview.RecipeViews;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Every kind of recipe the viewers show from the Core's kit, in one list for the bridges to JEI, EMI and REI: the
 * processing machines every mod declared, then the kinds declared for the viewers themselves ({@link RecipeViews}).
 * What a slot's chance says is the same in each viewer that does not show chances itself.
 */
public final class ViewKinds {

    private ViewKinds() {
    }

    /** Every kind, the machines' first. */
    public static List<RecipeViewKind<?>> all() {
        final List<RecipeViewKind<?>> out = new ArrayList<>();
        for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
            out.add(ProcessingRecipeViews.viewKind(kind));
        }
        out.addAll(RecipeViews.declared());
        return out;
    }

    /** The lines a slot's tooltip adds, with its chance last when it comes by chance. */
    public static List<Component> tooltipOf(final RecipeView.Slot slot) {
        final List<Component> out = new ArrayList<>(slot.tooltip());
        if (slot.chance() < 1.0f) {
            out.add(ProcessingRecipeViews.chance(slot.chance()));
        }
        return out;
    }
}
