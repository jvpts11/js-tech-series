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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * A splitter, of a kind a mod declares ({@link ConveyorType}): a belt that deals what comes in from behind out to its
 * front, its left and its right, one item to each in turn, passing over a way that takes nothing. It stands alone,
 * its own run.
 *
 * <pre>{@code
 * public static final BlockEntry<SplitterBlock> SPLITTER = CONTENT.block("splitter",
 *         properties -> new SplitterBlock(properties, MyTypes.BELT, () -> MyContent.CONVEYOR.get()))
 *         .named("Splitter").look(IBlockLook.facing(new IBlockModel.Handmade("splitter"))).register();
 * }</pre>
 */
public class SplitterBlock extends BeltPieceBlock {

    private final MapCodec<SplitterBlock> codec;

    public SplitterBlock(final Properties properties, final ConveyorType type,
                         final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties, type, entity);
        this.codec = simpleCodec(made -> new SplitterBlock(made, type, entity));
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(END, true));
    }

    /* To its front, its left and its right. */
    @Override
    public List<ConveyorOutlet> outlets(final BlockState state, final BlockPos pos) {
        final Direction facing = state.getValue(FACING);
        final Direction left = BeltSide.LEFT.of(facing);
        final Direction right = BeltSide.RIGHT.of(facing);
        return List.of(new ConveyorOutlet(pos.relative(facing), facing.getOpposite()),
                new ConveyorOutlet(pos.relative(left), left.getOpposite()),
                new ConveyorOutlet(pos.relative(right), right.getOpposite()));
    }

    /* From behind at its start, from above at its middle; its other sides give. */
    @Override
    public double intakeAt(final BlockState state, @Nullable final Direction side) {
        final Direction facing = state.getValue(FACING);
        if (side == facing.getOpposite()) {
            return START;
        }
        return side == Direction.UP || side == null ? MIDDLE : NOWHERE;
    }

    @Override
    public boolean joinsRuns() {
        return false;
    }

    @Override
    public boolean dealsInTurn() {
        return true;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, END);
    }
}
