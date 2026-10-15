/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

/**
 * Where a block takes or gives an energy of a mod's own, in one class of it, as the Core's energy capability offers it
 * ({@link EnergyPorts#PORT}): what it holds, what it takes and gives, and what befalls it when a live line of another
 * class meets it. Amounts are in the energy's own unit. FE is the game's own energy capability, not this.
 */
public interface IEnergyPort {

    /** The energy it takes and gives. */
    Energy energy();

    /** The class of line it is made for. */
    EnergyClass portClass();

    /** How much it holds. */
    long stored();

    /** How much it holds at most. */
    long capacity();

    /** Whether it takes energy in. */
    boolean canReceive();

    /** Whether it gives energy out. */
    boolean canExtract();

    /** Takes in up to {@code amount}; how much it took, or would when simulated. */
    long receive(long amount, boolean simulate);

    /** Gives out up to {@code amount}; how much it gave, or would when simulated. */
    long extract(long amount, boolean simulate);

    /**
     * A live line of {@code line} meets it, in tick {@code gameTime}, and {@code what} befalls it: told each tick the
     * line is live, so a port short of voltage knows it still is.
     */
    void meetWrongClass(WrongClass what, EnergyClass line, long gameTime);
}
