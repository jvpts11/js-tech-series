/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

/**
 * What pays a personal-use card's work in energy: the computer the card is in. The work costs more than its store
 * holds at once, so the computer owes it and pays it out of the store as the grid fills it; new work that costs energy
 * waits until it is paid.
 */
public interface IEnergyAccount {

    /** Whether it still owes for earlier work, which new work that costs energy waits behind. */
    boolean owesEnergy();

    /** Owes {@code joules} for work just done. */
    void oweEnergy(long joules);
}
