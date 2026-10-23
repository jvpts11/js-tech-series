/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.blockentity.IFieldPart;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * A block's store of an energy of a mod's own, in one class: what it holds, what it takes and gives at most in a tick,
 * and what a line of another class did to it. A block keeps one, declares it as a part of its fields
 * ({@code fields().part("Energy", store)}) so it is saved and sent, and offers it by {@link EnergyPorts#PORT}.
 *
 * <p>Besides what lines do to it, the block spends what it works with ({@link #spend}) and makes what it generates
 * ({@link #generate}). A store a line one class too high burnt is {@linkplain #damaged() damaged}: it takes and gives
 * nothing until it is {@linkplain #repair() repaired}. One a line of a class below meets is short of voltage for as long
 * as the line is live, and its block does not start.
 */
public class EnergyStore implements IEnergyPort, IFieldPart {

    private final String key;
    private final Energy energy;
    private EnergyClass portClass;
    private final long declaredCapacity;
    private long capacity;
    private final long maxReceive;
    private final long maxExtract;
    private final Runnable onChange;
    private long stored;
    private boolean damaged;
    private long shortSince = NEVER;
    private long overSince = NEVER;
    private Consumer<WrongClass> fault = what -> { };
    private @Nullable EnergyProtection guard;
    private boolean failsSafe;

    private static final String STORED = "stored";
    private static final String DAMAGED = "damaged";
    private static final String CAPACITY = "capacity";
    /* No tick at all: the store was never short. */
    private static final long NEVER = Long.MIN_VALUE / 2;

    /**
     * A store of {@code energy} in {@code portClass}, saved under {@code key}, holding up to {@code capacity} and taking
     * and giving at most {@code maxReceive} and {@code maxExtract} a tick, which tells {@code onChange} when what it
     * holds changes.
     */
    public EnergyStore(final String key, final Energy energy, final EnergyClass portClass, final long capacity,
                       final long maxReceive, final long maxExtract, final Runnable onChange) {
        this.key = Objects.requireNonNull(key, "key");
        this.energy = Objects.requireNonNull(energy, "energy");
        if (!energy.classes().contains(portClass)) {
            throw new IllegalArgumentException("the energy " + energy.id() + " has no class "
                    + Objects.requireNonNull(portClass, "portClass").path());
        }
        if (capacity < 0 || maxReceive < 0 || maxExtract < 0) {
            throw new IllegalArgumentException("a store holds, takes and gives no less than nothing");
        }
        this.portClass = portClass;
        this.declaredCapacity = capacity;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    @Override
    public Energy energy() {
        return this.energy;
    }

    @Override
    public EnergyClass portClass() {
        return this.portClass;
    }

    /** Makes it a port of {@code to}, as a converter set to another class is; its block invalidates its capability. */
    public void setPortClass(final EnergyClass to) {
        if (!this.energy.classes().contains(to)) {
            throw new IllegalArgumentException("the energy " + this.energy.id() + " has no class " + to.path());
        }
        if (to != this.portClass) {
            this.portClass = to;
            this.shortSince = NEVER;
            this.overSince = NEVER;
            this.onChange.run();
        }
    }

    @Override
    public long stored() {
        return this.stored;
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    /** What it was declared to hold, before anything enlarged it. */
    public long declaredCapacity() {
        return this.declaredCapacity;
    }

    /**
     * Makes it hold up to {@code to}, as an upgrade that enlarges its buffer does; what it held past the new capacity
     * is lost. Saved with it, so what it holds survives a reload.
     */
    public void setCapacity(final long to) {
        final long capacityNow = Math.max(0L, to);
        if (capacityNow != this.capacity) {
            this.capacity = capacityNow;
            this.stored = Math.min(this.stored, capacityNow);
            this.onChange.run();
        }
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0 && !this.damaged && !cutByFuse();
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0 && !this.damaged && !cutByFuse();
    }

    /** Puts its machine's fuse between it and a line of another class ({@link EnergyProtection}). */
    public void guardedBy(final EnergyProtection protection) {
        this.guard = Objects.requireNonNull(protection, "protection");
    }

    /** Whether its machine's fuse blew and cuts it off. */
    public boolean cutByFuse() {
        return this.guard != null && this.guard.cut();
    }

    @Override
    public long receive(final long amount, final boolean simulate) {
        if (!canReceive() || amount <= 0) {
            return 0L;
        }
        final long taken = Math.min(Math.min(amount, this.maxReceive), this.capacity - this.stored);
        if (taken > 0 && !simulate) {
            this.stored += taken;
            this.onChange.run();
        }
        return Math.max(0L, taken);
    }

    @Override
    public long extract(final long amount, final boolean simulate) {
        if (!canExtract() || amount <= 0) {
            return 0L;
        }
        final long given = Math.min(Math.min(amount, this.maxExtract), this.stored);
        if (given > 0 && !simulate) {
            this.stored -= given;
            this.onChange.run();
        }
        return given;
    }

    /** Spends up to {@code amount} of what it holds on its block's work; how much it spent. */
    public long spend(final long amount) {
        final long spent = Math.min(Math.max(0L, amount), this.stored);
        if (spent > 0) {
            this.stored -= spent;
            this.onChange.run();
        }
        return spent;
    }

    /** Makes up to {@code amount}, as much as there is room for; how much it made. */
    public long generate(final long amount) {
        final long made = Math.min(Math.max(0L, amount), this.capacity - this.stored);
        if (made > 0) {
            this.stored += made;
            this.onChange.run();
        }
        return made;
    }

    /** Sets what it holds, kept between nothing and its capacity. */
    public void setStored(final long amount) {
        final long to = Math.max(0L, Math.min(this.capacity, amount));
        if (to != this.stored) {
            this.stored = to;
            this.onChange.run();
        }
    }

    /** Whether a line too high burnt it, so it takes and gives nothing until it is repaired. */
    public boolean damaged() {
        return this.damaged;
    }

    /** Repairs it after a burn. */
    public void repair() {
        if (this.damaged) {
            this.damaged = false;
            this.onChange.run();
        }
    }

    /** Whether a live line of a class below met it in tick {@code gameTime} or the one before, so its block waits. */
    public boolean undervoltage(final long gameTime) {
        return gameTime - this.shortSince <= 1;
    }

    /**
     * Whether a live line of a class above met it in tick {@code gameTime} or the one before, where its energy lets a
     * block refuse to start rather than burn.
     */
    public boolean overvoltage(final long gameTime) {
        return gameTime - this.overSince <= 1;
    }

    /**
     * What its block does when a line too high burns it or hits it with an arc flash, besides the store's being damaged:
     * the smoke and sparks, or the block's own ruin ({@link EnergyFaults}).
     */
    public void onFault(final Consumer<WrongClass> what) {
        this.fault = Objects.requireNonNull(what, "what");
    }

    /**
     * Makes it fail safe, as the store of a source a player builds to forget: a line however high only keeps its block
     * from starting, as a line one class too high does where the energy lets a block refuse, and never burns it or
     * flashes over.
     */
    public void failsSafe() {
        this.failsSafe = true;
    }

    @Override
    public void meetWrongClass(final WrongClass met, final EnergyClass line, final long gameTime) {
        if (this.failsSafe && (met == WrongClass.BURNS || met == WrongClass.ARC_FLASH)) {
            this.overSince = gameTime;
            return;
        }
        WrongClass what = met;
        if (this.guard != null && (met == WrongClass.BURNS || met == WrongClass.ARC_FLASH)) {
            if (this.guard.cut()) {
                // A blown fuse left the circuit open: nothing reaches the machine through it.
                return;
            }
            what = this.guard.guard(met, this.energy.steps(this.portClass, line));
        }
        switch (what) {
            case NONE -> {
            }
            case UNDERVOLTAGE -> this.shortSince = gameTime;
            case OVERVOLTAGE -> this.overSince = gameTime;
            case BURNS, ARC_FLASH -> {
                final boolean first = !this.damaged;
                burn();
                if (first || what == WrongClass.ARC_FLASH) {
                    this.fault.accept(what);
                }
            }
        }
    }

    /** The game's FE view of it, through its energy's ratio to FE ({@link FeBridge}). */
    public IEnergyStorage asFe() {
        return FeBridge.view(this);
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = new CompoundTag();
        own.putLong(STORED, this.stored);
        own.putBoolean(DAMAGED, this.damaged);
        if (this.capacity != this.declaredCapacity) {
            own.putLong(CAPACITY, this.capacity);
        }
        tag.put(this.key, own);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (tag.contains(this.key)) {
            final CompoundTag own = tag.getCompound(this.key);
            this.capacity = own.contains(CAPACITY) ? Math.max(0L, own.getLong(CAPACITY)) : this.declaredCapacity;
            this.stored = Math.max(0L, Math.min(this.capacity, own.getLong(STORED)));
            this.damaged = own.getBoolean(DAMAGED);
        }
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        save(tag, registries);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag, registries);
    }

    private void burn() {
        if (!this.damaged) {
            this.damaged = true;
            this.onChange.run();
        }
    }
}
