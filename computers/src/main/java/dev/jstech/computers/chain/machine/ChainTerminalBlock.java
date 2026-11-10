/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Machine Terminal, the same for every machine of the chain (computers/docs/design/survival.md): the control
 * cabinet a player places at a machine's terminal place. Once a machine's whole shape stands round it, it becomes that
 * machine's own terminal, which runs it: as it is placed, as a block beside it changes, or when the player uses it.
 */
@TextHolder
public final class ChainTerminalBlock extends HorizontalDirectionalBlock {

    /** What it says to the player who uses it with no machine's shape whole round it. */
    public static final TextKey NO_MACHINE = TextKey.of("jsc.chain.terminal.no_machine",
            "No machine stands whole round this terminal yet: build its blocks in their places.");
    private static final MapCodec<ChainTerminalBlock> CODEC = simpleCodec(ChainTerminalBlock::new);

    public ChainTerminalBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server) {
            ChainMachines.formAt(server, pos, state.getValue(FACING));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level instanceof ServerLevel server && !ChainMachines.formAt(server, pos, state.getValue(FACING))) {
            player.displayClientMessage(GameText.component(NO_MACHINE), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block,
                                   final BlockPos from, final boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level instanceof ServerLevel server) {
            ChainMachines.formAt(server, pos, state.getValue(FACING));
        }
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
