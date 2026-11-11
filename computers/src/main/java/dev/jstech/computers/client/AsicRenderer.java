/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.AsicBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws an ASIC (computers/docs/design/teracoin.md) as its model of its era: its chassis, its fan housings at either
 * end, the blades in them turning while it mines, and its running lamp lit then.
 */
public final class AsicRenderer implements IKitDriver<AsicBlockEntity> {

    private static final ModelFacts<PartModels.Spins> FANS = new ModelFacts<>(model -> PartModels.Spins.of(
            model.bones()));
    /** The lamp lit while it mines, a bone of its model. */
    private static final String RUNNING_LAMP = "lamp_running";
    /* Its fans turn three times a second, three times a computer's: an ASIC's run flat out. */
    private static final double SPEED = 3.0;

    private AsicRenderer() {
    }

    /** The renderer of the ASICs. */
    public static BlockEntityRenderer<AsicBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<AsicBlockEntity>look(ComputingLooks.ASIC).driver(new AsicRenderer()).build();
    }

    @Override
    public void drive(final AsicBlockEntity asic, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final AsicBlockEntity asic, final ModelPose pose, final double seconds) {
        final boolean mining = asic.mining();
        DeviceLamps.show(pose, RUNNING_LAMP, mining);
        if (mining) {
            FANS.of(pose.model()).turn(pose, PartModels.fanTurn(seconds * SPEED));
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }
}
