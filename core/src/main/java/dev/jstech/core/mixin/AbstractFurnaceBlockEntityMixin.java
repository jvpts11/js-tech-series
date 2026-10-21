/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.jstech.core.recipe.RecipeChoices;
import java.util.Optional;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A furnace smelts by the recipe it was set to for what it smelts, when more than one fits it ({@link RecipeChoices}),
 * both as it works and as it reckons how long an item takes; the game's own choice otherwise. The furnace, the blast
 * furnace, the smoker and every mod's furnace built on the game's alike.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @WrapOperation(method = {"serverTick", "getTotalCookTime"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor("
                    + "Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)"
                    + "Ljava/util/Optional;"))
    private static Optional<RecipeHolder<AbstractCookingRecipe>> jscore$chosenRecipe(
            final RecipeManager.CachedCheck<SingleRecipeInput, AbstractCookingRecipe> check, final RecipeInput input,
            final Level level, final Operation<Optional<RecipeHolder<AbstractCookingRecipe>>> original,
            @Local(argsOnly = true) final AbstractFurnaceBlockEntity furnace) {
        final Optional<RecipeHolder<AbstractCookingRecipe>> found = original.call(check, input, level);
        return input instanceof SingleRecipeInput single ? RecipeChoices.furnace(furnace, single, level, found)
                : found;
    }
}
