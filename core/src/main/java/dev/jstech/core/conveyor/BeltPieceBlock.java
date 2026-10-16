/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of conveyor that is a belt lying in its block, facing the way it moves: the plain belt
 * ({@link ConveyorBlock}), and the splitter, the join and the diverter, which are belts that deal out, gather in and
 * sort. Flat unless the kind of piece says otherwise; a belt laid moves away from the player who lays it; its rollers
 * turn across it, and it carries what stands on it when its kind does.
 */
public abstract class BeltPieceBlock extends ConveyorPieceBlock {

    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    /** The way the belt moves. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected BeltPieceBlock(final Properties properties, final ConveyorType type,
                             final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties, type, entity);
    }

    @Override
    public ConveyorOpening entry(final BlockState state, final BlockPos pos) {
        return new ConveyorOpening(pos, state.getValue(FACING));
    }

    @Override
    public ConveyorOpening exit(final BlockState state, final BlockPos pos) {
        final Direction facing = state.getValue(FACING);
        return new ConveyorOpening(pos.relative(facing), facing);
    }

    /* From behind at its start, from the sides and above at its middle; nothing from its front, where it gives. */
    @Override
    public double intakeAt(final BlockState state, @Nullable final Direction side) {
        final Direction facing = state.getValue(FACING);
        if (side == facing) {
            return NOWHERE;
        }
        return side == facing.getOpposite() ? START : MIDDLE;
    }

    /* The line along the middle of the belt's top, from the middle of its back edge to the middle of its front. */
    @Override
    public ConveyorGeometry geometry(final BlockState state) {
        final Direction facing = state.getValue(FACING);
        final double surface = type().height() / PIXELS;
        return new ConveyorGeometry(new Vec3(MIDDLE - facing.getStepX() * MIDDLE, surface,
                MIDDLE - facing.getStepZ() * MIDDLE), new Vec3(MIDDLE + facing.getStepX() * MIDDLE, surface,
                MIDDLE + facing.getStepZ() * MIDDLE), facing, 0.0F);
    }

    /* An item lying on the belt, or a little above it, is taken on where it lies. */
    @Override
    public double landsAt(final BlockState state, final BlockPos pos, final Vec3 local) {
        final ConveyorGeometry line = geometry(state);
        final double along = line.along(local);
        return local.y - line.at(along).y <= LYING ? along : NOWHERE;
    }

    @Override
    public @Nullable Direction carries(final BlockState state) {
        return state.getValue(FACING);
    }

    /* Its rollers turn across it. */
    @Override
    public Direction.Axis axle(final BlockState state) {
        return state.getValue(FACING).getClockWise().getAxis();
    }

    @Override
    public boolean workedFromAbove(final BlockState state) {
        return true;
    }

    /* A belt laid moves away from the player who lays it. */
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return this.shapes.computeIfAbsent(state, this::shapeOf);
    }

    /** The belt's shape in {@code state}, worked out once a state: flat, a slab as high as the belt, unless said. */
    protected VoxelShape shapeOf(final BlockState state) {
        return box(0, 0, 0, PIXELS, type().height(), PIXELS);
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
