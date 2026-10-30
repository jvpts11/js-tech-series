/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crawler;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A piece of a crawler-transporter: a block of its kit on its own, and once the crawler round its engine is whole,
 * formed, showing nothing of itself while the engine draws the crawler whole. Used, a formed cab starts the crawler's
 * trip along its crawlerway.
 */
public class CrawlerPieceBlock extends Block {

    private final CrawlerLayout.Kind kind;
    private final MapCodec<CrawlerPieceBlock> codec;

    /** Whether it is part of a whole crawler. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public CrawlerPieceBlock(final Properties properties, final CrawlerLayout.Kind kind) {
        // Formed, it shows nothing, so it hides nothing of the blocks round it either.
        super(properties.noOcclusion());
        this.kind = kind;
        this.codec = simpleCodec(made -> new CrawlerPieceBlock(made, kind));
        registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    /** The kind of piece it is. */
    public CrawlerLayout.Kind kind() {
        return this.kind;
    }

    /** The engine of the whole crawler the cab at {@code cab} is one of; none when it is not in one. */
    public static Optional<BlockPos> engineOf(final Level level, final BlockPos cab) {
        for (final Direction along : new Direction[] {Direction.EAST, Direction.SOUTH}) {
            for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(BlockPos.ZERO, along)) {
                if (spot.kind() != CrawlerLayout.Kind.CAB) {
                    continue;
                }
                final BlockPos engine = cab.subtract(spot.at());
                if (level.getBlockState(engine).is(CrawlerBlocks.CRAWLER_ENGINE.get())
                        && CrawlerLayout.whole(level, engine, along)) {
                    return Optional.of(engine);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return this.codec;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (this.kind != CrawlerLayout.Kind.CAB) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer driver) {
            final Optional<BlockPos> engine = engineOf(level, pos);
            if (engine.isPresent() && level.getBlockEntity(engine.get()) instanceof CrawlerEngineBlockEntity crawler) {
                crawler.start().ifPresent(why -> driver.displayClientMessage(GameText.component(why), true));
            } else {
                driver.displayClientMessage(GameText.component(CrawlerTexts.NOT_WHOLE), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
