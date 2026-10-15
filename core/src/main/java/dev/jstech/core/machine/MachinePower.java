/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.energy.EnergyStore;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's energy at work, whichever it runs on: its FE store, or its port of a class with its fuse and battery.
 * The machine spends through it and reads its faults through it, the same for both.
 */
public final class MachinePower {

    private final Energy energy;
    private final @Nullable FieldEnergyStorage fe;
    private final @Nullable EnergyStore store;
    private final @Nullable EnergyProtection protection;
    private final long builtCapacity;

    private MachinePower(final Energy energy, final @Nullable FieldEnergyStorage fe, final @Nullable EnergyStore store,
                         final @Nullable EnergyProtection protection) {
        this.energy = energy;
        this.fe = fe;
        this.store = store;
        this.protection = protection;
        this.builtCapacity = fe != null ? fe.getMaxEnergyStored() : Objects.requireNonNull(store).declaredCapacity();
    }

    /** A machine's FE store at work. */
    public static MachinePower fe(final FieldEnergyStorage fe) {
        return new MachinePower(Energies.FE, Objects.requireNonNull(fe, "fe"), null, null);
    }

    /** A machine's port of a class at work, with the fuse and the battery that guard it. */
    public static MachinePower port(final EnergyStore store, final EnergyProtection protection) {
        return new MachinePower(store.energy(), null, Objects.requireNonNull(store, "store"),
                Objects.requireNonNull(protection, "protection"));
    }

    /** The energy it runs on. */
    public Energy energy() {
        return this.energy;
    }

    /** The class of its port; none for FE. */
    public Optional<EnergyClass> portClass() {
        return this.store == null ? Optional.empty() : Optional.of(this.store.portClass());
    }

    /** What it holds. */
    public long stored() {
        return this.fe != null ? this.fe.getEnergyStored() : this.store.stored();
    }

    /** The most it holds. */
    public long capacity() {
        return this.fe != null ? this.fe.getMaxEnergyStored() : this.store.capacity();
    }

    /** The most it holds as the machine was built, before upgrades enlarged its buffer. */
    public long builtCapacity() {
        return this.builtCapacity;
    }

    /** Holds as much as {@code effect} makes of what it held as built. */
    public void enlarge(final UpgradeEffect effect) {
        final long capacity = effect.storage(this.builtCapacity);
        if (this.fe != null) {
            this.fe.setCapacity((int) Math.min(Integer.MAX_VALUE, capacity));
        } else {
            this.store.setCapacity(capacity);
        }
    }

    /** Spends up to {@code amount} on the machine's work; how much it spent. */
    public long spend(final long amount) {
        if (amount <= 0) {
            return 0L;
        }
        return this.fe != null ? this.fe.spendUpTo((int) Math.min(Integer.MAX_VALUE, amount))
                : this.store.spend(amount);
    }

    /** Makes up to {@code amount}, as a generator does; how much it made. */
    public long generate(final long amount) {
        if (amount <= 0) {
            return 0L;
        }
        return this.fe != null ? this.fe.generate((int) Math.min(Integer.MAX_VALUE, amount))
                : this.store.generate(amount);
    }

    /** Whether a live line of a class below met its port in tick {@code gameTime} or the one before. */
    public boolean undervoltage(final long gameTime) {
        return this.store != null && this.store.undervoltage(gameTime);
    }

    /** Whether its fuse blew and cuts it off. */
    public boolean fuseBlown() {
        return this.protection != null && this.protection.cut();
    }

    /** Whether a line too high burnt its port, which takes nothing until the machine is repaired. */
    public boolean burnt() {
        return this.store != null && this.store.damaged();
    }

    /** Mends a burnt port, as a repaired machine's is. */
    public void mend() {
        if (this.store != null) {
            this.store.repair();
        }
    }

    /** One tick of its battery: charged while the line keeps the store full, given from when it runs low. */
    public void tick() {
        if (this.protection != null) {
            this.protection.tick(this.store);
        }
    }

    /** Its FE store; none for a machine that runs on a port of a class. */
    public @Nullable FieldEnergyStorage feStore() {
        return this.fe;
    }

    /** Its port of a class; none for a machine that runs on FE. */
    public @Nullable EnergyStore portStore() {
        return this.store;
    }

    /** The fuse and the battery of its Protection tab; none for a machine that runs on FE. */
    public @Nullable EnergyProtection protection() {
        return this.protection;
    }
}
