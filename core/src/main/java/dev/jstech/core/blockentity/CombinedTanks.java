/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Several tanks of a block offered as one to pipes: a pipe that fills puts into the first tank that takes the fluid,
 * one that drains draws from the first that holds it, in the order the tanks were declared. A tank only a machine
 * fills refuses pipes that fill.
 */
final class CombinedTanks implements IFluidHandler {

    private final List<FieldFluidTank> tanks;

    CombinedTanks(final List<FieldFluidTank> tanks) {
        this.tanks = List.copyOf(tanks);
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public FluidStack getFluidInTank(final int tank) {
        return tanks.get(tank).getFluid();
    }

    @Override
    public int getTankCapacity(final int tank) {
        return tanks.get(tank).getCapacity();
    }

    @Override
    public boolean isFluidValid(final int tank, final FluidStack stack) {
        return tanks.get(tank).isFluidValid(stack);
    }

    @Override
    public int fill(final FluidStack resource, final FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        // A tank already holding the fluid first, so a stream does not spread over several tanks.
        for (final FieldFluidTank tank : tanks) {
            if (FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) {
                final int filled = tank.fill(resource, action);
                if (filled > 0) {
                    return filled;
                }
            }
        }
        for (final FieldFluidTank tank : tanks) {
            final int filled = tank.fill(resource, action);
            if (filled > 0) {
                return filled;
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(final FluidStack resource, final FluidAction action) {
        for (final FieldFluidTank tank : tanks) {
            final FluidStack drained = tank.drain(resource, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(final int maxDrain, final FluidAction action) {
        for (final FieldFluidTank tank : tanks) {
            final FluidStack drained = tank.drain(maxDrain, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }
}
