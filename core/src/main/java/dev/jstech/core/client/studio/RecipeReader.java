/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The recipes: those that make the thing's item, the first opened by Open, or, for research, those that need it. None
 * is said as none, not as a problem: a thing may be made by no recipe on purpose.
 */
final class RecipeReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.CRAFTING_TABLE);

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        if (thing.kind() == ThingKind.RESEARCH) {
            final int needing = context.recipesNeeding(thing.id()).size();
            sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.RECIPE), needing == 0
                    ? GameText.resolve(StudioTexts.NONE) : GameText.resolve(StudioTexts.RECIPES_NEED.with(needing)),
                    needing == 0 ? StudioSheet.Mark.NONE : StudioSheet.Mark.FINE, Optional.empty()));
            return;
        }
        if (thing.item() == null) {
            return;
        }
        final List<RecipeHolder<?>> making = context.recipesMaking(thing.item());
        if (making.isEmpty()) {
            sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.RECIPE),
                    GameText.resolve(StudioTexts.NONE), StudioSheet.Mark.NONE, Optional.empty()));
            return;
        }
        final ResourceLocation first = making.getFirst().id();
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.RECIPE),
                GameText.resolve(StudioTexts.RECIPES_COUNT.with(making.size(), first.toString())),
                StudioSheet.Mark.FINE, Optional.of(StudioFiles.data(ResourceLocation.fromNamespaceAndPath(
                first.getNamespace(), "recipe/" + first.getPath() + ".json")))));
    }
}
