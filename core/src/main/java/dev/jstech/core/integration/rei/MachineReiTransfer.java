/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.rei;

import dev.jstech.core.client.machine.MachineRecipeMoves;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * REI's "+" on every processing machine's recipes, while a machine's screen is open: the same move as JEI's "+" and
 * EMI's fill button, once, or a stack's worth when the player asks for more; greyed out with the reason otherwise.
 */
final class MachineReiTransfer implements TransferHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Result handle(final Context context) {
        if (!(context.getMenu() instanceof MachineMenu menu) || !(context.getDisplay() instanceof DeclaredReiDisplay<?>
                display) || !(display.recipe() instanceof RecipeHolder<?> holder)
                || !(holder.value() instanceof ProcessingRecipe recipe)) {
            return Result.createNotApplicable();
        }
        final Player player = Minecraft.getInstance().player;
        if (player == null) {
            return Result.createNotApplicable();
        }
        final Optional<TextKey> refusal = MachineRecipeMoves.refusal(menu, player, recipe);
        if (refusal.isPresent()) {
            return Result.createFailed(GameText.component(refusal.get()));
        }
        if (context.isActuallyCrafting()) {
            MachineRecipeMoves.move(menu, ((RecipeHolder<ProcessingRecipe>) holder).id(), context.isStackedCrafting());
            Minecraft.getInstance().setScreen(context.getContainerScreen());
        }
        return Result.createSuccessful();
    }
}
