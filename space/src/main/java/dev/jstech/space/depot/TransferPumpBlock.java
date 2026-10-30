/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.depot;

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
 * The Transfer Pump, on the ground against a leg of a propellant depot: it finds the depot and forms it, holds its
 * sphere's liquid, and is where the pipes to the pad's service port take it from.
 */
public final class TransferPumpBlock extends Block implements EntityBlock {

    private static final MapCodec<TransferPumpBlock> CODEC = simpleCodec(TransferPumpBlock::new);

    public TransferPumpBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new TransferPumpBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != DepotBlocks.PUMP_ENTITY.get()) {
            return null;
        }
        return (ticking, pos, at, entity) -> ((TransferPumpBlockEntity) entity).tick();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
