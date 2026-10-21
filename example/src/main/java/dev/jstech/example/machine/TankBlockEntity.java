/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example.machine;

import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.example.ExampleContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The tank's contents: one tank of sixteen buckets, declared as a field of the Core's block entity, which saves it,
 * sends it to the players who see the tank, and offers it to pipes and buckets on every side.
 */
public final class TankBlockEntity extends SyncedBlockEntity {

    private final FieldFluidTank tank = fields().fluid("Tank", CAPACITY).save().toClient().exposed();

    /** What it holds, in millibuckets. */
    public static final int CAPACITY = 16_000;

    public TankBlockEntity(final BlockPos pos, final BlockState state) {
        super(ExampleContent.TANK_ENTITY.get(), pos, state);
    }

    /** The tank. */
    public FieldFluidTank tank() {
        return tank;
    }
}
