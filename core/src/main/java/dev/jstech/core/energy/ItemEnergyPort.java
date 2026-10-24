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

/**
 * The energy an item holds, of a mod's own energy and in one class of it, seen as a port ({@link EnergyPorts#ITEM}):
 * what it holds is the item's {@link CoreEnergy#ENERGY} component, so it goes wherever the item goes and is saved with
 * it. A battery is one: a machine's battery slot charges it and draws on it, and a charger fills it. Nothing befalls
 * an item a line meets, since no line meets an item.
 */
public final class ItemEnergyPort implements IEnergyPort {

    private final ItemStack stack;
    private final Energy energy;
    private final EnergyClass portClass;
    private final long capacity;
    private final long maxReceive;
    private final long maxExtract;

    /**
     * @param stack      the item
     * @param energy     the energy it holds
     * @param portClass  the class it is made for
     * @param capacity   the most it holds, in the energy's unit
     * @param maxReceive the most it takes at a time
     * @param maxExtract the most it gives at a time
     */
    public ItemEnergyPort(final ItemStack stack, final Energy energy, final EnergyClass portClass,
                          final long capacity, final long maxReceive, final long maxExtract) {
        this.stack = Objects.requireNonNull(stack, "stack");
        this.energy = Objects.requireNonNull(energy, "energy");
        this.portClass = Objects.requireNonNull(portClass, "portClass");
        if (capacity < 0 || maxReceive < 0 || maxExtract < 0) {
            throw new IllegalArgumentException("an item's energy is never below nothing");
        }
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    @Override
    public Energy energy() {
        return this.energy;
    }

    @Override
    public EnergyClass portClass() {
        return this.portClass;
    }

    @Override
    public long stored() {
        return Math.clamp(this.stack.getOrDefault(CoreEnergy.ENERGY.get(), 0L), 0L, this.capacity);
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public long receive(final long amount, final boolean simulate) {
        if (amount <= 0) {
            return 0L;
        }
        final long held = stored();
        final long taken = Math.min(Math.min(amount, this.maxReceive), this.capacity - held);
        if (!simulate && taken > 0) {
            setStored(held + taken);
        }
        return Math.max(0L, taken);
    }

    @Override
    public long extract(final long amount, final boolean simulate) {
        if (amount <= 0) {
            return 0L;
        }
        final long held = stored();
        final long given = Math.min(Math.min(amount, this.maxExtract), held);
        if (!simulate && given > 0) {
            setStored(held - given);
        }
        return Math.max(0L, given);
    }

    @Override
    public void meetWrongClass(final WrongClass what, final EnergyClass line, final long gameTime) {
        // No line meets an item.
    }

    /** Sets what the item holds, held to what it can. */
    public void setStored(final long amount) {
        this.stack.set(CoreEnergy.ENERGY.get(), Math.clamp(amount, 0L, this.capacity));
    }
}
