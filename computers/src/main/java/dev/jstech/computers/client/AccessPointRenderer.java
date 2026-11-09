/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Draws a Wireless Access Point as the one of its era, fixed where it was placed. A desk one lights its power lamp
 * while it has its energy and blinks its Wi-Fi and network lamps while it is on a network, and shows a hookup where
 * each of its two lines comes in behind it: the energy's cord, and the data's in its era's access cable's colour.
 */
public final class AccessPointRenderer implements IKitDriver<AccessPointBlockEntity> {

    private static final String[] ENERGY_HOOKUPS = BackHookups.bones(ComputingLooks.COMPUTER_HOOKUP);
    private static final String[] DATA_HOOKUPS = BackHookups.bones(ComputingLooks.ACCESS_POINT_DATA_HOOKUP);
    /* The two traffic lamps blink out of step, as a real one's do. */
    private static final long LAN_LAG_TICKS = 3L;

    private AccessPointRenderer() {
    }

    /** The renderer of the access points. */
    public static BlockEntityRenderer<AccessPointBlockEntity> create(
            final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<AccessPointBlockEntity>look(ComputingLooks.ACCESS_POINT)
                .driver(new AccessPointRenderer())
                .build();
    }

    /* Nothing on it moves: its lamps and hookups are bones shown and hidden. */
    @Override
    public void drive(final AccessPointBlockEntity point, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final AccessPointBlockEntity point, final ModelPose pose, final double seconds) {
        if (point.getBlockState().getValue(AccessPointBlock.MOUNT) != AccessPointMount.DESK) {
            return;
        }
        final Level level = point.getLevel();
        final boolean on = point.isOn();
        final boolean traffic = on && point.isLinked();
        DeviceLamps.show(pose, ComputingLooks.ACCESS_POINT_POWER_LAMP, on);
        DeviceLamps.show(pose, ComputingLooks.ACCESS_POINT_WLAN_LAMP, traffic && DeviceLamps.blinkLit(level));
        DeviceLamps.show(pose, ComputingLooks.ACCESS_POINT_LAN_LAMP, traffic
                && DeviceLamps.blinkLit(level, LAN_LAG_TICKS));
        final Direction facing = point.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        show(pose, ENERGY_HOOKUPS, BackHookups.plugged(level, point.getBlockPos(), facing,
                wire -> !DataWires.isNetwork(wire)));
        show(pose, DATA_HOOKUPS, BackHookups.plugged(level, point.getBlockPos(), facing, DataWires::isNetwork));
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    private static void show(final ModelPose pose, final String[] bones, final boolean[] plugged) {
        for (int k = 0; k < bones.length; k++) {
            DeviceLamps.show(pose, bones[k], plugged[k]);
        }
    }
}
