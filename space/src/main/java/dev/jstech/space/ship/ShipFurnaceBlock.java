/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The workshop's furnace, standing on the floor against the hull's wall where its part's model draws it: it smelts as
 * the game's furnace does, and gives light while it burns.
 */
public final class ShipFurnaceBlock extends ShipFixtureBlock {

    /** Whether it burns, as the game's furnace's own state says. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** How much light it gives while it burns, as the game's furnace does. */
    public static final int LIGHT = 13;

    public ShipFurnaceBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ShipFurnaceEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != ShipKitBlocks.SHIP_FURNACE_ENTITY.get()) {
            return null;
        }
        return (at, pos, now, entity) -> AbstractFurnaceBlockEntity.serverTick(at, pos, now,
                (AbstractFurnaceBlockEntity) entity);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShipFurnaceEntity furnace) {
            player.openMenu(furnace);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /* Broken, it gives back what it holds and the experience its smelting earned, as the game's furnace does. */
    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState now,
                            final boolean moved) {
        if (!state.is(now.getBlock()) && level.getBlockEntity(pos) instanceof ShipFurnaceEntity furnace) {
            if (level instanceof ServerLevel server) {
                Containers.dropContents(level, pos, furnace);
                furnace.getRecipesToAwardAndPopExperience(server, Vec3.atCenterOf(pos));
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, now, moved);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }
}
