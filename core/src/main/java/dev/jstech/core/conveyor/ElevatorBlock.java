/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A block of bucket elevator, of a kind a mod declares ({@link ConveyorType}): buckets on a belt that lift items up,
 * each a gap above the one below, elevators one above another a run. The buckets scoop up what comes in at any side or
 * from below; at the top of the run they tip it out to the front, the way the elevator faces, or, with nothing there,
 * into the block above. It moves by itself, on energy, or on a shaft across it, at the head as at any block.
 *
 * <pre>{@code
 * public static final BlockEntry<ElevatorBlock> ELEVATOR = CONTENT.block("bucket_elevator",
 *         properties -> new ElevatorBlock(properties, MyTypes.ELEVATOR, () -> MyContent.CONVEYOR.get()))
 *         .named("Bucket elevator").look(IBlockLook.facing(new IBlockModel.Handmade("bucket_elevator")))
 *         .register();
 * }</pre>
 */
public class ElevatorBlock extends ConveyorPieceBlock {

    private final MapCodec<ElevatorBlock> codec;

    /** The way the elevator tips out what it lifts, at the top of its run. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final double INSET = 1.0;
    private static final float UPRIGHT = 90.0F;
    private static final VoxelShape SHAPE = box(INSET, 0, INSET, PIXELS - INSET, PIXELS, PIXELS - INSET);
    private static final Vec3 FOOT = new Vec3(MIDDLE, 0.0, MIDDLE);
    private static final Vec3 HEAD = new Vec3(MIDDLE, 1.0, MIDDLE);

    public ElevatorBlock(final Properties properties, final ConveyorType type,
                         final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties, type, entity);
        this.codec = simpleCodec(made -> new ElevatorBlock(made, type, entity));
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(END, true));
    }

    @Override
    public ConveyorOpening entry(final BlockState state, final BlockPos pos) {
        return new ConveyorOpening(pos, Direction.UP);
    }

    @Override
    public ConveyorOpening exit(final BlockState state, final BlockPos pos) {
        return new ConveyorOpening(pos.above(), Direction.UP);
    }

    /* To its front, then above. */
    @Override
    public List<ConveyorOutlet> outlets(final BlockState state, final BlockPos pos) {
        final Direction facing = state.getValue(FACING);
        return List.of(new ConveyorOutlet(pos.relative(facing), facing.getOpposite()),
                new ConveyorOutlet(pos.above(), Direction.DOWN));
    }

    /* Its buckets scoop up what comes in at any side or from below, at its foot; nothing from above. */
    @Override
    public double intakeAt(final BlockState state, @Nullable final Direction side) {
        return side == Direction.UP ? NOWHERE : START;
    }

    @Override
    public ConveyorGeometry geometry(final BlockState state) {
        return new ConveyorGeometry(FOOT, HEAD, state.getValue(FACING), UPRIGHT);
    }

    /* Its belt's pulleys turn across the way it tips out. */
    @Override
    public Direction.Axis axle(final BlockState state) {
        return state.getValue(FACING).getClockWise().getAxis();
    }

    /* An elevator laid tips out away from the player who lays it. */
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, END);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE;
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
