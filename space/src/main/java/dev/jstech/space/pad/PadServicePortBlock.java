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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Pad Service Port, by the pad, where its lines come up: the deluge's water, its pumps' energy and the propellant
 * that fuels the rocket on the pad.
 */
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
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != PadBlocks.PORT_ENTITY.get()) {
            return null;
        }
        return (ticking, pos, at, entity) -> ((PadServicePortBlockEntity) entity).tick();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
