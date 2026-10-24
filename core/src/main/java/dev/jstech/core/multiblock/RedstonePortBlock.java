/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The block of a multiblock's redstone port, which a mod registers with its own look: it reads the signal round it
 * for its terminal, or, switched with an empty hand, gives out its terminal's signal.
 */
public class RedstonePortBlock extends Block implements EntityBlock {

    private final Supplier<? extends BlockEntityType<? extends RedstonePortBlockEntity>> type;

    /** Whether it gives its terminal's signal, rather than reading the one round it. */
    public static final BooleanProperty GIVES = BooleanProperty.create("gives");

    public RedstonePortBlock(final Properties properties,
                             final Supplier<? extends BlockEntityType<? extends RedstonePortBlockEntity>> type) {
        super(properties);
        this.type = Objects.requireNonNull(type, "type");
        registerDefaultState(this.stateDefinition.any().setValue(GIVES, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return this.type.get().create(pos, state);
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return state.getValue(GIVES);
    }

    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                            final Direction direction) {
        return state.getValue(GIVES) && level.getBlockEntity(pos) instanceof RedstonePortBlockEntity port
                ? port.given(level) : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        level.setBlock(pos, state.cycle(GIVES), Block.UPDATE_ALL);
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof RedstonePortBlockEntity port) {
            port.hear(server);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block from,
                                   final BlockPos fromPos, final boolean moving) {
        super.neighborChanged(state, level, pos, from, fromPos, moving);
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof RedstonePortBlockEntity port) {
            port.hear(server);
        }
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GIVES);
    }
}
