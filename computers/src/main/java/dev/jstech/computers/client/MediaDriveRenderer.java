/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.computers.os.media.MediaBay;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a floppy, CD or DVD drive as the drive of its day. The power lamp is lit while a computer is at the other
 * end of its cable, and the activity lamp blinks while that computer reads the drive. The medium in it is the very
 * item the player put in, and an optical drive's tray rides out and in as its button is pressed. The Dock Station is a
 * reader too, with a block entity and a renderer of its own.
 */
public final class MediaDriveRenderer implements IKitDriver<MediaReaderBlockEntity>,
        IKitExtras<MediaReaderBlockEntity> {

    private static final DiscTray.Clips TRAY = DiscTray.Clips.of("media_drive");

    private MediaDriveRenderer() {
    }

    /** The renderer of the drives, drawn only in the forms whose block draws no model of its own. */
    public static BlockEntityRenderer<MediaReaderBlockEntity> create(
            final BlockEntityRendererProvider.Context context) {
        final MediaDriveRenderer drawn = new MediaDriveRenderer();
        return KitBlockEntityRenderer.<MediaReaderBlockEntity>look(ComputingLooks.MEDIA_DRIVE)
                .driver(drawn)
                .extras(drawn)
                .drawnWhile(MediaReaderBlockEntity::modelled)
                .build();
    }

    /* A floppy's way in or out through its slot is the server's to play; the tray rides as its button is pressed. */
    @Override
    public void drive(final MediaReaderBlockEntity drive, final ModelAnimator animator, final double seconds) {
        DeviceClips.play(animator, DiscTray.CONTROLLER, drive.tray().playing(TRAY, drive.getLevel()), seconds);
    }

    @Override
    public void move(final MediaReaderBlockEntity drive, final ModelPose pose, final double seconds) {
        DeviceLamps.show(pose, MediaBay.POWER_LAMP, drive.ownerPos() != null);
        DeviceLamps.show(pose, MediaBay.BUSY_LAMP, drive.reading() && DeviceLamps.blinkLit(drive.getLevel()));
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /* No drive has a USB port, so how one would lie never comes up. */
    @Override
    public void draw(final MediaReaderBlockEntity drive, final KitInstance instance, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
        BayMedium.draw(drive.drawnMedium(), false, instance, poses, buffers, light, overlay, drive.getLevel());
    }
}
