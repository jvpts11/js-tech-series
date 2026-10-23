/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws a Redstone Interface as the sensor of its era. Its lens shows the strength it reads or emits in the three
 * brightnesses the model is drawn in, dark at 0, half up to 8 and full from 9; the green lamp is lit while it reads
 * and the amber one while it emits. With no computer to power it, the lamps and the lens are dark.
 *
 * <p>Turned up or down it turns about the middle of its block, not of its floor, so its lens comes as close to the
 * block above or below as it comes to a wall rather than lying half through the block under it.
 */
public final class RedstoneInterfaceRenderer implements IKitDriver<RedstoneInterfaceBlockEntity> {

    /** The strongest signal the lens shows at half brightness; above it, full. */
    private static final int HALF_UP_TO = 8;

    private RedstoneInterfaceRenderer() {
    }

    /** The renderer of the Redstone Interfaces. */
    public static BlockEntityRenderer<RedstoneInterfaceBlockEntity> create(
            final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<RedstoneInterfaceBlockEntity>look(ComputingLooks.REDSTONE_INTERFACE)
                .driver(new RedstoneInterfaceRenderer())
                .upAndDownAboutMiddle()
                .build();
    }

    /* Nothing on it moves: its lens and lamps are bones shown and hidden. */
    @Override
    public void drive(final RedstoneInterfaceBlockEntity sensor, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final RedstoneInterfaceBlockEntity sensor, final ModelPose pose, final double seconds) {
        final int shown = sensor.shownStrength();
        DeviceLamps.show(pose, ComputingLooks.REDSTONE_LENS_DARK, shown == 0);
        DeviceLamps.show(pose, ComputingLooks.REDSTONE_LENS_HALF, shown > 0 && shown <= HALF_UP_TO);
        DeviceLamps.show(pose, ComputingLooks.REDSTONE_LENS_FULL, shown > HALF_UP_TO);
        DeviceLamps.show(pose, ComputingLooks.REDSTONE_LAMP_IN, sensor.live() && !sensor.emits());
        DeviceLamps.show(pose, ComputingLooks.REDSTONE_LAMP_OUT, sensor.live() && sensor.emits());
        // Its feet stand it on a floor; lens up or lens down there is no floor under them to stand on.
        DeviceLamps.show(pose, ComputingLooks.REDSTONE_FEET, facing(sensor).getAxis() != Direction.Axis.Y);
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    private static Direction facing(final RedstoneInterfaceBlockEntity sensor) {
        final BlockState state = sensor.getBlockState();
        return state.hasProperty(DirectionalBlock.FACING) ? state.getValue(DirectionalBlock.FACING) : Direction.NORTH;
    }
}
