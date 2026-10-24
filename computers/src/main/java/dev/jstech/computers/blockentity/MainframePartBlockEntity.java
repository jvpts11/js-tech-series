/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.bigblock.BigBlockPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A part of the Mainframe, a big block.
 */
public class MainframePartBlockEntity extends BigBlockPartBlockEntity {

    public MainframePartBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.MAINFRAME_PART_BE.get(), pos, state);
    }
}
