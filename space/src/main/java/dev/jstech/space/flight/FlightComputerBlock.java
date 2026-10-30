/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.multiblock.plan.IPlanMember;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Flight Computer's block, the one block of the rocket kits that keeps something of its own, its state and its
 * screen, so it never merges into its part's formed model: once its capsule or its lander's cabin is formed, the
 * model draws it on its panel, so the block draws nothing and nothing bumps into it, while a click still works it.
 */
public final class FlightComputerBlock extends DeviceBlock implements IPlanMember {

    private final Device<?> device;
    private final MapCodec<FlightComputerBlock> codec;

    /** Whether its part is formed, its model drawing it. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public FlightComputerBlock(final Properties properties, final Device<?> device) {
        super(properties, device);
        this.device = device;
        this.codec = simpleCodec(made -> new FlightComputerBlock(made, this.device));
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed) {
        final BlockState state = level.getBlockState(pos);
        if (state.hasProperty(FORMED) && state.getValue(FORMED) != formed) {
            level.setBlock(pos, state.setValue(FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    /*
     * Formed, it hides nothing and lets the light through as its capsule's merged blocks do, so the light where it
     * stands, which its rocket's parts are drawn by, is the capsule's and not the dark inside a solid block.
     */
    @Override
    protected VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }
}
