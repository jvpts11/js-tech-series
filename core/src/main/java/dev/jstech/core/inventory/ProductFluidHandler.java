/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import dev.jstech.core.multiblock.PortProducts;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.ApiStatus;

/**
 * A fluid handler that lets its own fluids alone through, filled only with them and drained only of them: the tanks
 * of a machine as one of its ports bound to those products gives them out (see {@link PortProducts}).
 */
@ApiStatus.Experimental
public final class ProductFluidHandler implements IFluidHandler {

    private final IFluidHandler inner;
    private final Set<ResourceLocation> fluids;

    /**
     * @param inner  the machine's own tanks
     * @param fluids the ids of the fluids it lets through
     */
    public ProductFluidHandler(final IFluidHandler inner, final Set<ResourceLocation> fluids) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.fluids = Set.copyOf(fluids);
    }

    @Override
    public int getTanks() {
        return this.inner.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(final int tank) {
        return this.inner.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(final int tank) {
        return this.inner.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(final int tank, final FluidStack stack) {
        return is(stack) && this.inner.isFluidValid(tank, stack);
    }

    @Override
    public int fill(final FluidStack resource, final FluidAction action) {
        return is(resource) ? this.inner.fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(final FluidStack resource, final FluidAction action) {
        return is(resource) ? this.inner.drain(resource, action) : FluidStack.EMPTY;
    }

    /** Drains one of its own fluids from the first tank that holds one, up to {@code maxDrain}; nothing else. */
    @Override
    public FluidStack drain(final int maxDrain, final FluidAction action) {
        for (int tank = 0; tank < this.inner.getTanks(); tank++) {
            final FluidStack held = this.inner.getFluidInTank(tank);
            if (is(held)) {
                return this.inner.drain(held.copyWithAmount(Math.min(maxDrain, held.getAmount())), action);
            }
        }
        return FluidStack.EMPTY;
    }

    private boolean is(final FluidStack stack) {
        return !stack.isEmpty() && this.fluids.contains(BuiltInRegistries.FLUID.getKey(stack.getFluid()));
    }
}
