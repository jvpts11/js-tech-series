/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crawler;

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
 * The Crawler Engine, in the middle of a crawler-transporter's hull: it finds the crawler whole round it and forms it,
 * draws it whole, and drives it along its crawlerway when a cab is used.
 */
public final class CrawlerEngineBlock extends CrawlerPieceBlock implements EntityBlock {

    private static final MapCodec<CrawlerEngineBlock> CODEC = simpleCodec(CrawlerEngineBlock::new);

    public CrawlerEngineBlock(final Properties properties) {
        super(properties, CrawlerLayout.Kind.ENGINE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CrawlerEngineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != CrawlerBlocks.ENGINE_ENTITY.get()) {
            return null;
        }
        return (ticking, pos, at, entity) -> ((CrawlerEngineBlockEntity) entity).tick();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
