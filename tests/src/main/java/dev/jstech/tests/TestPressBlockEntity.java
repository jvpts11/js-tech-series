/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The test press at work: the recipes of its kind, the Core's way. */
public final class TestPressBlockEntity extends ProcessingMachineBlockEntity {

    private static final int ENERGY_CAPACITY = 100_000;

    public TestPressBlockEntity(final BlockPos pos, final BlockState state) {
        super(TestPress.BLOCK_ENTITY.get(), pos, state, TestPress.LAYOUT, ENERGY_CAPACITY, ENERGY_CAPACITY,
                TestPress.ENERGY_PER_TICK);
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        return byKind(level, TestPress.PRESSING, input);
    }
}
