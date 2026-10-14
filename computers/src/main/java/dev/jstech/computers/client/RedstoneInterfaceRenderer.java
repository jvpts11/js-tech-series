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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final double HALF_BLOCK = 0.5;

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
        // Its feet stand it on a floor; lens up or lens down there is no floor under them to stand on.
        DeviceLamps.show(model, ComputingLooks.REDSTONE_FEET, facing(sensor).getAxis() != Direction.Axis.Y);
    }

    /*
     * GeckoLib turns a block about the middle of its floor, which is right for the four sides but would lay a sensor
     * turned up or down half through the block under it. Up or down it turns about the middle of its block, so its lens
     * comes as close to the block above or below as it comes to a wall.
     */
    @Override
    protected void rotateBlock(final Direction facing, final PoseStack poseStack) {
        final boolean upright = facing.getAxis() == Direction.Axis.Y;
        if (upright) {
            poseStack.translate(0.0, HALF_BLOCK, 0.0);
        }
        super.rotateBlock(facing, poseStack);
        if (upright) {
            poseStack.translate(0.0, -HALF_BLOCK, 0.0);
        }
    }

    private static Direction facing(final RedstoneInterfaceBlockEntity sensor) {
        final BlockState state = sensor.getBlockState();
        return state.hasProperty(DirectionalBlock.FACING) ? state.getValue(DirectionalBlock.FACING) : Direction.NORTH;
    }
}
