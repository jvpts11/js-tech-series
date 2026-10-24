/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.machine.IndustrialProcessingMachine;
import dev.jstech.industrial.machine.MachineRating;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Macerator: grinds its input into the recipe's result, drawing 4 kW at low voltage over the recipe's processing
 * time (Tier 1: 200 ticks, ore to two dusts), with room besides for the byproducts a mineral gives as it is ground.
 */
public class MaceratorBlockEntity extends IndustrialProcessingMachine {

    /** A T1 machine on low voltage, drawing 4 kW as it works. */
    public static final MachineRating RATING = MachineRating.of(IndustrialTier.T1, JsEnergy.LOW, 4_000);
    /** Its output slots: the dusts of what an ore is mined for, and its byproducts. */
    public static final int OUTPUTS = 6;

    public MaceratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.MACERATOR_BE.get(), pos, state, RATING, Layout.items(1, OUTPUTS));
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        return byKind(level, IndustrialModule.MACERATING, input);
    }
}
