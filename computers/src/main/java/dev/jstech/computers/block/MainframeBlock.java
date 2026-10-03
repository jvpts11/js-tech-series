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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.menu.MainframeMenu;
import dev.jstech.core.multiblock.AbstractMultiblockControllerBlock;
import dev.jstech.core.multiblock.IMultiblockGeometry;
import dev.jstech.core.multiblock.MultiblockPatternGeometry;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.peripheral.PeripheralLine;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.BlockDrops;
import dev.jstech.core.util.BlockEntityTickers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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

import java.util.ArrayList;
import java.util.List;

/**
 * The Mainframe, the network's orchestrator: one block for every era, the era given where it is declared.
 */
public class MainframeBlock extends AbstractMultiblockControllerBlock
        implements IFaceConnector, IEraChassisBlock {

    private final HardwareEra era;
    /*
     * The Mainframe sits on the backbone, on any face, its era's cable and every earlier one's, the fibre only with an
     * Optical Network Card; it never takes the access line directly (a router joins that).
     */
    private final FacePorts ports;

    public static final MapCodec<MainframeBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(MainframeBlock::era)
    ).apply(i, MainframeBlock::new));

    public MainframeBlock(final Properties properties, final HardwareEra era) {
        super(properties);
        this.era = era;
        this.ports = FacePorts.everyFace(DataLines.upTo(era, DataLine.BACKBONE), PeripheralLine.of(era));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(OpticalPort.OPTICAL, false));
    }

    /**
     * The hardware era this Mainframe belongs to. It selects the cabinet's model and skin and gates which MTX board
     * installs: only a board of this same era is accepted and counted in the build.
     */
    public HardwareEra era() {
        return era;
    }

    @Override
    public HardwareEra chassisEra() {
        return era();
    }

    @Override
    protected MapCodec<? extends MainframeBlock> codec() {
        return CODEC;
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return IFaceConnector.super.accepts(state, face, offered) && OpticalPort.admits(state, offered);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OpticalPort.OPTICAL);
    }

    /** Says on the Mainframe and every part of it whether it holds an Optical Network Card. */
    public void setOptical(final Level level, final BlockPos controller, final boolean optical) {
        final BlockState state = level.getBlockState(controller);
        if (!(state.getBlock() instanceof MainframeBlock)) {
            return;
        }
        final List<BlockPos> blocks = new ArrayList<>(geometry().partPositions(controller, state.getValue(FACING)));
        blocks.add(controller);
        OpticalPort.set(level, blocks, optical);
    }

    @Override
    protected IMultiblockGeometry geometry() {
        return new MultiblockPatternGeometry(MainframeStructure.PATTERN);
    }

    @Override
    protected boolean isOwnPart(final BlockState state) {
        return state.getBlock() instanceof MainframePartBlock;
    }

    @Override
    protected boolean isOwnController(final BlockState state) {
        return state.getBlock() instanceof MainframeBlock;
    }

    @Override
    protected BlockState partStateFor(final BlockPos controller, final Direction facing,
                                      final BlockPos part, final BlockState controllerState) {
        // Stamp the controller's era onto every structural part so the whole footprint wears one skin.
        final HardwareEra era =
                controllerState.getBlock() instanceof MainframeBlock mf
                        ? mf.era()
                        : HardwareEra.STANDARD;
        return ComputingModule.MAINFRAME_PART.get().defaultBlockState()
                .setValue(FACING, facing)
                .setValue(MainframePartBlock.CORE,
                        MainframeStructure.isCentralColumn(controller, facing, part))
                .setValue(MainframePartBlock.ERA, era.level())
                .setValue(OpticalPort.OPTICAL, controllerState.hasProperty(OpticalPort.OPTICAL)
                        && controllerState.getValue(OpticalPort.OPTICAL));
    }

    @Override
    protected void dropContents(final ServerLevel level, final BlockPos controller) {
        // Its own item, so a broken Mainframe gives back the era it was.
        Block.popResource(level, controller, new ItemStack(asItem()));
        if (level.getBlockEntity(controller) instanceof MainframeBlockEntity be) {
            BlockDrops.spill(level, controller, be.getInventory());
        }
    }

    @Override
    protected void onControllerBroken(final ServerLevel level, final BlockPos controller) {
        if (level.getBlockEntity(controller) instanceof MainframeBlockEntity be) {
            be.onBroken();
        }
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction facing = context.getHorizontalDirection().getOpposite();
        final Level level = context.getLevel();
        final List<BlockPos> obstructedBlocks = getObstructedBlocks(level, context.getClickedPos(), facing);
        if (!obstructedBlocks.isEmpty()) {
            /*
             * No room for the 3x2x2 structure, so cancel placement, item not consumed, and outline the
             * obstructing cells with particles so the player can see what is in the way.
             */
            spawnMisplaceParticles(level, obstructedBlocks);
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        /*
         * The Mainframe block is hardware only: clicking it always opens the hardware-assembly GUI
         * (the same one its parts open). All software (firmware, OS) is used on a linked monitor.
         */
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe) {
            /*
             * Sneaking takes the service panel off the card bay (or puts it back): the processors,
             * memory and cards are only ever seen through that opening.
             */
            if (player.isShiftKeyDown()) {
                mainframe.toggleServicePanel();
                return InteractionResult.sidedSuccess(false);
            }
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new MainframeMenu(
                                    id, inventory, mainframe),
                            // Each era is its own machine and carries its own name in the GUI header.
                            state.getBlock().getName()),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new MainframeBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.MAINFRAME_BE.get(),
                MainframeBlockEntity::serverTick);
    }

}
