/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.blockentity.PersonalRouterBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.INetworkBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * The Personal Router: a simple, tier-less block that converts between Ethernet and HBW so a Personal Computer (Ethernet) can reach the HBW backbone.
 */
public class PersonalRouterBlock extends HorizontalDirectionalBlock
        implements EntityBlock, IFaceConnector, INetworkBridge {

    public static final MapCodec<PersonalRouterBlock> CODEC = simpleCodec(PersonalRouterBlock::new);
    /* The Personal Router bridges exactly the two access and backbone tiers it converts between, on any face. */
    private static final FacePorts PORTS = FacePorts.everyFace(
            DataLines.of(DataTier.T1_ETHERNET, DataTier.T2_HBW));

    public PersonalRouterBlock(final Properties properties) {
        super(properties);
        // Facing is purely cosmetic (the status panel); cables still connect on every side.
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<PersonalRouterBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public FacePorts ports() {
        return PORTS;
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            DataWires.removeRouter(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new PersonalRouterBlockEntity(pos, state);
    }
}
