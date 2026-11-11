/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.blockentity.MiningComputerPartBlockEntity;
import dev.jstech.core.bigblock.AbstractBigBlock;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The second block of a Mining Computer two blocks wide: it takes the cables its origin takes, and a click or a break
 * on it is one on the rig.
 */
public class MiningComputerPartBlock extends HorizontalDirectionalBlock implements EntityBlock, IFaceConnector {

    public static final MapCodec<MiningComputerPartBlock> CODEC = simpleCodec(MiningComputerPartBlock::new);
    /** The era the part inherits from its origin ({@link PartEras}). */
    public static final IntegerProperty ERA = PartEras.property();
    /* What any part may take, for the cables to know which lines it is a device of: every era's. */
    private static final FacePorts PORTS = MiningComputerBlock.portsOf(HardwareEra.ADVANCED);
    /* What a part of each era's rig takes, by the era's place among the series' seven. */
    private static final List<FacePorts> BY_ERA = byEra();

    public MiningComputerPartBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(ERA, PartEras.valueFor(HardwareEra.STANDARD)));
    }

    @Override
    public FacePorts ports() {
        return PORTS;
    }

    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return BY_ERA.get(state.getValue(ERA)).accepts(state, face, offered);
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
                                        final Player player) {
        // The whole rig goes with either of its blocks, so a player's break here is a break of the rig.
        if (level instanceof ServerLevel serverLevel && origin(level, pos) instanceof BlockPos origin
                && level.getBlockState(origin).getBlock() instanceof AbstractBigBlock big) {
            big.brokenBy(serverLevel, origin, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** What the middle mouse button picks off the rig: the rig itself, in its own era. */
    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level,
                                       final BlockPos pos, final Player player) {
        final BlockPos origin = origin(level, pos);
        if (origin != null && level.getBlockState(origin).getBlock() instanceof MiningComputerBlock rig) {
            return new ItemStack(rig);
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new MiningComputerPartBlockEntity(pos, state);
    }

    /** The origin of the rig the part at {@code pos} belongs to, or null when it has lost it. */
    @Nullable
    public static BlockPos origin(final LevelReader level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MiningComputerPartBlockEntity part ? part.originPos() : null;
    }

    @Override
    protected MapCodec<MiningComputerPartBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ERA);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack held, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        final BlockPos origin = origin(level, pos);
        return origin == null ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : MiningComputerBlock.seatAt(level, origin, player, held);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        final BlockPos origin = origin(level, pos);
        return origin == null ? InteractionResult.PASS : MiningComputerBlock.useAt(level, origin, player);
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        final BlockPos origin = origin(level, pos);
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel && origin != null
                && level.getBlockState(origin).getBlock() instanceof AbstractBigBlock big) {
            big.dissolve(serverLevel, origin, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static List<FacePorts> byEra() {
        final List<FacePorts> ports = new ArrayList<>();
        for (int value = 0; value <= PartEras.LAST; value++) {
            ports.add(MiningComputerBlock.portsOf(PartEras.eraOf(value)));
        }
        return List.copyOf(ports);
    }
}
