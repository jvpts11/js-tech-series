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
 * A part of the WAN Gateway Computer, a big block.
 */
public class WanGatewayPartBlockEntity extends BigBlockPartBlockEntity {

    public WanGatewayPartBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.WAN_GATEWAY_PART_BE.get(), pos, state);
    }
}
