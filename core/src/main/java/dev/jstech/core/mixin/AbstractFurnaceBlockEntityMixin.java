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
import dev.jstech.core.room.Flames;
import dev.jstech.core.room.RoomBalance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A furnace smelts by the recipe it was set to for what it smelts, when more than one fits it ({@link RecipeChoices}),
 * both as it works and as it reckons how long an item takes; the game's own choice otherwise. It lights its fuel only
 * where a fire could burn, in the air round it ({@link Flames}), while fire needs oxygen in the Core's settings. The
 * furnace, the blast furnace, the smoker and every mod's furnace built on the game's alike.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;canBurn("
                    + "Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/RecipeHolder;"
                    + "Lnet/minecraft/core/NonNullList;I"
                    + "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)Z"))
    private static boolean jscore$lightsWhereFireBurns(final RegistryAccess registries,
            final @Nullable RecipeHolder<?> recipe, final NonNullList<ItemStack> items, final int most,
            final AbstractFurnaceBlockEntity furnace, final Operation<Boolean> original,
            @Local(argsOnly = true) final Level level, @Local(argsOnly = true) final BlockPos pos,
            @Local(argsOnly = true) final BlockState state) {
        return original.call(registries, recipe, items, most, furnace) && (!RoomBalance.fireNeedsOxygen()
                || !(level instanceof ServerLevel server) || Flames.canBurn(server, pos, state));
    }

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
