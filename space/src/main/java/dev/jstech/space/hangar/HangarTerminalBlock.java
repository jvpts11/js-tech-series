/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Rocket Hangar's terminal, set in a wall of the hangar facing into its bay, the way it faces whoever set it: from
 * it the hangar finds its bay ({@link HangarTerminalBlockEntity}). Used, it opens the hangar's design screen once the
 * hangar is formed, or says what keeps it from forming. A data cable on any face puts it on a network, which may set
 * its crane building. Broken, the hangar is undone, and the blocks its crane held drop.
 */
public final class HangarTerminalBlock extends HorizontalDirectionalBlock implements EntityBlock, IFaceConnector {

    private static final MapCodec<HangarTerminalBlock> CODEC = simpleCodec(HangarTerminalBlock::new);
    /* A machine on the network, as a server's access and backbone lines reach it, of every era. */
    private static final FacePorts DATA = FacePorts.everyFace(DataLines.upTo(HardwareEra.SINGULARITY,
            DataLine.ACCESS, DataLine.BACKBONE));

    public HangarTerminalBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new HangarTerminalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> type) {
        if (level.isClientSide() || type != HangarBlocks.TERMINAL_ENTITY.get()) {
            return null;
        }
        return (ticking, pos, at, entity) -> HangarTerminalBlockEntity.serverTick(ticking, pos, at,
                (HangarTerminalBlockEntity) entity);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HangarTerminalBlockEntity terminal) {
            terminal.look();
            if (terminal.bay().isPresent() && player instanceof ServerPlayer designer) {
                RocketDesigner.open(designer, terminal);
            } else {
                player.displayClientMessage(GameText.component(terminal.reason()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState after,
                            final boolean moved) {
        if (!state.is(after.getBlock()) && level.getBlockEntity(pos) instanceof HangarTerminalBlockEntity terminal) {
            terminal.broken();
        }
        super.onRemove(state, level, pos, after, moved);
    }

    @Override
    public FacePorts ports() {
        return DATA;
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
