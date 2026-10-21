/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example.machine;

import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.example.ExampleContent;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The crusher: one slot in, one out, and the crushing recipes to say what one makes of the other. The Core's
 * processing machine does the rest: it takes the input, spends the energy over the recipe's time, puts the output
 * away, waits when the output is full, and shows its progress on its screen.
 */
public final class CrusherBlockEntity extends ProcessingMachineBlockEntity {

    /** What it holds. */
    public static final int CAPACITY = 16_000;
    /** What it takes at most each tick. */
    public static final int TAKES = 1_000;
    /** What it spends each tick on a recipe that names no energy of its own. */
    public static final int SPENDS = 20;

    public CrusherBlockEntity(final BlockPos pos, final BlockState state) {
        super(ExampleContent.CRUSHER_ENTITY.get(), pos, state, Layout.items(1, 1),
                MachineEnergy.fe(CAPACITY, TAKES, 0), SPENDS);
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        return byKind(level, ExampleContent.CRUSHING, input);
    }
}
