/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.connect.Neighbours;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.signal.ISignalOutput;
import dev.jstech.core.team.Ownership;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A block that makes a block entity and does what its {@link Device} says: it faces whoever placed it, ticks its
 * block entity on the server, opens its menu when used with an empty hand, is filled and emptied by a bucket when it
 * takes one, and spills the block entity's inventories declared {@code dropsWhenBroken()} when it is broken or
 * replaced, however that happens.
 *
 * <p>A block that does more than that (takes a medium into a slot, forms a structure) extends this one and adds only
 * what it does besides. A cable or a line joins it through the ports its device declares, none unless it does.
 */
public class DeviceBlock extends HorizontalDirectionalBlock implements EntityBlock, IFaceConnector {

    private final Device<?> device;
    private final MapCodec<DeviceBlock> codec;

    public DeviceBlock(final Properties properties, final Device<?> device) {
        super(properties);
        this.device = device;
        this.codec = simpleCodec(made -> new DeviceBlock(made, device));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return device.create(pos, state);
    }

    @Override
    public FacePorts ports() {
        return device.ports();
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        return level.isClientSide() ? null : device.serverTicker(type);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!device.opensMenu()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            final BlockEntity at = level.getBlockEntity(pos);
            device.openMenu(server, at, at instanceof IMenuTitle titled ? titled.menuTitle() : getName(), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /* A container of fluid in the hand fills or empties a device that takes them, before anything else it does. */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (device.fillsFromContainersByHand()
                && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /* The block entity hears which of its faces saw its neighbour change, as it declared it wants to. */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighbourBlock, final BlockPos neighbourPos,
                                   final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);
        final Direction face = Neighbours.faceTowards(pos, neighbourPos);
        if (face != null && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().neighbourChanged(level, face);
        }
    }

    /* Whoever places a machine owns it. */
    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof MachineBlockEntity machine && machine.ownership() == null) {
            machine.own(Ownership.of(player.getUUID()));
        }
    }

    /* A comparator reads what its block entity speaks in redstone, as a sensor or a machine says. */
    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ISignalOutput output ? output.strength() : 0;
    }

    /* A working machine shows it, on the player's side. */
    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos,
                            final RandomSource random) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine && machine.running()) {
            machine.animate(level, pos, random);
        }
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().broken(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
