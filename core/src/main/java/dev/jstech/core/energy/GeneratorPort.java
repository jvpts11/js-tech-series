/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import java.util.Objects;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The port of a generator that follows its load ({@link LoadFollower}): it gives the grid what the grid draws, up to
 * what its output may reach this tick, and holds nothing. Its block settles each tick and burns fuel for what it made:
 *
 * <pre>{@code
 * long made = generator.follower().settle();
 * burnFuelFor(made);
 * }</pre>
 *
 * A generator of FE offers {@link #asFe()} by the game's energy capability; one of a mod's own energy offers itself by
 * {@link EnergyPorts#PORT}.
 */
public final class GeneratorPort implements IEnergyPort {

    private final Energy energy;
    private final EnergyClass portClass;
    private final LoadFollower follower;
    private boolean running = true;

    /** The port of a generator of {@code energy} in {@code portClass}, its output following {@code follower}. */
    public GeneratorPort(final Energy energy, final EnergyClass portClass, final LoadFollower follower) {
        this.energy = Objects.requireNonNull(energy, "energy");
        this.portClass = Objects.requireNonNull(portClass, "portClass");
        this.follower = Objects.requireNonNull(follower, "follower");
        if (!energy.classes().contains(portClass)) {
            throw new IllegalArgumentException("the energy " + energy.id() + " has no class " + portClass.path());
        }
    }

    /** How its output follows the load. */
    public LoadFollower follower() {
        return this.follower;
    }

    /** Whether it runs: one out of fuel gives nothing, and starts again from nothing. */
    public boolean running() {
        return this.running;
    }

    /** Runs it or stops it. */
    public void setRunning(final boolean run) {
        if (this.running && !run) {
            this.follower.stop();
        }
        this.running = run;
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
        return this.running ? this.follower.available() : 0L;
    }

    @Override
    public long capacity() {
        return this.follower.maximum();
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return this.running;
    }

    @Override
    public long receive(final long amount, final boolean simulate) {
        return 0L;
    }

    @Override
    public long extract(final long amount, final boolean simulate) {
        return this.running ? this.follower.draw(amount, simulate) : 0L;
    }

    /** A generator burnt by a line too high stops until its block repairs it and runs it again. */
    @Override
    public void meetWrongClass(final WrongClass what, final EnergyClass line, final long gameTime) {
        if (what == WrongClass.BURNS || what == WrongClass.ARC_FLASH) {
            setRunning(false);
        }
    }

    /** The game's FE view of it ({@link FeBridge}). */
    public IEnergyStorage asFe() {
        return FeBridge.view(this);
    }
}
