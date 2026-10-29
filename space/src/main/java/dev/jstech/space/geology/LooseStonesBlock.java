/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Loose stones lying on the ground: a few small stones of a rock, never a whole block, in one of the
 * {@link StoneArrangements}, turned any way. They are walked through, broken at once to be picked up, and lie only
 * on ground that holds them, going with it when it goes. A stone placed lies as chance has it, the same at the same
 * place.
 */
public final class LooseStonesBlock extends HorizontalDirectionalBlock {

    /** Which of the {@link StoneArrangements} the stones lie in. */
    public static final IntegerProperty ARRANGEMENT = IntegerProperty.create("arrangement", 0,
            StoneArrangements.ALL.size() - 1);

    private static final MapCodec<LooseStonesBlock> CODEC = simpleCodec(LooseStonesBlock::new);
    /* Each arrangement's outline, for each facing in the order of their two-dimensional values. */
    private static final VoxelShape[][] SHAPES = shapes();
    /* The facing a block state's model is turned by none, and a quarter turn's degrees. */
    private static final int UNTURNED_FACING = 180;
    private static final int QUARTER = 90;

    public LooseStonesBlock(final Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ARRANGEMENT, 0));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final RandomSource chance = RandomSource.create(Mth.getSeed(context.getClickedPos()));
        return defaultBlockState().setValue(FACING, Direction.Plane.HORIZONTAL.getRandomDirection(chance))
                .setValue(ARRANGEMENT, chance.nextInt(StoneArrangements.ALL.size()));
    }

    /**
     * How many quarters clockwise the stones facing {@code facing} are turned from facing north, as their block
     * state turns their model.
     */
    public static int quarterTurns(final Direction facing) {
        return Math.floorMod((int) facing.toYRot() + UNTURNED_FACING, 360) / QUARTER;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ARRANGEMENT);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPES[state.getValue(ARRANGEMENT)][state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        final BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(final BlockState state, final Direction direction, final BlockState neighbour,
                                     final LevelAccessor level, final BlockPos pos, final BlockPos neighbourPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    private static VoxelShape[][] shapes() {
        final VoxelShape[][] out = new VoxelShape[StoneArrangements.ALL.size()][4];
        for (int arrangement = 0; arrangement < out.length; arrangement++) {
            final List<StoneArrangements.Box> boxes = StoneArrangements.ALL.get(arrangement);
            for (final Direction facing : Direction.Plane.HORIZONTAL) {
                VoxelShape shape = Shapes.empty();
                for (final StoneArrangements.Box box : boxes) {
                    final StoneArrangements.Box turned = box.turned(quarterTurns(facing));
                    shape = Shapes.or(shape, Block.box(turned.x0(), turned.y0(), turned.z0(), turned.x1(),
                            turned.y1(), turned.z1()));
                }
                out[arrangement][facing.get2DDataValue()] = shape;
            }
        }
        return out;
    }
}
