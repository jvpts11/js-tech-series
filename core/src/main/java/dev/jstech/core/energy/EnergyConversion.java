/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * How a converter works, either way between FE and a mod's energy: each tick it moves up to its limit, counted in FE,
 * from one side to the other, by the energy's ratio to FE. It rounds down, so it never makes energy: what arrives is
 * worth no more than what left.
 */
public final class EnergyConversion {

    private EnergyConversion() {
    }

    /**
     * Moves up to {@code limitFe} worth of energy between {@code fe} and {@code port}, {@code way} round; how much,
     * counted in FE, crossed.
     */
    public static long convert(final IEnergyStorage fe, final IEnergyPort port, final Way way,
                               final long limitFe) {
        if (limitFe <= 0) {
            return 0L;
        }
        final int most = (int) Math.min(Integer.MAX_VALUE, limitFe);
        return switch (way) {
            case FE_TO_ENERGY -> feToEnergy(fe, port, most);
            case ENERGY_TO_FE -> energyToFe(port, fe, most);
        };
    }

    private static long feToEnergy(final IEnergyStorage fe, final IEnergyPort port, final int most) {
        final Energy energy = port.energy();
        final int offered = fe.extractEnergy(most, true);
        // Paid in whole FE for what the port has room for, and given what those FE are worth, rounded down.
        final long units = port.receive(energy.fromFe(offered), true);
        final long paid = energy.toFe(units);
        final long bought = energy.fromFe(paid);
        if (paid <= 0 || bought <= 0) {
            return 0L;
        }
        final int taken = fe.extractEnergy((int) paid, false);
        port.receive(energy.fromFe(taken), false);
        return taken;
    }

    private static long energyToFe(final IEnergyPort port, final IEnergyStorage fe, final int most) {
        final Energy energy = port.energy();
        final long available = port.extract(energy.fromFe(most), true);
        final int accepted = fe.receiveEnergy((int) Math.min(Integer.MAX_VALUE, energy.toFe(available)), true);
        final long units = energy.fromFe(accepted);
        final long worth = energy.toFe(units);
        if (units <= 0 || worth <= 0) {
            return 0L;
        }
        final long left = port.extract(units, false);
        return fe.receiveEnergy((int) Math.min(Integer.MAX_VALUE, energy.toFe(left)), false);
    }

    /** Which way a converter works. */
    public enum Way {
        /** From FE into the mod's energy. */
        FE_TO_ENERGY,
        /** From the mod's energy into FE. */
        ENERGY_TO_FE
    }
}
