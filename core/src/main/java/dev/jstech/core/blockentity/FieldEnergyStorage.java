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
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * The FE a block entity holds, declared with where it goes: {@link #save()}, {@link #toClient()},
 * {@link #toMenu()} (its stored amount), and {@link #exposed()}, offered to cables and generators on every side.
 * Besides what cables do to it, the block entity itself spends and makes energy with {@link #consume} and
 * {@link #generate}.
 */
public final class FieldEnergyStorage extends EnergyStorage implements IField {

    private final FieldFlags flags;
    private boolean exposed;

    FieldEnergyStorage(final FieldFlags flags, final int capacity, final int maxReceive, final int maxExtract) {
        super(capacity, maxReceive, maxExtract);
        this.flags = flags;
    }

    /** Keeps the stored energy in the save. */
    public FieldEnergyStorage save() {
        flags.save();
        return this;
    }

    /** Sends the stored energy to the players who see the block. */
    public FieldEnergyStorage toClient() {
        flags.toClient();
        return this;
    }

    /** Shows the stored energy to the menu open on the block. */
    public FieldEnergyStorage toMenu() {
        flags.toMenu();
        return this;
    }

    /** Offers the storage to cables, generators and machines on every side. */
    public FieldEnergyStorage exposed() {
        exposed = true;
        return this;
    }

    boolean isExposed() {
        return exposed;
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        final int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) {
            flags.changed();
        }
        return received;
    }

    @Override
    public int extractEnergy(final int toExtract, final boolean simulate) {
        final int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) {
            flags.changed();
        }
        return extracted;
    }

    /** Spends {@code amount} FE if all of it is there; spends nothing and says so otherwise. */
    public boolean consume(final int amount) {
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        flags.changed();
        return true;
    }

    /** Makes up to {@code amount} FE, as much as there is room for, and says how much. */
    public int generate(final int amount) {
        final int stored = Math.min(amount, capacity - energy);
        if (stored > 0) {
            energy += stored;
            flags.changed();
        }
        return stored;
    }

    /** Sets the stored energy, kept between none and the capacity. */
    public void setEnergyStored(final int amount) {
        final int to = Math.max(0, Math.min(capacity, amount));
        if (to != energy) {
            energy = to;
            flags.changed();
        }
    }

    @Override
    public FieldFlags flags() {
        return flags;
    }

    @Override
    public void write(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put(flags.key(), serializeNBT(registries));
    }

    @Override
    public void read(final CompoundTag tag, final HolderLookup.Provider registries) {
        final Tag stored = tag.get(flags.key());
        if (stored != null) {
            deserializeNBT(registries, stored);
        }
    }

    @Override
    public int menuGet(final int part) {
        return energy;
    }

    @Override
    public void menuSet(final int part, final int to) {
        energy = to;
    }
}
