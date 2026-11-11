/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a Vinyl RW Drive as its era's record cutter: the record on its platter while one lies there, turning at
 * thirty-three and a third while the drive records or cuts, its recording lamp lit (blinking while it records, as a
 * cutter's does while the lathe runs) and, on the Standard's, its ring of light round the platter.
 */
public final class VinylDriveRenderer implements IKitDriver<VinylDriveBlockEntity> {

    private static final String SPIN = "vinyl_spin";

    private VinylDriveRenderer() {
    }

    /** The renderer of the drives. */
    public static BlockEntityRenderer<VinylDriveBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<VinylDriveBlockEntity>look(ComputingLooks.VINYL_DRIVE)
                .driver(new VinylDriveRenderer())
                .build();
    }

    @Override
    public void drive(final VinylDriveBlockEntity drive, final ModelAnimator animator, final double seconds) {
        final boolean turning = drive.work() != VinylDriveBlockEntity.Work.NONE;
        DeviceClips.play(animator, SPIN, turning ? "animation.vinyl_rw_drive.spin_"
                + HardwareEra.withParts(drive.era()).path() : null, seconds);
    }

    @Override
    public void move(final VinylDriveBlockEntity drive, final ModelPose pose, final double seconds) {
        final boolean working = drive.work() != VinylDriveBlockEntity.Work.NONE;
        DeviceLamps.show(pose, ComputingLooks.VINYL_RECORD, !drive.onPlatter().isEmpty());
        DeviceLamps.show(pose, ComputingLooks.VINYL_REC_LAMP, drive.work() == VinylDriveBlockEntity.Work.RECORDING
                ? DeviceLamps.blinkLit(drive.getLevel()) : working);
        DeviceLamps.show(pose, ComputingLooks.VINYL_RING_LAMP, working);
    }

    @Override
    public boolean movesBones() {
        return true;
    }
}
