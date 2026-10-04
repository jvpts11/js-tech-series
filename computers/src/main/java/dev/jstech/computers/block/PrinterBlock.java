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
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.menu.PrinterMenu;
import dev.jstech.computers.printer.PrinterModel;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A printer, one for each era, generic: it prints whatever a program of its computer sends it. A click with paper fills
 * its tray; a plain click opens its window, with the tray, the job printing, the queue and the sheets that came out.
 * It links to a computer as a drive does, against it or on a peripheral cable into the port of its era on its back.
 */
public class PrinterBlock extends DeviceBlock implements IFaceConnector, IEraChassisBlock {

    private final HardwareEra era;
    private final FacePorts ports;

    public static final MapCodec<PrinterBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(b -> b.era)
    ).apply(i, PrinterBlock::new));

    /** The printer's block entity, ticking its link and its pages, with its window as its menu. */
    private static final Device<PrinterBlockEntity> DEVICE = Device.of(() -> ComputingModule.PRINTER_BE.get())
            .ticks(PrinterBlockEntity::serverTick)
            .opensMenu(PrinterMenu::new);

    public PrinterBlock(final Properties properties, final HardwareEra era) {
        super(properties, DEVICE);
        this.era = era;
        this.ports = PeripheralSockets.back(era);
    }

    /** The printer this block is. */
    public PrinterModel model() {
        return PrinterModel.of(era);
    }

    public HardwareEra era() {
        return era;
    }

    @Override
    public HardwareEra chassisEra() {
        return era;
    }

    /** The device port of its era, in the middle of its back. */
    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** The printer is one model drawn by the block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /* Each printer stands on a base that fills its block, so the shape is the plain cube. */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return Shapes.block();
    }

    /* Paper in hand goes into the tray, as much as it takes; anything else opens the window. */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!heldStack.is(Items.PAPER)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) {
            final ItemStack left = printer.paper().insertItem(0, heldStack.copy(), false);
            heldStack.setCount(left.getCount());
        }
        return ItemInteractionResult.SUCCESS;
    }
}
