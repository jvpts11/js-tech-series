/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CaseFrontLayout;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.IStandingParts;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.BlockDrops;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * What the three small computers (Personal, Crafting and Cluster Management) share as blocks: a case that faces the
 * player, the power button and the other {@link CaseFrontClicks parts of its front} a click works, the side panel
 * that comes off when sneaking, the hardware menu a click anywhere else on the case opens, and handing the hardware
 * back when the block goes, however it goes. Each one brings its block entity, its menu and its ports; a rule about how
 * any of them is clicked, broken or ticked is written once, here.
 *
 * @param <E> the block entity the block holds
 */
public abstract class AbstractSmallComputerBlock<E extends AbstractSmallComputerBlockEntity>
        extends HorizontalDirectionalBlock implements EntityBlock, IFaceConnector, IComputerCase, IStandingParts {

    private final HardwareEra era;
    private final CaseStyle caseStyle;
    private final FacePorts ports;
    /** Where the case's front carries its power button, its bays and its USB ports. */
    private final CaseFrontLayout front;
    private final Class<E> entityClass;
    private final Supplier<BlockEntityType<E>> entityType;
    private final BlockEntityTicker<E> serverTicker;
    /*
     * The computers a player is breaking right now, whose hardware fell as the player broke them, or was forgone by a
     * player in creative: the removal that follows gives nothing back a second time. A removal with no player first
     * finds its computer missing here and gives the hardware back then.
     */
    private final Set<BlockPos> settled = ConcurrentHashMap.newKeySet();

    protected AbstractSmallComputerBlock(final Properties properties, final HardwareEra era,
                                         final CaseStyle caseStyle, final FacePorts ports,
                                         final Class<E> entityClass, final Supplier<BlockEntityType<E>> entityType,
                                         final BlockEntityTicker<E> serverTicker) {
        super(properties);
        this.era = era;
        this.caseStyle = caseStyle;
        this.ports = ports;
        this.front = CaseFrontLayout.of(era, caseStyle);
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

    /** Where the case's front carries its power button, its bays and its USB ports. */
    public CaseFrontLayout frontLayout() {
        return front;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state,
                                        final Player player) {
        // A player in creative takes the computer and leaves nothing; any other player gets its hardware back now.
        final E computer = computerAt(level, pos);
        if (level instanceof ServerLevel serverLevel && computer != null && settled.add(pos.immutable())
                && !player.getAbilities().instabuild) {
            spillHardware(serverLevel, pos, computer);
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
    protected ItemInteractionResult useItemOn(final ItemStack held, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        final E computer = computerAt(level, pos);
        final Direction facing = state.getValue(FACING);
        if (computer != null) {
            // A look that landed on a stick or an open tray, out of the case, works that part and nothing behind it.
            final List<CaseFrontClicks.StandingPart> parts = standingPartsOf(computer);
            final int part = DeviceFront.partAt(boxesOf(parts, facing), pos, hit.getLocation());
            if (part >= 0) {
                CaseFrontClicks.clickStanding(computer, parts.get(part), held, player);
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        final double[] at = DeviceFront.point(facing, pos, hit, player.getEyePosition(), 0);
        if (computer == null || at == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // The power button answers whatever the hand holds, as the one on the assembly screen does.
        if (front.power().contains(at[0], at[1])) {
            if (!level.isClientSide()) {
                computer.togglePower();
                computer.showPowerPressed();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        final double[] ahead = DeviceFront.point(facing, pos, hit, player.getEyePosition(),
                -CaseFrontClicks.TRAY_REACH);
        if (CaseFrontClicks.click(computer, front, at, ahead, held, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /* The sticks in its ports and its drives' open trays stand out of the case, looked at and clicked as its own. */
    @Override
    public List<AABB> standingParts(final BlockState state, final BlockGetter level, final BlockPos pos) {
        final E computer = computerAt(level, pos);
        return computer == null ? List.of() : boxesOf(standingPartsOf(computer), state.getValue(FACING));
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
            // Taken without a player (an explosion, a command, another mod's machine), it gives its hardware back here.
            if (!settled.remove(pos)) {
                spillHardware(serverLevel, pos, computer);
            }
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

    /* What the case holds falls on the ground: the installed hardware, and what its front's bays and ports hold. */
    private void spillHardware(final ServerLevel level, final BlockPos pos, final E computer) {
        computer.spillFront(level, pos);
        BlockDrops.spill(level, pos, computer.getHardware());
    }

    @Nullable
    private E computerAt(final BlockGetter level, final BlockPos pos) {
        final BlockEntity found = level.getBlockEntity(pos);
        return entityClass.isInstance(found) ? entityClass.cast(found) : null;
    }

    /* What stands out of the case now: a stick in each port that holds one, and the tray of each open drive. */
    private List<CaseFrontClicks.StandingPart> standingPartsOf(final E computer) {
        final CaseFront parts = computer.front();
        List<CaseFrontClicks.StandingPart> out = List.of();
        final int sockets = Math.min(parts.ports(), front.ports().size());
        for (int port = 0; port < sockets; port++) {
            if (!parts.stick(port).isEmpty()) {
                out = added(out, new CaseFrontClicks.StandingPart(true, port));
            }
        }
        final int bays = Math.min(parts.bays(), front.bays().size());
        for (int bay = 0; bay < bays; bay++) {
            final BayDriveSpec.Kind kind = parts.kind(bay);
            if (kind != null && kind != BayDriveSpec.Kind.HOT_SWAP && parts.tray(bay).isOpen()) {
                out = added(out, new CaseFrontClicks.StandingPart(false, bay));
            }
        }
        return out;
    }

    /* Each part as the box it fills, out of the front of a case facing {@code facing}. */
    private List<AABB> boxesOf(final List<CaseFrontClicks.StandingPart> parts, final Direction facing) {
        if (parts.isEmpty()) {
            return List.of();
        }
        final List<AABB> boxes = new ArrayList<>(parts.size());
        for (final CaseFrontClicks.StandingPart part : parts) {
            boxes.add(part.stick()
                    ? DeviceFront.box(front.stick(part.index()), -CaseFrontLayout.STICK_OUT, 0, facing)
                    : DeviceFront.box(front.tray(part.index()), -CaseFrontLayout.TRAY_OUT, 0, facing));
        }
        return boxes;
    }

    /* A list with one more, made only once something stands out, so a case with nothing out allocates nothing. */
    private static List<CaseFrontClicks.StandingPart> added(final List<CaseFrontClicks.StandingPart> list,
                                                            final CaseFrontClicks.StandingPart part) {
        final List<CaseFrontClicks.StandingPart> out = list.isEmpty() ? new ArrayList<>(2) : list;
        out.add(part);
        return out;
    }
}
