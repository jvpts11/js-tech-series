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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What a block of a formed structure merged into its controller's model keeps: the block it was, given back when the
 * structure comes apart, and the controller it belongs to. The block it was is sent to the players who see it, so it
 * breaks, sounds and shows its particles as that block would.
 */
public final class FormedPartBlockEntity extends SyncedBlockEntity {

    private final ValueField<BlockState> original;
    private final ValueField<BlockPos> controller;

    public FormedPartBlockEntity(final BlockPos pos, final BlockState state) {
        super(FormedParts.ENTITY.get(), pos, state);
        this.original = fields().value("Original", BlockState.CODEC, Blocks.AIR.defaultBlockState()).save()
                .toClient();
        this.controller = fields().nullable("Controller", BlockPos.CODEC).save();
    }

    /** The block it was before its structure formed. */
    public BlockState original() {
        return this.original.get();
    }

    /** The controller of the structure it is part of, or none for one not yet told. */
    public @Nullable BlockPos controller() {
        return this.controller.get();
    }

    /* Told what it was and whose it is, as it merges. */
    void merged(final BlockState was, final BlockPos owner) {
        this.original.set(was);
        this.controller.set(owner.immutable());
    }
}
