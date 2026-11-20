/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Where a cooling plant machine's waters stand in its tanks: each of its ports lets one water alone through, so each
 * water is found by what it is, not by its tank's place, and an output tank holds one water or none.
 */
final class PlantTanks {

    private static final int TICKS = 20;

    private PlantTanks() {
    }

    /** The input tank holding {@code fluid}, or -1 when none does. */
    static int holding(final ProcessingInput input, final Fluid fluid) {
        for (int tank = 0; tank < input.fluids().size(); tank++) {
            if (input.fluids().get(tank).is(fluid)) {
                return tank;
            }
        }
        return -1;
    }

    /** How much the input tank {@code tank} holds, nothing for none. */
    static int amount(final ProcessingInput input, final int tank) {
        return tank < 0 ? 0 : input.fluids().get(tank).getAmount();
    }

    /** How much more {@code fluid} its first {@code outputs} output tanks take: the one holding it, else an empty. */
    static int room(final ProcessingMachineBlockEntity machine, final int outputs, final Fluid fluid) {
        int empty = 0;
        for (int tank = 0; tank < outputs; tank++) {
            final FluidStack held = machine.outputTank(tank).getFluid();
            if (held.is(fluid)) {
                return machine.outputTank(tank).getSpace();
            }
            empty = held.isEmpty() ? machine.outputTank(tank).getCapacity() : empty;
        }
        return empty;
    }

    /** The energy a tick of a second's work on {@code amount} of the most {@code most} takes, at {@code fullWatts}. */
    static int energyPerTick(final int fullWatts, final int amount, final int most) {
        final long whole = (long) most * TICKS;
        return (int) Math.max(1, ((long) fullWatts * amount + whole - 1) / whole);
    }
}
