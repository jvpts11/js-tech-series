/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import dev.jstech.core.transfer.Batch;
import dev.jstech.core.transfer.HandlerSteps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Moving things from one store to another, as a bus, a pipe or a machine's output does: items that pass a filter, a
 * fluid, energy. Each move takes out and puts in as one {@link Batch}, so what leaves one store always arrives in the
 * other, or stays where it was.
 */
public final class Transfers {

    private Transfers() {
    }

    /**
     * Moves up to {@code max} items that pass {@code filter} from {@code from} to {@code to}, stack by stack in the
     * order of {@code from}'s slots.
     *
     * @return how many moved
     */
    public static int moveItems(final IItemHandler from, final IItemHandler to, final ItemFilter filter,
                                final int max) {
        int moved = 0;
        for (int slot = 0; slot < from.getSlots() && moved < max; slot++) {
            final ItemStack there = from.getStackInSlot(slot);
            if (there.isEmpty() || !ItemFilters.allows(filter, there)) {
                continue;
            }
            final ItemStack taken = from.extractItem(slot, Math.min(max - moved, there.getCount()), true);
            if (taken.isEmpty()) {
                continue;
            }
            final int fits = taken.getCount() - ItemHandlerHelper.insertItem(to, taken.copy(), true).getCount();
            if (fits <= 0) {
                continue;
            }
            final Batch.Outcome outcome = new Batch()
                    .add(HandlerSteps.extract(from, taken, fits))
                    .add(HandlerSteps.insert(to, taken.copyWithCount(fits)))
                    .commit();
            if (outcome.done()) {
                moved += fits;
            }
        }
        return moved;
    }

    /**
     * Moves up to {@code max} millibuckets of whatever fluid {@code from} gives first into {@code to}.
     *
     * @return how much moved
     */
    public static int moveFluid(final IFluidHandler from, final IFluidHandler to, final int max) {
        final FluidStack offered = from.drain(max, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return 0;
        }
        final int fits = to.fill(offered.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (fits <= 0) {
            return 0;
        }
        final FluidStack moving = offered.copyWithAmount(fits);
        return new Batch().add(HandlerSteps.drain(from, moving)).add(HandlerSteps.fill(to, moving)).commit().done()
                ? fits : 0;
    }

    /**
     * Moves up to {@code max} FE from {@code from} into {@code to}.
     *
     * @return how much moved
     */
    public static int moveEnergy(final IEnergyStorage from, final IEnergyStorage to, final int max) {
        final int fits = to.receiveEnergy(from.extractEnergy(max, true), true);
        if (fits <= 0) {
            return 0;
        }
        return new Batch().add(HandlerSteps.extract(from, fits)).add(HandlerSteps.receive(to, fits)).commit().done()
                ? fits : 0;
    }
}
