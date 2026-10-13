/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.industrial.IndustrialModule;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Compressor: presses its input into the recipe's result, spending {@value #FE_PER_TICK} FE per tick over the
 * recipe's processing time (Tier 1: 120 ticks, ingot to plate).
 */
public class CompressorBlockEntity extends ProcessingMachineBlockEntity {

    public static final int FE_PER_TICK = 30;
    private static final int ENERGY_CAPACITY = 12_000;
    private static final int ENERGY_MAX_RECEIVE = 600;

    public CompressorBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.COMPRESSOR_BE.get(), pos, state, Layout.items(1, 1), ENERGY_CAPACITY,
                ENERGY_MAX_RECEIVE, FE_PER_TICK);
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        return byKind(level, IndustrialModule.COMPRESSING, input);
    }
}
