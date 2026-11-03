/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A barrel of a starship's workshop or hold, standing on the floor against the hull's wall where its part's model draws
 * it: it keeps things as a barrel does.
 */
public final class ShipBarrelBlock extends ShipContainerBlock {

    public ShipBarrelBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ShipBarrelEntity(pos, state);
    }
}
