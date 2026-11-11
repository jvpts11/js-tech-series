/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.ExtendedCdDriveBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws an Extended CD Drive as its era's duplicator tower, two blocks tall, from its lower block: the reading tray's
 * lamp blinks while it reads; each writing tray's blinks while it writes and stays lit once its copy is done.
 */
public final class ExtendedCdDriveRenderer implements IKitDriver<ExtendedCdDriveBlockEntity> {

    private ExtendedCdDriveRenderer() {
    }

    /** The renderer of the towers. */
    public static BlockEntityRenderer<ExtendedCdDriveBlockEntity> create(
            final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<ExtendedCdDriveBlockEntity>look(ComputingLooks.EXTENDED_CD_DRIVE)
                .driver(new ExtendedCdDriveRenderer())
                .build();
    }

    /* Nothing on it moves: its lamps are bones shown and hidden. */
    @Override
    public void drive(final ExtendedCdDriveBlockEntity tower, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final ExtendedCdDriveBlockEntity tower, final ModelPose pose, final double seconds) {
        final boolean blink = DeviceLamps.blinkLit(tower.getLevel());
        DeviceLamps.show(pose, ComputingLooks.EXTENDED_CD_DRIVE_LAMPS.getFirst(),
                tower.work() == ExtendedCdDriveBlockEntity.Work.READING && blink);
        for (int tray = 0; tray < ExtendedCdDriveBlockEntity.TRAYS; tray++) {
            final boolean writing = tower.work() == ExtendedCdDriveBlockEntity.Work.WRITING && tower.writing(tray);
            DeviceLamps.show(pose, ComputingLooks.EXTENDED_CD_DRIVE_LAMPS.get(tray + 1),
                    tower.written(tray) || writing && blink);
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }
}
