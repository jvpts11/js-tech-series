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
 * The second block of a Mining Computer two blocks wide, a big block.
 */
public class MiningComputerPartBlockEntity extends BigBlockPartBlockEntity {

    public MiningComputerPartBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.MINING_COMPUTER_PART_BE.get(), pos, state);
    }
}
