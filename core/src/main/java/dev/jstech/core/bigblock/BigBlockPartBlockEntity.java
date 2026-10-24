/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.bigblock;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The shared base of a big block's parts: each part remembers its origin, so a break or a use on the part reaches the
 * block entity at the origin.
 */
public abstract class BigBlockPartBlockEntity extends SyncedBlockEntity {

    private final ValueField<BlockPos> origin = fields().nullable("Origin", BlockPos.CODEC).save();

    protected BigBlockPartBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /** Where its big block's origin is, or none for a part not yet bound. */
    @Nullable
    public BlockPos originPos() {
        return origin.get();
    }

    public void setOrigin(final BlockPos pos) {
        origin.set(pos.immutable());
    }
}
