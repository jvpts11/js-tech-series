/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Light standing in the air, as the game's own light does where nothing gives it: no shape, nothing to hit or stand
 * on, given way by whatever is placed there, in water as well as in air. A spotlight's far end lights the world with
 * it ({@link BeamLights}), and a player's game lights the way of what is held with it. On the server it looks every
 * two seconds whether a light still holds it, and goes when none does, so none is left behind by a light that went
 * away with the world closed.
 */
public final class LightBlock extends Block implements SimpleWaterloggedBlock {

    /** Its level. */
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /* How often it looks whether it is held, in ticks. */
    private static final int LOOKS_EVERY = 40;

    public LightBlock(final Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LEVEL)));
        registerDefaultState(defaultBlockState().setValue(LEVEL, LightMath.BRIGHTEST).setValue(WATERLOGGED, false));
    }

    /** It, at {@code level}, in water where {@code wet}. */
    public BlockState at(final int level, final boolean wet) {
        return defaultBlockState().setValue(LEVEL, Math.clamp(level, 0, LightMath.BRIGHTEST)).setValue(WATERLOGGED,
                wet);
    }

    /** Whether light can stand at a place holding {@code state}: air, still water, or itself. */
    public static boolean fits(final BlockState state) {
        return state.isAir() || state.getBlock() instanceof LightBlock || state.is(Blocks.WATER)
                && state.getFluidState().isSource();
    }

    /** What the place held before light stood there: water for light in water, else air. */
    public static BlockState under(final BlockState state) {
        return state.getBlock() instanceof LightBlock && state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState()
                : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, WATERLOGGED);
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
        return true;
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(final BlockState state, final Direction direction, final BlockState neighbour,
                                     final LevelAccessor level, final BlockPos pos, final BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState old,
                           final boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, LOOKS_EVERY);
        }
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos,
                        final RandomSource random) {
        if (BeamLights.holds(level, pos)) {
            level.scheduleTick(pos, this, LOOKS_EVERY);
        } else {
            level.setBlock(pos, under(state), Block.UPDATE_ALL);
        }
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return ItemStack.EMPTY;
    }
}
