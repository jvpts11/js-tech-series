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
import dev.jstech.computers.audio.VinylDiscs;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Vinyl RW Drive, one for each era (computers/docs/design/peripherals.md): a click with a music disc or a blank vinyl
 * lays it on the platter, an empty hand takes it off; Soundfoundry, on the computer at the cable's other end, records
 * the disc and cuts onto the blank. It links to a computer as a drive does, against it or on a peripheral cable into the
 * port of its era on its back.
 */
public class VinylDriveBlock extends DeviceBlock implements IFaceConnector, IEraChassisBlock {

    private final Era era;
    private final FacePorts ports;

    public static final MapCodec<VinylDriveBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(b -> b.era)
    ).apply(i, VinylDriveBlock::new));

    /** The drive's block entity, ticking its link and its work; it has no window of its own. */
    private static final Device<VinylDriveBlockEntity> DEVICE = Device.of(() -> MediaDevices.VINYL_DRIVE_BE.get())
            .ticks(VinylDriveBlockEntity::serverTick);

    public VinylDriveBlock(final Properties properties, final Era era) {
        super(properties, DEVICE);
        this.era = era;
        this.ports = PeripheralSockets.back(era);
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

    /** The drive is one model drawn by the block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /* Each drive stands on a plinth that fills its block, so the shape is the plain cube. */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return Shapes.block();
    }

    /* A disc or a blank vinyl in hand goes on a free platter. */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!VinylDiscs.playable(heldStack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VinylDriveBlockEntity drive
                && drive.work() == VinylDriveBlockEntity.Work.NONE && drive.onPlatter().isEmpty()) {
            drive.platter().setStackInSlot(0, heldStack.split(1));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /* An empty hand takes the record off the platter, unless the drive is at work on it. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof VinylDriveBlockEntity drive
                && drive.work() == VinylDriveBlockEntity.Work.NONE && !drive.onPlatter().isEmpty()) {
            player.getInventory().placeItemBackInInventory(drive.platter().extractItem(0, 1, false));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
