/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.machine.MachineWrench;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A belt, of a kind a mod declares ({@link ConveyorType}), facing the way it moves, flat or sloped a block up or down
 * across its block ({@link BeltSlope}). A belt laid where a belt of its kind waits a block above, ahead or behind,
 * slopes to meet it, and the belt it meets slopes to meet it back when nothing else holds that end, as a rail does; a
 * wrench turns a belt's slope. A sloped belt is a stair of four steps to walk on.
 *
 * <pre>{@code
 * public static final BlockEntry<ConveyorBlock> BELT = CONTENT.block("belt",
 *         properties -> new ConveyorBlock(properties, MyTypes.BELT, () -> MyContent.CONVEYOR.get()))
 *         .named("Belt").look(IBlockLook.facingByState(state -> new IBlockModel.Handmade(
 *                 "belt_" + state.getValue(ConveyorBlock.SLOPE).getSerializedName()))).register();
 * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBlockEntity>> CONVEYOR =
 *         CONTENT.blockEntity("conveyor", (pos, state) -> new ConveyorBlockEntity(CONVEYOR.get(), pos, state), BELT);
 * }</pre>
 */
public class ConveyorBlock extends BeltPieceBlock {

    private final MapCodec<ConveyorBlock> codec;

    /** How the belt lies across its block. */
    public static final EnumProperty<BeltSlope> SLOPE = EnumProperty.create("slope", BeltSlope.class);
    /* A sloped belt is a stair of this many steps. */
    private static final int STEPS = 4;
    private static final float SLOPED = 45.0F;

    public ConveyorBlock(final Properties properties, final ConveyorType type,
                         final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties, type, entity);
        this.codec = simpleCodec(made -> new ConveyorBlock(made, type, entity));
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(SLOPE, BeltSlope.FLAT).setValue(END, true));
    }

    @Override
    public ConveyorOpening entry(final BlockState state, final BlockPos pos) {
        return new ConveyorOpening(state.getValue(SLOPE) == BeltSlope.FALLING ? pos.above() : pos,
                state.getValue(FACING));
    }

    @Override
    public ConveyorOpening exit(final BlockState state, final BlockPos pos) {
        final Direction facing = state.getValue(FACING);
        final BlockPos ahead = pos.relative(facing);
        return new ConveyorOpening(state.getValue(SLOPE) == BeltSlope.RISING ? ahead.above() : ahead, facing);
    }

    /* A sloped belt's line rises, or falls, a block from its back edge to its front. */
    @Override
    public ConveyorGeometry geometry(final BlockState state) {
        final ConveyorGeometry flat = super.geometry(state);
        return switch (state.getValue(SLOPE)) {
            case FLAT -> flat;
            case RISING -> new ConveyorGeometry(flat.start(), flat.end().add(0.0, 1.0, 0.0), flat.facing(), SLOPED);
            case FALLING -> new ConveyorGeometry(flat.start().add(0.0, 1.0, 0.0), flat.end(), flat.facing(),
                    -SLOPED);
        };
    }

    /* Only a flat belt is worked from above. */
    @Override
    public boolean workedFromAbove(final BlockState state) {
        return state.getValue(SLOPE) == BeltSlope.FLAT;
    }

    /* A belt laid moves away from the player who lays it, and slopes to meet a belt of its kind a block off. */
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState flat = super.getStateForPlacement(context);
        if (firstAhead(level, pos, flat) != null || firstBehind(level, pos, flat) != null) {
            return flat;
        }
        final BlockState rising = flat.setValue(SLOPE, BeltSlope.RISING);
        if (firstAhead(level, pos, rising) != null) {
            return rising;
        }
        final BlockState falling = flat.setValue(SLOPE, BeltSlope.FALLING);
        return firstBehind(level, pos, falling) != null ? falling : flat;
    }

    /*
     * A flat belt of its kind that ends a block below and behind the one laid rises to meet it, and one that begins a
     * block below and ahead of it falls from it, when nothing else holds that end of it.
     */
    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        final Direction facing = state.getValue(FACING);
        meetFrom(level, entry(state, pos).cell().relative(facing.getOpposite()).below(), facing, BeltSlope.RISING);
        meetFrom(level, exit(state, pos).cell().below(), facing, BeltSlope.FALLING);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOPE, END);
    }

    /* A wrench turns the belt's slope: flat, rising, falling, and flat again. */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!MachineWrench.isWrench(stack) || player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(SLOPE, state.getValue(SLOPE).next()), Block.UPDATE_ALL);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /* Flat, a slab as high as the belt; sloped, four steps rising the way it rises. */
    @Override
    protected VoxelShape shapeOf(final BlockState state) {
        final BeltSlope slope = state.getValue(SLOPE);
        if (slope == BeltSlope.FLAT) {
            return super.shapeOf(state);
        }
        final Direction facing = state.getValue(FACING);
        final double step = PIXELS / STEPS;
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < STEPS; i++) {
            final int rise = slope == BeltSlope.RISING ? i : STEPS - 1 - i;
            shape = Shapes.or(shape, along(facing, step * i, step * (i + 1),
                    Math.min(PIXELS, type().height() + step * rise)));
        }
        return shape.optimize();
    }

    /* Turns the flat belt of its kind facing {@code facing} at {@code at} to {@code slope}, when that end is free. */
    private void meetFrom(final Level level, final BlockPos at, final Direction facing, final BeltSlope slope) {
        final BlockState belt = level.getBlockState(at);
        if (!belt.is(this) || belt.getValue(FACING) != facing || belt.getValue(SLOPE) != BeltSlope.FLAT) {
            return;
        }
        final boolean free = slope == BeltSlope.RISING ? firstAhead(level, at, belt) == null
                : firstBehind(level, at, belt) == null;
        if (free) {
            level.setBlock(at, belt.setValue(SLOPE, slope), Block.UPDATE_ALL);
        }
    }

    /* A box the whole width of the block, from {@code from} to {@code to} pixels along {@code facing}. */
    private static VoxelShape along(final Direction facing, final double from, final double to, final double top) {
        return switch (facing) {
            case SOUTH -> box(0, 0, from, PIXELS, top, to);
            case NORTH -> box(0, 0, PIXELS - to, PIXELS, top, PIXELS - from);
            case EAST -> box(from, 0, 0, to, top, PIXELS);
            default -> box(PIXELS - to, 0, 0, PIXELS - from, top, PIXELS);
        };
    }
}
