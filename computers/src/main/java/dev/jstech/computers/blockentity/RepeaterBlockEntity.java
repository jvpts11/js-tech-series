/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.DataWires;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Behind a repeater: a place of the data grid in the lane of each line the repeater carries, joining the wires of that
 * line on its faces and no other line's, and ending each run that reaches it, so the next one counts afresh.
 */
public class RepeaterBlockEntity extends SyncedBlockEntity {

    public RepeaterBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.REPEATER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            DataWires.placeRepeater(serverLevel, worldPosition);
        }
    }
}
