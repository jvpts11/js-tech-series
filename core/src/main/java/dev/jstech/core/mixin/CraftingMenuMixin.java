/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.jstech.core.progression.KnowledgeGate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Says who is at the crafting grid while the game works out its result, the crafting table's and the inventory's
 * alike: a crafting recipe that asks for knowledge matches only for a player whose team knows it, and a recipe is
 * asked only whether it matches the grid and the world, never who is crafting.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @WrapMethod(method = "slotChangedCraftingGrid")
    private static void jscore$craftAsThePlayer(final AbstractContainerMenu menu, final Level level,
                                                final Player player, final CraftingContainer craftSlots,
                                                final ResultContainer resultSlots,
                                                final @Nullable RecipeHolder<CraftingRecipe> recipe,
                                                final Operation<Void> original) {
        KnowledgeGate.craftingAs(player, () -> original.call(menu, level, player, craftSlots, resultSlots, recipe));
    }
}
