/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.power.UpsRating;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import java.util.function.Consumer;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Where the line comes into an MF PowerGuard (computers/docs/design/power.md): a store in J's Core's low voltage class
 * holding a second of the most it carries and a tick of what its battery charges with, which the computers standing
 * against it are fed from.
 *
 * <p>It isolates them from the grid as a real double-conversion UPS does: a line one class too high, or a surge, does
 * not burn it, it only takes nothing from that line, so the battery carries the load. Two classes above, not even a UPS
 * holds, and it meets the line as any block of the series does.
 */
final class UpsInlet implements IUpsLine {

    private final EnergyStore store;
    /* What the store held as the UPS's last tick ended, so the next one sees what the line brought since. */
    private long left;
    /* Whether it was full then. */
    private boolean leftFull;

    private static final int TICKS_A_SECOND = 20;

    UpsInlet(final UpsRating rating, final Runnable changed, final Consumer<WrongClass> struck) {
        this.store = new Isolating(rating.carryWatts() + rating.chargePerTick(), changed);
        this.store.onFault(struck);
    }

    /** Its store, offered to the grid as the energy port on the faces the cable plugs into. */
    EnergyStore store() {
        return this.store;
    }

    /** What the game's FE capability sees of it: its store, while the series speaks FE; nothing otherwise. */
    @Nullable
    IEnergyStorage fe() {
        return JsEnergy.speaksFe() ? this.store.asFe() : null;
    }

    /** Feeds {@code to}, a computer's energy port, as much of what it holds as the computer takes. */
    void feed(final IEnergyPort to) {
        final long given = to.receive(this.store.stored(), false);
        if (given > 0L) {
            this.store.spend(given);
        }
    }

    @Override
    public boolean canRun(final double watts) {
        return !ComputersServerConfig.drawEnergy()
                || !this.store.damaged() && this.store.stored() >= (long) Math.ceil(watts / TICKS_A_SECOND);
    }

    @Override
    public long stored() {
        return this.store.stored();
    }

    /* A store left full had no room for the line to fill, so the line's silence says nothing of it. */
    @Override
    public boolean fed() {
        return this.store.stored() > this.left || this.leftFull;
    }

    /** The UPS's tick is over: what the store holds now is what the line's next delivery is told from. */
    void settle() {
        this.left = this.store.stored();
        this.leftFull = this.store.capacity() > 0L && this.left >= this.store.capacity();
    }

    @Override
    public long fromBattery(final long joules) {
        return this.store.damaged() ? 0L : this.store.generate(joules);
    }

    @Override
    public long spareForCharging(final long most, final double watts) {
        if (!ComputersServerConfig.drawEnergy() || most <= 0L || this.store.damaged()) {
            return 0L;
        }
        final long spare = this.store.stored() - (long) Math.ceil(watts / TICKS_A_SECOND);
        return spare <= 0L ? 0L : this.store.spend(Math.min(most, spare));
    }

    /* The store that refuses a line one class too high, or a surge, rather than burning. */
    private static final class Isolating extends EnergyStore {

        Isolating(final long second, final Runnable changed) {
            super("Energy", JsEnergy.ENERGY, JsEnergy.LOW, second, second, 0L, changed);
        }

        @Override
        public void meetWrongClass(final WrongClass met, final EnergyClass line, final long gameTime) {
            final boolean held = met == WrongClass.BURNS && energy().steps(portClass(), line) <= 1;
            super.meetWrongClass(held ? WrongClass.OVERVOLTAGE : met, line, gameTime);
        }
    }
}
