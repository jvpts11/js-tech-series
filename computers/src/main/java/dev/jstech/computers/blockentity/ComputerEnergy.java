/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * What a computer, or a rack of them, draws from the grid through its power supplies: J's Energy in J's Core's low
 * voltage class, as a real supply plugs into a socket, or FE where J's Core lets the series take it. Each tick it runs,
 * it spends the watts its supplies take from the line, its parts' watts over their efficiency ({@code watts drawn =
 * watts of the parts / efficiency}); its store holds a second of what the supplies can give, so the grid keeps it
 * topped up. Without the energy for a tick it cannot run, and a server setting turns the draw off, every computer then
 * running free.
 *
 * <p>Work its cards do costs energy of its own, more than a store holds at once: it is owed, and paid out of the store
 * tick by tick as the grid fills it, a tick's draw always kept back so the machine runs on. New work waits until the
 * debt is paid.
 *
 * <p>A store that runs dry tells how: a line that still brought something within the last second fed it less than it
 * draws, a weak generator or an overloaded grid, which is unstable power; one that brought nothing for longer was cut.
 */
final class ComputerEnergy implements IUpsLine {

    private final EnergyStore store;
    private final Runnable changed;
    /* The part of a joule a tick's draw left over, paid with the next tick's. */
    private double owed;
    /* The joules owed for work its cards did. */
    private long debt;
    /* What the store held once this machine had taken its share, so the next draw sees what the line brought since. */
    private long left;
    /* Whether the store was full as a rack's last tick ended. */
    private boolean leftFull;
    /* How many draws in a row the line brought nothing. */
    private int dryDraws;

    /* The most a store takes from the grid in a tick: more than the supplies of a full rack give in one. */
    private static final long TAKES_A_TICK = 4_000L;
    private static final int TICKS_A_SECOND = 20;
    /* A line that brought something this recently, though too little, wavers rather than being cut. */
    private static final int WAVERS_WITHIN = TICKS_A_SECOND;
    private static final double PERCENT = 100.0;
    private static final String DEBT = "EnergyOwed";

    ComputerEnergy(final Runnable changed) {
        this.changed = changed;
        this.store = new EnergyStore("Energy", JsEnergy.ENERGY, JsEnergy.LOW, 0L, TAKES_A_TICK, 0L, changed);
    }

    /** What {@code partsWatts} take from the line through {@code psu}, in watts: the parts' over its efficiency. */
    static double lineWatts(final int partsWatts, final PsuSpec psu) {
        return partsWatts * PERCENT / psu.efficiencyPercent();
    }

    /** Its store, offered to the grid as the energy port. */
    EnergyStore store() {
        return this.store;
    }

    /** What the game's FE capability sees of it: its store, while the series speaks FE; nothing otherwise. */
    @Nullable
    IEnergyStorage fe() {
        return JsEnergy.speaksFe() ? this.store.asFe() : null;
    }

    /** Sizes the store to a second of {@code watts} from the line: what its supplies can give at most. */
    void sizeFor(final double watts) {
        sizeFor(watts, 0L);
    }

    /** The same, with room for {@code more} joules on top: a tick of what a UPS's battery charges with. */
    void sizeFor(final double watts, final long more) {
        this.store.setCapacity((long) Math.ceil(Math.max(0.0, watts)) + Math.max(0L, more));
    }

    /** Whether there is energy for a tick drawing {@code watts} from the line, or the draw is off. */
    @Override
    public boolean canRun(final double watts) {
        if (!ComputersServerConfig.drawEnergy()) {
            return true;
        }
        return !this.store.damaged() && this.store.stored() >= (long) Math.ceil(watts / TICKS_A_SECOND);
    }

    /**
     * Spends one tick of {@code watts} from the line, and answers whether it had it: what runs on it has lost its
     * power when it did not. Nothing is spent while the draw is off.
     */
    boolean draw(final double watts) {
        if (!ComputersServerConfig.drawEnergy()) {
            return true;
        }
        // Counted only as far as it decides anything, so a store that sits full for years never runs the count over.
        this.dryDraws = this.store.stored() > this.left ? 0 : Math.min(WAVERS_WITHIN, this.dryDraws + 1);
        final double need = watts / TICKS_A_SECOND + this.owed;
        final long whole = (long) need;
        if (this.store.damaged() || this.store.stored() < whole) {
            this.owed = 0.0;
            this.left = this.store.stored();
            return false;
        }
        this.store.spend(whole);
        this.owed = need - whole;
        this.left = this.store.stored();
        return true;
    }

    /**
     * Whether a draw that failed failed on unstable power: the line still brought something within the last second,
     * only less than the machine draws. A line that brought nothing, or a store burnt by the wrong class, is a power
     * loss.
     */
    boolean wavering() {
        return !this.store.damaged() && this.dryDraws < WAVERS_WITHIN;
    }

    @Override
    public long stored() {
        return this.store.stored();
    }

    /** A rack's tick is over: what the store holds now is what the line's next delivery is told from. */
    void settle() {
        this.left = this.store.stored();
        this.leftFull = this.store.capacity() > 0L && this.left >= this.store.capacity();
    }

    /* A store left full had no room for the line to fill, so the line's silence says nothing of it. */
    @Override
    public boolean fed() {
        return this.store.stored() > this.left || this.leftFull;
    }

    /**
     * Puts up to {@code joules} from a UPS's battery into the store, which the line did not bring, so it does not count
     * as the line; answers what it took.
     */
    @Override
    public long fromBattery(final long joules) {
        final long taken = joules <= 0L ? 0L : this.store.receive(joules, false);
        this.left = this.store.stored();
        return taken;
    }

    /**
     * Takes up to {@code most} joules of what the line brought beyond a tick of {@code watts}, for a UPS's battery to
     * charge with; answers what it took. Nothing while the draw is off.
     */
    @Override
    public long spareForCharging(final long most, final double watts) {
        if (!ComputersServerConfig.drawEnergy() || most <= 0L || this.store.damaged()) {
            return 0L;
        }
        final long spare = this.store.stored() - (long) Math.ceil(watts / TICKS_A_SECOND);
        final long taken = Math.min(most, spare);
        if (taken <= 0L) {
            return 0L;
        }
        this.store.spend(taken);
        this.left = this.store.stored();
        return taken;
    }

    /** Owes {@code joules} for work its cards did; nothing while the draw is off. */
    void owe(final long joules) {
        if (joules > 0L && ComputersServerConfig.drawEnergy()) {
            this.debt += joules;
            this.changed.run();
        }
    }

    /** Whether it still owes for work its cards did, which new work waits behind. */
    boolean owing() {
        return this.debt > 0L && ComputersServerConfig.drawEnergy();
    }

    /** What it still owes for work its cards did, in joules. */
    long debt() {
        return this.debt;
    }

    /**
     * Pays what it can of its debt out of the store, keeping back a tick of {@code watts} for the machine to run on.
     * A debt left when the draw is turned off is forgiven.
     */
    void payDebt(final double watts) {
        if (this.debt <= 0L) {
            return;
        }
        if (!ComputersServerConfig.drawEnergy()) {
            this.debt = 0L;
            this.changed.run();
            return;
        }
        final long spare = this.store.stored() - (long) Math.ceil(watts / TICKS_A_SECOND);
        final long paid = Math.min(this.debt, spare);
        if (paid > 0L && !this.store.damaged()) {
            this.store.spend(paid);
            this.debt -= paid;
            this.left = this.store.stored();
            this.changed.run();
        }
    }

    void saveDebt(final CompoundTag tag) {
        if (this.debt > 0L) {
            tag.putLong(DEBT, this.debt);
        }
    }

    void loadDebt(final CompoundTag tag) {
        this.debt = Math.max(0L, tag.getLong(DEBT));
    }
}
