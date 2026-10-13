/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.menu.RedstoneInterfaceMenu;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.BlockEntityTickers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A Redstone Interface: a small sensor on the peripheral cable that lets its computer read a redstone signal or emit
 * one. It is placed as an observer is, its lens toward what the player looks at and its back, where the cable plugs,
 * toward the player, any of six ways. Only the lens's face reads or emits: reading, it takes the signal coming into
 * that face; emitting, it powers the block there strongly, as a lever powers the block it stands on.
 *
 * <p>Like any device on a USB or parallel port it has no power of its own: with no computer at the other end of its
 * cable it reads nothing and emits nothing. It takes a device port of its computer, or of a hub.
 */
public class RedstoneInterfaceBlock extends DirectionalBlock implements EntityBlock, IFaceConnector {

    private final HardwareEra era;
    private final FacePorts ports;

    public static final MapCodec<RedstoneInterfaceBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(RedstoneInterfaceBlock::era)
    ).apply(i, RedstoneInterfaceBlock::new));

    public RedstoneInterfaceBlock(final Properties properties, final HardwareEra era) {
        super(properties);
        this.era = era;
        this.ports = PeripheralSockets.back(era);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The era of its housing, which decides the port on its back. */
    public HardwareEra era() {
        return era;
    }

    /** The port of its era in the middle of its back, where its wires go into a gland. */
    @Override
    public FacePorts ports() {
        return ports;
    }

    /* The lens faces the way the player looks, so it meets the block the player placed it against. */
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RedstoneInterfaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.REDSTONE_INTERFACE_BE.get(),
                RedstoneInterfaceBlockEntity::serverTick);
    }

    /* Its screen opens with its name and its computer's, which a plain device menu does not carry. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof RedstoneInterfaceBlockEntity sensor) {
            final RedstoneInterfaceMenu.Opening opening = sensor.opening((ServerLevel) level);
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new RedstoneInterfaceMenu(id,
                    inventory, sensor, opening), getName()), opening::write);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    /*
     * What it emits toward the neighbour asking: only the block in front of its lens, which asks from the side facing
     * it, gets anything.
     */
    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                            final Direction side) {
        return side == state.getValue(FACING).getOpposite()
                && level.getBlockEntity(pos) instanceof RedstoneInterfaceBlockEntity sensor ? sensor.output() : 0;
    }

    @Override
    protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final Direction side) {
        return getSignal(state, level, pos, side);
    }

    /** Tells the block in front of its lens, and the blocks around that one, that what it emits changed. */
    public void updateFront(final Level level, final BlockPos pos, final BlockState state) {
        final Direction facing = state.getValue(FACING);
        final BlockPos front = pos.relative(facing);
        level.neighborChanged(front, this, pos);
        level.updateNeighborsAtExceptFromFacing(front, this, facing.getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    /*
     * Broken or replaced, it frees its port on its computer, and the block it was powering hears that it stopped,
     * which the game would not tell it once this block is gone.
     */
    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
                synced.fields().broken(level, pos);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
            updateFront(level, pos, state);
            return;
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
