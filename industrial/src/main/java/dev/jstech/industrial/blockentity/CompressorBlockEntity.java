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
 * The Compressor: presses its input into the recipe's result, drawing 3 kW at low voltage over the recipe's
 * processing time (Tier 1: 120 ticks, ingot to plate).
 */
public class CompressorBlockEntity extends IndustrialProcessingMachine {

    /** A T1 machine on low voltage, drawing 3 kW as it works. */
    public static final MachineRating RATING = MachineRating.of(IndustrialTier.T1, JsEnergy.LOW, 3_000);

    public CompressorBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.COMPRESSOR_BE.get(), pos, state, RATING, Layout.items(1, 1));
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        return byKind(level, IndustrialModule.COMPRESSING, input);
    }
}
