/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import java.util.Objects;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A fluid handler as one face of a block gives it out: the block's own, filled only through a face that lets things
 * in and only with what the filter lets pass, and drained only through a face that lets things out.
 */
public final class FaceFluidHandler implements IFluidHandler {

    private final IFluidHandler inner;
    private final FaceMode mode;
    private final ItemFilter filter;

    /**
     * @param inner  the block's own handler
     * @param mode   what the face lets through
     * @param filter what may go in
     */
    public FaceFluidHandler(final IFluidHandler inner, final FaceMode mode, final ItemFilter filter) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.filter = Objects.requireNonNull(filter, "filter");
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
        return this.mode.takesIn() && passes(stack) && this.inner.isFluidValid(tank, stack);
    }

    @Override
    public int fill(final FluidStack resource, final FluidAction action) {
        return this.mode.takesIn() && passes(resource) ? this.inner.fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(final FluidStack resource, final FluidAction action) {
        return this.mode.givesOut() ? this.inner.drain(resource, action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(final int maxDrain, final FluidAction action) {
        return this.mode.givesOut() ? this.inner.drain(maxDrain, action) : FluidStack.EMPTY;
    }

    private boolean passes(final FluidStack stack) {
        return stack.isEmpty() || this.filter.allows(new FluidSubject(stack));
    }
}
