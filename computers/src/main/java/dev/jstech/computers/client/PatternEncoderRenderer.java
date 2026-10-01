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
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.os.media.MediaBay;
import dev.jstech.core.client.geo.LookGeoModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws a Pattern Encoder as its era's device. It states what it is doing with its lamps: the power lamp lit while a
 * computer is on the other end of its cable, and the activity lamp blinking while a pattern is written and held lit
 * on an error. The medium in the bay is the very item the player put in, drawn where the model marks its place: on
 * the tray, in the slot or in the port, moving with the clip that takes it in or out.
 */
public final class PatternEncoderRenderer extends GeoBlockRenderer<PatternEncoderBlockEntity> {

    public PatternEncoderRenderer(final BlockEntityRendererProvider.Context context) {
        super(new LookGeoModel<>(ComputingLooks.PATTERN_ENCODER));
        addRenderLayer(new BayMediumLayer<>(this, PatternEncoderBlockEntity::drawnMedium));
    }

    @Override
    public void preRender(final PoseStack poseStack, final PatternEncoderBlockEntity encoder,
                          final BakedGeoModel model, final MultiBufferSource bufferSource,
                          final VertexConsumer buffer, final boolean isReRender, final float partialTick,
                          final int packedLight, final int packedOverlay, final int colour) {
        super.preRender(poseStack, encoder, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
        DeviceLamps.show(model, MediaBay.POWER_LAMP, encoder.ownerPos() != null);
        DeviceLamps.show(model, MediaBay.BUSY_LAMP, activityLit(encoder));
    }

    /* The activity lamp: blinking while a pattern is written, held lit on an error, dark otherwise. */
    private static boolean activityLit(final PatternEncoderBlockEntity encoder) {
        if (encoder.busy()) {
            return DeviceLamps.blinkLit(encoder.getLevel());
        }
        return encoder.phase() == PatternEncoderBlockEntity.Phase.ERROR;
    }
}
