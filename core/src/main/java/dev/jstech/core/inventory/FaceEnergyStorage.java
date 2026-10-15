/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import java.util.Objects;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * An FE store as one face of a block gives it out: the block's own, taking energy only through a face that lets
 * things in, and giving it only through a face that lets things out.
 */
public final class FaceEnergyStorage implements IEnergyStorage {

    private final IEnergyStorage inner;
    private final FaceMode mode;

    /**
     * @param inner the block's own store
     * @param mode  what the face lets through
     */
    public FaceEnergyStorage(final IEnergyStorage inner, final FaceMode mode) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        return this.mode.takesIn() ? this.inner.receiveEnergy(toReceive, simulate) : 0;
    }

    @Override
    public int extractEnergy(final int toExtract, final boolean simulate) {
        return this.mode.givesOut() ? this.inner.extractEnergy(toExtract, simulate) : 0;
    }

    @Override
    public int getEnergyStored() {
        return this.inner.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return this.inner.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return this.mode.givesOut() && this.inner.canExtract();
    }

    @Override
    public boolean canReceive() {
        return this.mode.takesIn() && this.inner.canReceive();
    }
}
