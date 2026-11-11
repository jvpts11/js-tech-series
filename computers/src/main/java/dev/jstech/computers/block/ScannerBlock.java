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
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.blockentity.ScannerBlockEntity;
import dev.jstech.computers.menu.ScannerMenu;
import dev.jstech.computers.scan.ScannerModel;
import dev.jstech.computers.scan.Scans;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A flatbed scanner, one for each era (computers/docs/design/peripherals.md): a click with something it reads lays it on
 * the glass; a plain click opens its window, with the glass, Scan and Stop, and where the file goes. It links to a
 * computer as the printer does, against it or on a peripheral cable into the port of its era on its back.
 */
@TextHolder
public class ScannerBlock extends DeviceBlock implements IFaceConnector, IEraChassisBlock {

    private final Era era;
    private final FacePorts ports;
    private final Map<Direction, VoxelShape> shapes;

    public static final MapCodec<ScannerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(b -> b.era)
    ).apply(i, ScannerBlock::new));

    /** The scanner's block entity, ticking its link and its pages, with its window as its menu. */
    private static final Device<ScannerBlockEntity> DEVICE = Device.of(() -> MediaDevices.SCANNER_BE.get())
            .ticks(ScannerBlockEntity::serverTick)
            .opensMenu(ScannerMenu::new);
    private static final TextKey GLASS_TAKEN = TextKey.of("block.jsc.scanner.glass_taken",
            "Something already lies on the glass");
    /* The body and its closed lid, as it stands facing north, its hinges at the back; in sixteenths. */
    private static final double LEFT = 1;
    private static final double RIGHT = 15;
    private static final double FRONT = 1;
    private static final double BACK = 12;

    public ScannerBlock(final Properties properties, final Era era) {
        super(properties, DEVICE);
        this.era = era;
        this.ports = PeripheralSockets.back(era);
        this.shapes = FacingShapes.of(new double[] {LEFT, 0, FRONT, RIGHT, height(era), BACK});
    }

    /** The scanner this block is. */
    public ScannerModel model() {
        return ScannerModel.of(era);
    }

    public Era era() {
        return era;
    }

    @Override
    public Era chassisEra() {
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

    /** The scanner is one model drawn by the block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    /* Something it reads, in hand, goes on the glass when the glass is free; anything else opens the window. */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!Scans.reads(heldStack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ScannerBlockEntity scanner) {
            final ItemStack left = scanner.glass().insertItem(0, heldStack.copyWithCount(1), false);
            if (left.isEmpty()) {
                heldStack.shrink(1);
            } else {
                player.displayClientMessage(GameText.component(GLASS_TAKEN), true);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    /* How tall the body and its closed lid stand, in sixteenths: each era's scanner as its model is drawn. */
    private static double height(final Era era) {
        final Era parts = HardwareEra.withParts(era);
        if (parts == HardwareEra.VINTAGE) {
            return 6.5;
        }
        if (parts == HardwareEra.LEGACY) {
            return 4.5;
        }
        if (parts == HardwareEra.TRANSITION) {
            return 4;
        }
        return parts == HardwareEra.STANDARD ? 6 : 3;
    }
}
