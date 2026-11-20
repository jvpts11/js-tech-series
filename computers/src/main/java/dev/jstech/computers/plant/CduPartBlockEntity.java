/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import dev.jstech.core.bigblock.BigBlockPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A part of the CDU, a big block, which knows its origin.
 */
public class CduPartBlockEntity extends BigBlockPartBlockEntity {

    public CduPartBlockEntity(final BlockPos pos, final BlockState state) {
        super(CoolingPlant.CDU_PART_BE.get(), pos, state);
    }
}
