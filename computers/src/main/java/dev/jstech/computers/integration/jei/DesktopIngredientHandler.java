/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.jei;

import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.storage.StorageKey;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.runtime.IClickableIngredient;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Optional;

/**
 * Lets the viewer show the recipes and uses of a stack a program on the desktop shows under the cursor, as it does
 * over a slot: the Network Interactor's grid is drawn by the desktop itself, so the viewer cannot see its cells as
 * slots and has to be told what is under the cursor. Items and liquids; a gas is no ingredient the viewer knows.
 */
public final class DesktopIngredientHandler implements IGuiContainerHandler<DesktopScreen> {

    /* Half the side of the square the viewer marks round the cursor: about a grid cell. */
    private static final int HALF_CELL = 8;

    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
            final IClickableIngredientFactory factory, final DesktopScreen screen, final double mouseX,
            final double mouseY) {
        final StorageKey key = screen.keyUnderMouse(mouseX, mouseY);
        if (key == null) {
            return Optional.empty();
        }
        final int x = (int) Math.floor(mouseX) - HALF_CELL;
        final int y = (int) Math.floor(mouseY) - HALF_CELL;
        if (key.isItem()) {
            return factory.createBuilder(key.stack(1)).buildWithArea(x, y, 2 * HALF_CELL, 2 * HALF_CELL);
        }
        if (key.isFluid()) {
            return factory.createBuilder(NeoForgeTypes.FLUID_STACK, key.fluidStack(FluidType.BUCKET_VOLUME))
                    .buildWithArea(x, y, 2 * HALF_CELL, 2 * HALF_CELL);
        }
        return Optional.empty();
    }
}
