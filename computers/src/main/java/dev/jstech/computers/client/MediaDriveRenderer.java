/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.os.media.MediaBay;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.client.geo.LookGeoModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws a floppy, CD or DVD drive as the drive of its day. The power lamp is lit while a computer is at the other
 * end of its cable, and the activity lamp blinks while that computer reads the drive. The medium in it is the very
 * item the player put in. The Dock Station is a reader too, with a block entity and a renderer of its own.
 */
public final class MediaDriveRenderer extends GeoBlockRenderer<MediaReaderBlockEntity> {

    public MediaDriveRenderer(final BlockEntityRendererProvider.Context context) {
        super(new LookGeoModel<>(ComputingLooks.MEDIA_DRIVE));
        addRenderLayer(new BayMediumLayer<>(this, MediaReaderBlockEntity::drawnMedium));
    }

    @Override
    public void render(final MediaReaderBlockEntity drive, final float partialTick, final PoseStack poseStack,
                       final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
        if (drive.modelled()) {
            super.render(drive, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    @Override
    public void preRender(final PoseStack poseStack, final MediaReaderBlockEntity drive, final BakedGeoModel model,
                          final MultiBufferSource bufferSource, final VertexConsumer buffer, final boolean isReRender,
                          final float partialTick, final int packedLight, final int packedOverlay, final int colour) {
        super.preRender(poseStack, drive, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        DeviceLamps.show(model, MediaBay.POWER_LAMP, drive.ownerPos() != null);
        DeviceLamps.show(model, MediaBay.BUSY_LAMP, drive.reading() && DeviceLamps.blinkLit(drive.getLevel()));
    }
}
