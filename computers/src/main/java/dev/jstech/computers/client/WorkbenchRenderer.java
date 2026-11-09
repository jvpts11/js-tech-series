/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Draws an Electronics Workbench as the one of its era, with the socket screwed to the back of its drawers and the cord
 * to it from where its energy line comes in behind it, shown only while a line is plugged in there.
 */
public final class WorkbenchRenderer implements IKitDriver<WorkbenchBlockEntity> {

    private static final String[] HOOKUPS = BackHookups.bones(ComputingLooks.COMPUTER_HOOKUP);

    private WorkbenchRenderer() {
    }

    /** The renderer of the benches. */
    public static BlockEntityRenderer<WorkbenchBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<WorkbenchBlockEntity>look(ComputingLooks.WORKBENCH)
                .driver(new WorkbenchRenderer())
                .build();
    }

    /* Nothing on it moves: its hookups are bones shown and hidden. */
    @Override
    public void drive(final WorkbenchBlockEntity bench, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final WorkbenchBlockEntity bench, final ModelPose pose, final double seconds) {
        final boolean[] plugged = BackHookups.plugged(bench.getLevel(), bench.getBlockPos(),
                bench.getBlockState().getValue(HorizontalDirectionalBlock.FACING), wire -> !DataWires.isNetwork(wire));
        for (int k = 0; k < HOOKUPS.length; k++) {
            DeviceLamps.show(pose, HOOKUPS[k], plugged[k]);
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }
}
