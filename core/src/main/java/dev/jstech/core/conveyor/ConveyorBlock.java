/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A block of conveyor, of a kind a mod declares ({@link ConveyorType}), facing the way its belt moves. Belts facing
 * the same way one behind another are a run, moved as one by the belt at its end, the only one whose state says it is
 * an end and the only one that ticks: a run of any length costs a belt's tick. Its block entity is of a type its mod
 * registers, as a part of the mechanical kit's is.
 *
 * <pre>{@code
 * public static final BlockEntry<ConveyorBlock> BELT = CONTENT.block("belt",
 *         properties -> new ConveyorBlock(properties, MyTypes.BELT, () -> MyContent.CONVEYOR.get()))
 *         .named("Belt").look(IBlockLook.facing(new IBlockModel.Handmade("belt"))).register();
 * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBlockEntity>> CONVEYOR =
 *         CONTENT.blockEntity("conveyor", (pos, state) -> new ConveyorBlockEntity(CONVEYOR.get(), pos, state), BELT);
 * }</pre>
 */
public class ConveyorBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private final ConveyorType type;
    private final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity;
    private final MapCodec<ConveyorBlock> codec;
    private final VoxelShape shape;

    /** Whether the belt is the end of its run: no belt facing its way stands in front of it. */
    public static final BooleanProperty END = BooleanProperty.create("end");

    public ConveyorBlock(final Properties properties, final ConveyorType type,
                         final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties);
        this.type = Objects.requireNonNull(type, "type");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.codec = simpleCodec(made -> new ConveyorBlock(made, type, entity));
        this.shape = box(0, 0, 0, 16, type.height(), 16);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(END, true));
    }

    /** The kind of conveyor it is. */
    public ConveyorType type() {
        return this.type;
    }

    /** The type of its block entity. */
    public BlockEntityType<? extends ConveyorBlockEntity> entityType() {
        return this.entity.get();
    }

    /**
     * Whether the belt in {@code state} at {@code pos} carries on into the belt in front of it, in one run: one of its
     * kind, facing its way. A belt of another kind ahead takes what it hands on, but moves on its own.
     */
    public static boolean continuesInto(final BlockState state, final BlockGetter level, final BlockPos pos) {
        final Direction facing = state.getValue(FACING);
        final BlockState ahead = level.getBlockState(pos.relative(facing));
        return state.getBlock() instanceof ConveyorBlock self && ahead.getBlock() instanceof ConveyorBlock next
                && next.type == self.type && ahead.getValue(FACING) == facing;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, END);
    }

    /* A belt laid moves away from the player who lays it. */
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        return state.setValue(END, !continuesInto(state, context.getLevel(), context.getClickedPos()));
    }

    /* Whether it ends its run is its state's, worked out again as the blocks around it change. */
    @Override
    protected BlockState updateShape(final BlockState state, final Direction direction, final BlockState neighbour,
                                     final LevelAccessor level, final BlockPos pos, final BlockPos neighbourPos) {
        return state.setValue(END, !continuesInto(state, level, pos));
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return this.shape;
    }

    /* An item that lands on the belt is taken onto it, and what stands on it is carried. */
    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (level.getBlockEntity(pos) instanceof ConveyorBlockEntity belt) {
            belt.touched(entity);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return this.entity.get().create(pos, state);
    }

    /* Only the end of a run ticks: it moves the whole run. */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> blockEntity) {
        if (level.isClientSide() || !state.getValue(END)) {
            return null;
        }
        return BlockEntityTickers.create(blockEntity, this.entity.get(), ConveyorBlockEntity::serverTick);
    }
}
