/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example.machine;

import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.example.ExampleContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The battery: a store of energy that takes and gives as fast on every side, and does nothing else. Everything it is
 * comes from the Core's machine, which saves its energy, offers it to cables and shows it on its screen.
 */
public final class BatteryBlockEntity extends MachineBlockEntity {

    /** What it holds. */
    public static final int CAPACITY = 100_000;
    /** What it takes and gives at most each tick. */
    public static final int RATE = 1_000;

    public BatteryBlockEntity(final BlockPos pos, final BlockState state) {
        super(ExampleContent.BATTERY_ENTITY.get(), pos, state, 0, CAPACITY, RATE, RATE);
    }
}
