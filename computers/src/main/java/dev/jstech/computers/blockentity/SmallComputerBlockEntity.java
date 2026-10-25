/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of a small computer another mod brings: a Personal Computer in all but what its entry says, the
 * machine it is among them, so the programs made for that machine install on it and the personal-use cards only on a
 * Personal Computer. One block entity type serves every mod's small computers.
 */
public class SmallComputerBlockEntity extends PersonalComputerBlockEntity {

    public SmallComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.SMALL_COMPUTER_BE.get(), pos, state);
    }
}
