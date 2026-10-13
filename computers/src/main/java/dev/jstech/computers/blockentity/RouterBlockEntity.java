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
 * Behind a router: a whole block of the data grid that joins every data wire its faces take, so the access line and
 * the backbone it touches, or the fibre runs an optical router turns, are one network.
 */
public class RouterBlockEntity extends SyncedBlockEntity {

    public RouterBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.ROUTER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            DataWires.placeRouter(serverLevel, worldPosition);
        }
    }
}
