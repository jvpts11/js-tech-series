/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.menu.NetworkGatewayMenu;
import dev.jstech.core.peripheral.IPeripheralConnectable;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.util.BlockEntityTickers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Gateway block: placed facing the player, so its front (the ComputerCraft modem socket and
 * the status plate) looks at whoever placed it and its back (the peripheral cable socket) points at the
 * computer. A right-click opens its buffer; everything else about it is set on the host computer.
 */
public class NetworkGatewayBlock extends HorizontalDirectionalBlock implements EntityBlock, IPeripheralConnectable {

    public static final MapCodec<NetworkGatewayBlock> CODEC = simpleCodec(NetworkGatewayBlock::new);

    /** True while the status lights are on: linked to a host, or blinking for Identify. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public NetworkGatewayBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<NetworkGatewayBlock> codec() {
        return CODEC;
    }

    @Override
    public PeripheralCableType peripheralType() {
        return PeripheralCableType.COMPUTING;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof NetworkGatewayBlockEntity gateway) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inventory, p) -> new NetworkGatewayMenu(id, inventory, gateway),
                            Component.translatable(getDescriptionId())),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof NetworkGatewayBlockEntity gateway) {
            gateway.dropContents(level, pos);
            if (level instanceof ServerLevel serverLevel) {
                gateway.unlink(serverLevel);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new NetworkGatewayBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.NETWORK_GATEWAY_BE.get(),
                NetworkGatewayBlockEntity::serverTick);
    }
}
