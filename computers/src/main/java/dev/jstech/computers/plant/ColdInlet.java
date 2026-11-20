/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import java.util.Objects;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A cooling loop's COLD inlet (computers/docs/design/temperature.md): filled only, with Cold Water, or with plain
 * water, which it takes as Cold Water, so a loop is filled once from any water supply, as a real plant fills its loops
 * from the mains.
 */
public final class ColdInlet implements IFluidHandler {

    private final IFluidHandler tank;

    /** @param tank the tank of Cold Water it fills */
    public ColdInlet(final IFluidHandler tank) {
        this.tank = Objects.requireNonNull(tank, "tank");
    }

    /** Whether a COLD inlet takes {@code stack}: Cold Water, or plain water as Cold Water. */
    public static boolean takes(final FluidStack stack) {
        return stack.is(CoolingWater.COLD.get()) || stack.is(Fluids.WATER);
    }

    @Override
    public int getTanks() {
        return this.tank.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(final int index) {
        return this.tank.getFluidInTank(index);
    }

    @Override
    public int getTankCapacity(final int index) {
        return this.tank.getTankCapacity(index);
    }

    @Override
    public boolean isFluidValid(final int index, final FluidStack stack) {
        return takes(stack);
    }

    @Override
    public int fill(final FluidStack resource, final FluidAction action) {
        return takes(resource) ? this.tank.fill(new FluidStack(CoolingWater.COLD.get(), resource.getAmount()), action)
                : 0;
    }

    @Override
    public FluidStack drain(final FluidStack resource, final FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(final int maxDrain, final FluidAction action) {
        return FluidStack.EMPTY;
    }
}
