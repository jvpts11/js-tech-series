/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.GameText;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A few pieces of a mineral's ore lying on the ground over its vein, as a prospector's float tells where a vein runs
 * (core/docs/design/world.md): two to four pieces, in one of {@link #LAYOUTS} ways turned any way, each cut
 * from the ore's own picture. It stands only on solid ground, is walked through and broken at once by hand, and gives
 * the ore itself, one of what its ore block gives (never a trace of it).
 */
public class OrePiecesBlock extends Block {

    private final Material material;

    /** How many ways its pieces may lie. */
    public static final int LAYOUTS = 3;
    /** The way its pieces lie, and which way they are turned. */
    public static final IntegerProperty LAYOUT = IntegerProperty.create("layout", 0, LAYOUTS - 1);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /* What the mouse finds of it: the pieces' spread, as low as the tallest. */
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 3, 14);

    public OrePiecesBlock(final Properties properties, final Material material) {
        super(properties);
        this.material = Objects.requireNonNull(material, "material");
        registerDefaultState(this.stateDefinition.any().setValue(LAYOUT, 0).setValue(FACING, Direction.NORTH));
    }

    /** The mineral whose ore its pieces are. */
    public Material material() {
        return this.material;
    }

    @Override
    public MutableComponent getName() {
        return GameText.component(MaterialTexts.PIECES.with(this.material.name()));
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        return Shapes.empty();
    }

    /* On solid ground only, as a stone lies on it. */
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

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYOUT, FACING);
    }
}
