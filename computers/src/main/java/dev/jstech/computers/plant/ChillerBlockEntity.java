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
 * The Chiller at work (computers/docs/design/temperature.md): each second, on its racks' side, it cools the Hot Water
 * come back from the CDUs into Cold Water, up to {@link #MOST_A_SECOND} millibuckets (120 kW), for its compressor's
 * {@link #COMPRESSOR_WATTS} at full load; on its tower's side it turns {@link #TOWER_HEATED} millibuckets of the
 * tower's Cold Water into Hot Water for every {@link #RACKS_CHILLED} it cools, the racks' heat and its compressor's own
 * (150 kW at full load).
 */
public class ChillerBlockEntity extends ChainMachineBlockEntity {

    /** The most Hot Water it cools a second on its racks' side, in millibuckets: 120 kW of heat. */
    public static final int MOST_A_SECOND = 120;
    /** What its compressor draws at full load, in watts. */
    public static final int COMPRESSOR_WATTS = 30_000;
    /** The tower's water it heats for every {@link #RACKS_CHILLED} of the racks' it cools: 150 kW to 120. */
    public static final int TOWER_HEATED = 25;
    public static final int RACKS_CHILLED = 20;
    private static final int TICKS = 20;
    private static final int OUTPUTS = 2;

    public ChillerBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
    }

    /* A second's chilling, as far as the racks' hot water, the tower's cold water and both outlets' room allow. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        final int racks = PlantTanks.holding(input, CoolingWater.HOT.get());
        // Plain water at its COLD inlet is taken as Cold Water: how its loop is first filled.
        final int cold = PlantTanks.holding(input, CoolingWater.COLD.get());
        final int tower = cold >= 0 ? cold : PlantTanks.holding(input, Fluids.WATER);
        final int towerCold = PlantTanks.amount(input, tower);
        final int towerRoom = PlantTanks.room(this, OUTPUTS, CoolingWater.HOT.get());
        final int chilled = Math.min(Math.min(MOST_A_SECOND, PlantTanks.amount(input, racks)),
                Math.min(PlantTanks.room(this, OUTPUTS, CoolingWater.COLD.get()),
                        Math.min(towerCold, towerRoom) * RACKS_CHILLED / TOWER_HEATED));
        if (chilled <= 0) {
            return Optional.empty();
        }
        final int rejected = towerShare(chilled);
        return run(level, input, new Processing(List.of(), List.of(new Take(racks, chilled), new Take(tower,
                rejected)), List.of(), List.of(new FluidStack(CoolingWater.COLD.get(), chilled),
                new FluidStack(CoolingWater.HOT.get(), rejected)), TICKS,
                PlantTanks.energyPerTick(COMPRESSOR_WATTS, chilled, MOST_A_SECOND), List.of(), List.of(),
                List.of(), List.of()), null);
    }

    /** The tower's water it heats for {@code chilled} millibuckets it cools, rounded up. */
    public static int towerShare(final int chilled) {
        return (chilled * TOWER_HEATED + RACKS_CHILLED - 1) / RACKS_CHILLED;
    }
}
