/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The energy an item holds, seen as the game's energy storage: what it holds is the item's {@link CoreEnergy#ENERGY}
 * component, so it goes wherever the item goes and is saved with it. The game counts energy in ints, so an item that
 * holds more than an int says it holds as much as an int can, and takes and gives in steps an int holds.
 */
public final class ItemEnergyStorage implements IEnergyStorage {

    private final ItemStack stack;
    private final long capacity;
    private final int maxReceive;
    private final int maxExtract;

    /**
     * @param stack      the item
     * @param capacity   the most FE it holds
     * @param maxReceive the most FE it takes at a time
     * @param maxExtract the most FE it gives at a time
     */
    public ItemEnergyStorage(final ItemStack stack, final long capacity, final int maxReceive, final int maxExtract) {
        this.stack = Objects.requireNonNull(stack, "stack");
        if (capacity < 0 || maxReceive < 0 || maxExtract < 0) {
            throw new IllegalArgumentException("an item's energy is never below nothing");
        }
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    /** The FE the item holds, in full. */
    public long stored() {
        return Math.clamp(this.stack.getOrDefault(CoreEnergy.ENERGY.get(), 0L), 0L, this.capacity);
    }

    /** Sets the FE the item holds, held to what it can. */
    public void setStored(final long amount) {
        this.stack.set(CoreEnergy.ENERGY.get(), Math.clamp(amount, 0L, this.capacity));
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        if (!canReceive() || toReceive <= 0) {
            return 0;
        }
        final long held = stored();
        final int taken = (int) Math.min(Math.min(toReceive, this.maxReceive), this.capacity - held);
        if (!simulate && taken > 0) {
            setStored(held + taken);
        }
        return taken;
    }

    @Override
    public int extractEnergy(final int toExtract, final boolean simulate) {
        if (!canExtract() || toExtract <= 0) {
            return 0;
        }
        final long held = stored();
        final int given = (int) Math.min(Math.min(toExtract, this.maxExtract), held);
        if (!simulate && given > 0) {
            setStored(held - given);
        }
        return given;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, stored());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, this.capacity);
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }
}
