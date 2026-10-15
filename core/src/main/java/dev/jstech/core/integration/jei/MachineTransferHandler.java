/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.client.machine.MachineRecipeMoves;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.Machines;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * JEI's "+" on the recipes of one kind of machine, while a machine's screen is open: it moves the recipe into the
 * machine when the machine works it and the player holds a set of it, and greys out with the reason otherwise.
 */
final class MachineTransferHandler implements IRecipeTransferHandler<MachineMenu, RecipeHolder<ProcessingRecipe>> {

    private final ProcessingKind kind;
    private final IRecipeTransferHandlerHelper helper;

    MachineTransferHandler(final ProcessingKind kind, final IRecipeTransferHandlerHelper helper) {
        this.kind = kind;
        this.helper = helper;
    }

    @Override
    public Class<? extends MachineMenu> getContainerClass() {
        return MachineMenu.class;
    }

    @Override
    public Optional<MenuType<MachineMenu>> getMenuType() {
        return Optional.of(Machines.MENU.get());
    }

    @Override
    public RecipeType<RecipeHolder<ProcessingRecipe>> getRecipeType() {
        return ProcessingCategory.typeOf(this.kind);
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(final MachineMenu menu,
                                                         final RecipeHolder<ProcessingRecipe> recipe,
                                                         final IRecipeSlotsView slots, final Player player,
                                                         final boolean maxTransfer, final boolean doTransfer) {
        final Optional<TextKey> refusal = MachineRecipeMoves.refusal(menu, player, recipe.value());
        if (refusal.isPresent()) {
            return this.helper.createUserErrorWithTooltip(GameText.component(refusal.get()));
        }
        if (doTransfer) {
            MachineRecipeMoves.move(menu, recipe.id(), maxTransfer);
        }
        return null;
    }
}
