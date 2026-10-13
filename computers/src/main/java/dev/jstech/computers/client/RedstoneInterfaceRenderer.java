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
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.core.client.geo.LookGeoModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws a Redstone Interface as the sensor of its era. Its lens shows the strength it reads or emits in the three
 * brightnesses the model is drawn in, dark at 0, half up to 8 and full from 9; the green lamp is lit while it reads
 * and the amber one while it emits. With no computer to power it, the lamps and the lens are dark.
 */
public final class RedstoneInterfaceRenderer extends GeoBlockRenderer<RedstoneInterfaceBlockEntity> {

    /** The strongest signal the lens shows at half brightness; above it, full. */
    private static final int HALF_UP_TO = 8;

    public RedstoneInterfaceRenderer(final BlockEntityRendererProvider.Context context) {
        super(new LookGeoModel<>(ComputingLooks.REDSTONE_INTERFACE));
    }

    @Override
    public void preRender(final PoseStack poseStack, final RedstoneInterfaceBlockEntity sensor,
                          final BakedGeoModel model, final MultiBufferSource bufferSource, final VertexConsumer buffer,
                          final boolean isReRender, final float partialTick, final int packedLight,
                          final int packedOverlay, final int colour) {
        super.preRender(poseStack, sensor, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        final int shown = sensor.shownStrength();
        DeviceLamps.show(model, ComputingLooks.REDSTONE_LENS_DARK, shown == 0);
        DeviceLamps.show(model, ComputingLooks.REDSTONE_LENS_HALF, shown > 0 && shown <= HALF_UP_TO);
        DeviceLamps.show(model, ComputingLooks.REDSTONE_LENS_FULL, shown > HALF_UP_TO);
        DeviceLamps.show(model, ComputingLooks.REDSTONE_LAMP_IN, sensor.live() && !sensor.emits());
        DeviceLamps.show(model, ComputingLooks.REDSTONE_LAMP_OUT, sensor.live() && sensor.emits());
    }
}
