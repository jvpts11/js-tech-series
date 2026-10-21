/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.jstech.core.client.machine.MachineRecipeMoves;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.ProcessingRecipe;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * EMI's fill button on every processing machine's recipes, while a machine's screen is open: the same move as JEI's
 * "+", once, or a stack's worth when EMI asks for more than one.
 */
final class MachineRecipeHandler implements EmiRecipeHandler<MachineMenu> {

    @Override
    public EmiPlayerInventory getInventory(final AbstractContainerScreen<MachineMenu> screen) {
        final Player player = Minecraft.getInstance().player;
        return player == null ? new EmiPlayerInventory(List.of()) : EmiPlayerInventory.of(player);
    }

    @Override
    public boolean supportsRecipe(final EmiRecipe recipe) {
        return processing(recipe).isPresent();
    }

    @Override
    public boolean canCraft(final EmiRecipe recipe, final EmiCraftContext<MachineMenu> context) {
        final Player player = Minecraft.getInstance().player;
        return player != null && processing(recipe).filter(holder -> MachineRecipeMoves.refusal(
                context.getScreenHandler(), player, holder.value()).isEmpty()).isPresent();
    }

    @Override
    public boolean craft(final EmiRecipe recipe, final EmiCraftContext<MachineMenu> context) {
        final Optional<RecipeHolder<ProcessingRecipe>> holder = processing(recipe);
        if (holder.isEmpty() || !canCraft(recipe, context)) {
            return false;
        }
        MachineRecipeMoves.move(context.getScreenHandler(), holder.get().id(), context.getAmount() > 1);
        Minecraft.getInstance().setScreen(context.getScreen());
        return true;
    }

    /* The processing recipe behind an EMI recipe, when it is one. */
    @SuppressWarnings("unchecked")
    private static Optional<RecipeHolder<ProcessingRecipe>> processing(final EmiRecipe recipe) {
        return recipe instanceof DeclaredEmiRecipe<?> each && each.getBackingRecipe() != null
                && each.getBackingRecipe().value() instanceof ProcessingRecipe
                ? Optional.of((RecipeHolder<ProcessingRecipe>) each.getBackingRecipe()) : Optional.empty();
    }
}
