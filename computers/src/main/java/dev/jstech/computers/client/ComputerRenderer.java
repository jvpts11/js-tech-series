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
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.core.client.geo.LookGeoModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws a small computer as its case: the tower of its age, or the one of three cases a later machine comes in, its
 * front toward whoever placed it. Its lamps stay dark: the case shows the machine as it stands, switched off.
 *
 * @param <T> which of the small computers
 */
public final class ComputerRenderer<T extends AbstractSmallComputerBlockEntity> extends GeoBlockRenderer<T> {

    public ComputerRenderer(final BlockEntityRendererProvider.Context context) {
        super(new LookGeoModel<>(ComputingLooks.COMPUTER));
    }

    @Override
    public void preRender(final PoseStack poseStack, final T computer, final BakedGeoModel model,
                          final MultiBufferSource bufferSource, final VertexConsumer buffer, final boolean isReRender,
                          final float partialTick, final int packedLight, final int packedOverlay, final int colour) {
        super.preRender(poseStack, computer, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        DeviceLamps.show(model, ComputingLooks.COMPUTER_POWER_LAMP, false);
        DeviceLamps.show(model, ComputingLooks.COMPUTER_DISK_LAMP, false);
    }
}
