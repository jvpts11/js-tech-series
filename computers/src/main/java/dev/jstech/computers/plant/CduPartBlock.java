/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.bigblock.AbstractBigBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A part of the CDU, a big block: one of the eleven blocks round its origin. Used, it opens the CDU's screen; broken,
 * the whole CDU goes with it; the middle mouse button picks the CDU off it.
 */
public class CduPartBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<CduPartBlock> CODEC = simpleCodec(CduPartBlock::new);

    public CduPartBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
                                        final Player player) {
        // The whole CDU goes with any of its blocks, so a player's break here is a break of the CDU.
        if (level instanceof ServerLevel serverLevel && origin(level, pos) instanceof BlockPos origin
                && level.getBlockState(origin).getBlock() instanceof AbstractBigBlock big) {
            big.brokenBy(serverLevel, origin, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level,
                                       final BlockPos pos, final Player player) {
        final BlockPos origin = origin(level, pos);
        if (origin != null && level.getBlockState(origin).getBlock() instanceof CduBlock cdu) {
            return new ItemStack(cdu);
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CduPartBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<CduPartBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        final BlockPos origin = origin(level, pos);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && origin != null
                && level.getBlockEntity(origin) instanceof CduBlockEntity cdu) {
            CduBlock.open(serverPlayer, origin, cdu);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        final BlockPos origin = origin(level, pos);
        // Lifted whole, as a moving structure takes its blocks, the CDU is moved, not broken.
        if (!state.is(newState.getBlock()) && !movedByPiston && level instanceof ServerLevel serverLevel
                && origin != null && level.getBlockState(origin).getBlock() instanceof AbstractBigBlock big) {
            big.dissolve(serverLevel, origin, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The origin of the CDU the part at {@code pos} belongs to, or null when it has lost it. */
    @Nullable
    public static BlockPos origin(final LevelReader level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CduPartBlockEntity part ? part.originPos() : null;
    }
}
