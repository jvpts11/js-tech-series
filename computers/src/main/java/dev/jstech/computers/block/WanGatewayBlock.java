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
import dev.jstech.computers.blockentity.ChunkAnchor;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.menu.WanGatewayMenu;
import dev.jstech.core.bigblock.AbstractBigBlock;
import dev.jstech.core.bigblock.IBigBlockGeometry;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.peripheral.PeripheralLine;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.util.BlockDrops;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The WAN Gateway Computer, a 4 x 2 x 2 big block: one block for every era, the era given where it is declared. The
 * origin, the block the player places, is the power button's; the rest are its parts ({@link WanGatewayPartBlock}),
 * four of which carry the ports of the long-distance line on their backs.
 */
public class WanGatewayBlock extends AbstractBigBlock implements IFaceConnector, IEraChassisBlock {

    private final Era era;
    /*
     * Like the Mainframe it sits on the backbone, on any face, its era's cable and every earlier one's, the fibre only
     * with an Optical Network Card; the origin stands in the front row, so it has no port of the long-distance line.
     */
    private final FacePorts ports;

    public static final MapCodec<WanGatewayBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(WanGatewayBlock::era)
    ).apply(i, WanGatewayBlock::new));
    /** The power button on the sill, in the middle of the origin's front, where the Mainframe has its own. */
    public static final FrontArea POWER_BUTTON = MainframeBlock.POWER_BUTTON;

    public WanGatewayBlock(final Properties properties, final Era era) {
        super(properties);
        this.era = era;
        this.ports = FacePorts.everyFace(DataLines.upTo(era, DataLine.BACKBONE), PeripheralLine.of(era));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(OpticalPort.OPTICAL, false));
    }

    /** The era of the gateway: its cabinet's model, the board it takes and the newest line its ports take. */
    public Era era() {
        return era;
    }

    @Override
    public Era chassisEra() {
        return era;
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return IFaceConnector.super.accepts(state, face, offered) && OpticalPort.admits(state, offered);
    }

    /** Says on the gateway and every part of it whether it holds an Optical Network Card. */
    public void setOptical(final Level level, final BlockPos origin, final boolean optical) {
        final BlockState state = level.getBlockState(origin);
        if (!(state.getBlock() instanceof WanGatewayBlock)) {
            return;
        }
        final List<BlockPos> blocks = new ArrayList<>(geometry().partPositions(origin, state.getValue(FACING)));
        blocks.add(origin);
        OpticalPort.set(level, blocks, OpticalPort.OPTICAL, optical);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction facing = context.getHorizontalDirection().getOpposite();
        final Level level = context.getLevel();
        final List<BlockPos> obstructed = getObstructedBlocks(level, context.getClickedPos(), facing);
        if (!obstructed.isEmpty()) {
            // No room for the 4 x 2 x 2 cabinet: nothing is placed, and what is in the way is shown.
            spawnMisplaceParticles(level, obstructed);
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new WanGatewayBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        return level.isClientSide() ? null : BlockEntityTickers.create(type, ComputingModule.WAN_GATEWAY_BE.get(),
                WanGatewayBlockEntity::serverTick);
    }

    /** Opens the gateway's assembly screen for {@code player}, from its origin or any of its parts. */
    public static void open(final ServerPlayer player, final BlockPos origin, final WanGatewayBlockEntity gateway) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new WanGatewayMenu(id, inventory, gateway),
                // Each era is its own machine and carries its own name in the screen's header.
                gateway.getBlockState().getBlock().getName()), buf -> buf.writeBlockPos(origin));
    }

    @Override
    protected MapCodec<? extends WanGatewayBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OpticalPort.OPTICAL);
    }

    @Override
    protected IBigBlockGeometry geometry() {
        return WanGatewayStructure.FOOTPRINT;
    }

    @Override
    protected boolean isOwnPart(final BlockState state) {
        return state.getBlock() instanceof WanGatewayPartBlock;
    }

    @Override
    protected boolean isOwnOrigin(final BlockState state) {
        return state.getBlock() instanceof WanGatewayBlock;
    }

    @Override
    protected BlockState partStateFor(final BlockPos origin, final Direction facing, final BlockPos part,
                                      final BlockState originState) {
        // Every part wears the origin's era, and the four of the bottom back row carry a port each.
        return ComputingModule.WAN_GATEWAY_PART.get().defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WanGatewayPartBlock.ERA, PartEras.valueFor(era))
                .setValue(WanGatewayPartBlock.PORT, WanGatewayStructure.portAt(origin, facing, part))
                .setValue(OpticalPort.OPTICAL, originState.hasProperty(OpticalPort.OPTICAL)
                        && originState.getValue(OpticalPort.OPTICAL));
    }

    @Override
    protected void dropContents(final ServerLevel level, final BlockPos origin) {
        // Its own item, so a broken gateway gives back the era it was.
        Block.popResource(level, origin, new ItemStack(asItem()));
        if (level.getBlockEntity(origin) instanceof WanGatewayBlockEntity gateway) {
            BlockDrops.spill(level, origin, gateway.getInventory());
        }
    }

    @Override
    protected void onOriginBroken(final ServerLevel level, final BlockPos origin) {
        if (level.getBlockEntity(origin) instanceof WanGatewayBlockEntity gateway) {
            gateway.onBroken(level);
        }
        ChunkAnchor.letGo(level, origin);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack held, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        // The power button on the sill answers whatever the hand holds, as the one on the assembly screen does.
        final double[] at = DeviceFront.point(state.getValue(FACING), pos, hit, player.getEyePosition(), 0);
        if (at == null || !POWER_BUTTON.contains(at[0], at[1])
                || !(level.getBlockEntity(pos) instanceof WanGatewayBlockEntity gateway)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            gateway.togglePower();
            gateway.showPowerPressed();
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        // Hardware only, as the Mainframe: its system is used on a linked monitor. Sneaking takes the panel off.
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof WanGatewayBlockEntity gateway) {
            if (player.isShiftKeyDown()) {
                gateway.toggleServicePanel();
            } else {
                open(serverPlayer, pos, gateway);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
