/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * What a furnace's window smelts by and whose it is, which the game keeps to itself: the recipes that fit what is in
 * an open furnace are found by them, for the player to choose between.
 */
@Mixin(AbstractFurnaceMenu.class)
public interface AbstractFurnaceMenuAccessor {

    /** The kind of recipe the furnace smelts by: smelting, blasting or smoking, or a mod's own. */
    @Accessor("recipeType")
    RecipeType<? extends AbstractCookingRecipe> jscore$recipeType();

    /** The furnace's own container: its block entity on the server, a stand-in on a player's game. */
    @Accessor("container")
    Container jscore$container();
}
