/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.Neighbours;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A block that makes a block entity and does what its {@link Device} says: it faces whoever placed it, ticks its
 * block entity on the server, opens its menu when used with an empty hand, and spills the block entity's inventories
 * declared {@code dropsWhenBroken()} when it is broken or replaced, however that happens.
 *
 * <p>A block that does more than that (takes a medium into a slot, forms a structure) extends this one and adds only
 * what it does besides.
 */
public class DeviceBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private final Device<?> device;
    private final MapCodec<DeviceBlock> codec;

    public DeviceBlock(final Properties properties, final Device<?> device) {
        super(properties);
        this.device = device;
        this.codec = simpleCodec(made -> new DeviceBlock(made, device));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return device.create(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        return level.isClientSide() ? null : device.serverTicker(type);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!device.opensMenu()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            device.openMenu(server, level.getBlockEntity(pos), getName(), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /* The block entity hears which of its faces saw its neighbour change, as it declared it wants to. */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighbourBlock, final BlockPos neighbourPos,
                                   final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);
        final Direction face = Neighbours.faceTowards(pos, neighbourPos);
        if (face != null && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().neighbourChanged(level, face);
        }
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().broken(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
