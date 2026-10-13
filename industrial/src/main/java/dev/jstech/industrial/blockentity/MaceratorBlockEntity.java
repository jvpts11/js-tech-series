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
 * The Macerator: grinds its input into the recipe's result, spending {@value #FE_PER_TICK} FE per tick over the
 * recipe's processing time (Tier 1: 200 ticks, ore to two dusts).
 */
public class MaceratorBlockEntity extends ProcessingMachineBlockEntity {

    public static final int FE_PER_TICK = 40;
    private static final int ENERGY_CAPACITY = 16_000;
    private static final int ENERGY_MAX_RECEIVE = 1_000;

    public MaceratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.MACERATOR_BE.get(), pos, state, Layout.items(1, 1), ENERGY_CAPACITY,
                ENERGY_MAX_RECEIVE, FE_PER_TICK);
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        return byKind(level, IndustrialModule.MACERATING, input);
    }
}
