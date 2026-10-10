/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.BlockDrops;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * What the three small computers (Personal, Crafting and Cluster Management) share as blocks: a case that faces the
 * player, the side panel that comes off when sneaking, the hardware menu, and handing the hardware back when the
 * block goes. Each one brings its block entity, its menu and its ports; a rule about how any of them is clicked,
 * broken or ticked is written once, here.
 *
 * @param <E> the block entity the block holds
 */
public abstract class AbstractSmallComputerBlock<E extends AbstractSmallComputerBlockEntity>
        extends HorizontalDirectionalBlock implements EntityBlock, IFaceConnector, IComputerCase {

    private final HardwareEra era;
    private final CaseStyle caseStyle;
    private final FacePorts ports;
    private final Class<E> entityClass;
    private final Supplier<BlockEntityType<E>> entityType;
    private final BlockEntityTicker<E> serverTicker;

    protected AbstractSmallComputerBlock(final Properties properties, final HardwareEra era,
                                         final CaseStyle caseStyle, final FacePorts ports,
                                         final Class<E> entityClass, final Supplier<BlockEntityType<E>> entityType,
                                         final BlockEntityTicker<E> serverTicker) {
        super(properties);
        this.era = era;
        this.caseStyle = caseStyle;
        this.ports = ports;
        this.entityClass = entityClass;
        this.entityType = entityType;
        this.serverTicker = serverTicker;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * The hardware era this computer belongs to. It selects the block's skin and gates which board the machine
     * accepts: only a board of this era (and of the era's form factor) installs.
     */
    public HardwareEra era() {
        return era;
    }

    @Override
    public HardwareEra chassisEra() {
        return era();
    }

    @Override
    public CaseStyle caseStyle() {
        return caseStyle;
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
                                        final Player player) {
        // Spill the installed hardware so a broken computer never destroys its components.
        final E computer = computerAt(level, pos);
        if (level instanceof ServerLevel serverLevel && !player.getAbilities().instabuild && computer != null) {
            BlockDrops.spill(serverLevel, pos, computer.getHardware());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, entityType.get(), serverTicker);
    }

    /** The case is one model drawn by the block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        // The computer block is hardware only: clicking it opens the hardware-assembly menu, and the software is
        // used on a linked monitor.
        final E computer = computerAt(level, pos);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && computer != null) {
            // Sneaking takes the left side off the case, or puts it back, to see what is inside.
            if (player.isShiftKeyDown()) {
                computer.toggleSidePanel();
                return InteractionResult.sidedSuccess(false);
            }
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> createMenu(id, inventory, computer), menuTitle()),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        final E computer = computerAt(level, pos);
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel && computer != null) {
            // Drops this computer's network-node registration.
            computer.onBroken(serverLevel);
            dropWhenRemoved(serverLevel, pos, computer);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The menu a click opens on the computer. */
    protected abstract AbstractContainerMenu createMenu(int containerId, Inventory inventory, E computer);

    /** The title of that menu. */
    protected abstract Component menuTitle();

    /** Whatever else the computer lets fall when it goes, besides its hardware; nothing unless a computer says. */
    protected void dropWhenRemoved(final ServerLevel level, final BlockPos pos, final E computer) {
    }

    @Nullable
    private E computerAt(final Level level, final BlockPos pos) {
        final BlockEntity found = level.getBlockEntity(pos);
        return entityClass.isInstance(found) ? entityClass.cast(found) : null;
    }
}
