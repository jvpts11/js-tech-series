/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the non-controller parts of a multiblock structure. Each part remembers its controller's position
 * so a break or an interaction on the part can reach the controller, declared once here as a saved field for every
 * structure that uses it.
 */
public abstract class MultiblockPartBlockEntity extends SyncedBlockEntity {

    private final ValueField<BlockPos> controller = fields().nullable("Controller", BlockPos.CODEC).save();

    protected MultiblockPartBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                        final BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public BlockPos controllerPos() {
        return controller.get();
    }

    public void setController(final BlockPos pos) {
        controller.set(pos.immutable());
    }
}
