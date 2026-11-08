/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.core.heat.IThermalBudget;
import org.jetbrains.annotations.Nullable;

/**
 * A computer's heat as J's Core reads machines' heat (computers/docs/design/temperature.md): the watts its parts make
 * while it runs, what its case, coolers and fans take away, how hot that leaves it, and the share of its clock it
 * keeps. Read live from the computer each time it is asked.
 *
 * @param computer the computer it reads
 */
public record ComputerThermalBudget(AbstractComputerBlockEntity computer) implements IThermalBudget {

    private static final double KELVIN = 273.15;

    /** The budget of {@code computer}, or none for one cooled from outside it, which keeps no heat of its own. */
    public static @Nullable IThermalBudget of(final @Nullable AbstractComputerBlockEntity computer) {
        return computer == null || !computer.computer().cooled() ? null : new ComputerThermalBudget(computer);
    }

    @Override
    public double loadWatts() {
        return computer.isRunning() ? computer.partsWatts() : 0.0;
    }

    @Override
    public double budgetWatts() {
        return computer.thermal().budgetWatts();
    }

    @Override
    public double temperature() {
        final ThermalState thermal = computer.thermal();
        return thermal.known() ? thermal.celsius() + KELVIN : Double.NaN;
    }

    @Override
    public double performance() {
        return computer.heatClock();
    }
}
