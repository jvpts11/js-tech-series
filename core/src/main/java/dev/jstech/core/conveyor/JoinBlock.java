/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * A join, of a kind a mod declares ({@link ConveyorType}): a belt that gathers what comes in from behind, its left and
 * its right onto one belt to its front, taking from each in turn, as a zip does, so no line waits on another for long.
 * It stands alone, its own run.
 *
 * <pre>{@code
 * public static final BlockEntry<JoinBlock> JOIN = CONTENT.block("join",
 *         properties -> new JoinBlock(properties, MyTypes.BELT, () -> MyContent.CONVEYOR.get()))
 *         .named("Join").look(IBlockLook.facing(new IBlockModel.Handmade("join"))).register();
 * }</pre>
 */
public class JoinBlock extends BeltPieceBlock {

    private final MapCodec<JoinBlock> codec;

    public JoinBlock(final Properties properties, final ConveyorType type,
                     final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties, type, entity);
        this.codec = simpleCodec(made -> new JoinBlock(made, type, entity));
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(END, true));
    }

    /* From behind and both sides at its start, from above at its middle; nothing from its front, where it gives. */
    @Override
    public double intakeAt(final BlockState state, @Nullable final Direction side) {
        if (side == state.getValue(FACING)) {
            return NOWHERE;
        }
        return side == Direction.UP || side == null ? MIDDLE : START;
    }

    @Override
    public boolean joinsRuns() {
        return false;
    }

    @Override
    public boolean takesInTurn() {
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
