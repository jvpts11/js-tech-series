/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.ApiStatus;

/**
 * The fluid a block entity holds, declared with where it goes: {@link #save()}, {@link #toClient()}, and
 * {@link #exposed()}, offered to pipes and machines on every side.
 */
public final class FieldFluidTank extends FluidTank implements IField {

    private final FieldFlags flags;
    private boolean exposed;
    /** Whether pipes may only draw from it: a machine's output, filled by the machine alone. */
    private boolean outputOnly;
    /** Whether pipes may only fill it: a machine's input, drawn on by the machine alone. */
    private boolean inputOnly;

    FieldFluidTank(final FieldFlags flags, final int capacity) {
        super(capacity);
        this.flags = flags;
    }

    /** Lets pipes draw from the tank but never fill it; the block fills it through {@link #fillInside}. */
    public FieldFluidTank outputOnly() {
        outputOnly = true;
        return this;
    }

    /**
     * Lets pipes fill the tank but never draw from it, as a face that gives out gives what the machine made; the block
     * draws on it through {@link #drainInside}.
     */
    @ApiStatus.Experimental
    public FieldFluidTank inputOnly() {
        inputOnly = true;
        return this;
    }

    /** Fills the tank from inside the block, as a machine puts out what it made, whatever pipes may do. */
    public int fillInside(final FluidStack resource, final FluidAction action) {
        return super.fill(resource, action);
    }

    /** Draws from the tank inside the block, as a machine spends what it was given, whatever pipes may do. */
    @ApiStatus.Experimental
    public FluidStack drainInside(final int maxDrain, final FluidAction action) {
        return super.drain(maxDrain, action);
    }

    @Override
    public int fill(final FluidStack resource, final FluidAction action) {
        return outputOnly ? 0 : super.fill(resource, action);
    }

    @Override
    public FluidStack drain(final FluidStack resource, final FluidAction action) {
        return inputOnly ? FluidStack.EMPTY : super.drain(resource, action);
    }

    @Override
    public FluidStack drain(final int maxDrain, final FluidAction action) {
        return inputOnly ? FluidStack.EMPTY : super.drain(maxDrain, action);
    }

    /** Keeps the fluid in the save. */
    public FieldFluidTank save() {
        flags.save();
        return this;
    }

    /** Sends the fluid to the players who see the block. */
    public FieldFluidTank toClient() {
        flags.toClient();
        return this;
    }

    /** Offers the tank to pipes and machines on every side. */
    public FieldFluidTank exposed() {
        exposed = true;
        return this;
    }

    boolean isExposed() {
        return exposed;
    }

    @Override
    protected void onContentsChanged() {
        flags.changed();
    }

    @Override
    @ApiStatus.Internal
    public FieldFlags flags() {
        return flags;
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(flags.key(), writeToNBT(registries, new CompoundTag()));
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        readFromNBT(registries, tag.getCompound(flags.key()));
    }

    @Override
    public int menuSlots() {
        return 0;
    }
}
