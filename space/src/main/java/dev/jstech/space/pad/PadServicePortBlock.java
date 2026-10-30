/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.pad;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The Pad Service Port, by the pad, where its lines come up: the deluge's water and its pumps' energy. */
public final class PadServicePortBlock extends Block implements EntityBlock {

    private static final MapCodec<PadServicePortBlock> CODEC = simpleCodec(PadServicePortBlock::new);

    public PadServicePortBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new PadServicePortBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
