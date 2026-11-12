/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.UpsBlockEntity;
import dev.jstech.computers.power.UpsReading;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws an MF PowerGuard as the UPS of its era, its lamps showing how it stands: the green one while the line carries
 * the load, blinking in bypass as a real one warns of an overload; the amber one while the battery carries it; and the
 * red one that asks for a new battery once the battery is near its end, down to where the system shuts itself down.
 */
public final class UpsRenderer implements IKitDriver<UpsBlockEntity> {

    private UpsRenderer() {
    }

    /** The renderer of the MF PowerGuards. */
    public static BlockEntityRenderer<UpsBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<UpsBlockEntity>look(ComputingLooks.UPS)
                .driver(new UpsRenderer())
                .build();
    }

    /* Nothing on it moves: its lamps are bones shown and hidden. */
    @Override
    public void drive(final UpsBlockEntity ups, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final UpsBlockEntity ups, final ModelPose pose, final double seconds) {
        final UpsReading.Mode mode = ups.reading().mode();
        final boolean line = mode == UpsReading.Mode.ON_LINE
                || mode == UpsReading.Mode.BYPASS && DeviceLamps.blinkLit(ups.getLevel());
        DeviceLamps.show(pose, ComputingLooks.UPS_LINE_LAMP, line);
        DeviceLamps.show(pose, ComputingLooks.UPS_BATTERY_LAMP, mode == UpsReading.Mode.ON_BATTERY);
        DeviceLamps.show(pose, ComputingLooks.UPS_REPLACE_LAMP, ups.reading().nearEnd());
    }

    @Override
    public boolean movesBones() {
        return true;
    }
}
