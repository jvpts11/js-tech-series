/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.core.machine.ProcessingInput;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Cooling Tower at work (computers/docs/design/temperature.md): each second it cools as much of the Hot Water
 * come back from the Chiller as it can, up to {@link #MOST_A_SECOND} millibuckets (200 kW), into Cold Water for the
 * Chiller, evaporating a millibucket of its make-up water, plain water at its FILL inlet, for every
 * {@link #COOLED_A_MAKEUP} it cools; its fan draws its full {@link #FAN_WATTS} only at full load.
 */
public class CoolingTowerBlockEntity extends ChainMachineBlockEntity {

    /** The most Hot Water it cools a second, in millibuckets: 200 kW of heat. */
    public static final int MOST_A_SECOND = 200;
    /** How many millibuckets it cools for each of its make-up water it evaporates. */
    public static final int COOLED_A_MAKEUP = 10;
    /** What its fan draws at full load, in watts; an estimate. */
    public static final int FAN_WATTS = 2_000;
    private static final int TICKS = 20;
    private static final int OUTPUTS = 1;

    public CoolingTowerBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
    }

    /* A second's cooling: what hot water there is, as far as the make-up water and the cold water's room allow. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        final int hot = PlantTanks.holding(input, CoolingWater.HOT.get());
        final int fill = PlantTanks.holding(input, Fluids.WATER);
        final int cooled = Math.min(Math.min(MOST_A_SECOND, PlantTanks.amount(input, hot)),
                Math.min(PlantTanks.amount(input, fill) * COOLED_A_MAKEUP,
                        PlantTanks.room(this, OUTPUTS, CoolingWater.COLD.get())));
        if (cooled <= 0) {
            return Optional.empty();
        }
        final int makeUp = (cooled + COOLED_A_MAKEUP - 1) / COOLED_A_MAKEUP;
        return run(level, input, new Processing(List.of(), List.of(new Take(hot, cooled), new Take(fill, makeUp)),
                List.of(), List.of(new FluidStack(CoolingWater.COLD.get(), cooled)), TICKS,
                PlantTanks.energyPerTick(FAN_WATTS, cooled, MOST_A_SECOND), List.of(), List.of(), List.of(),
                List.of()), null);
    }
}
