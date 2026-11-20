/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.plant.CduBlockEntity;
import dev.jstech.computers.plant.CoolingPlant;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws the CDU whole from its origin, as its approved design draws it (computers/docs/design/temperature.md): its
 * pumps turning, its water flowing and its lamps lit, its working loop, while it works, at rest otherwise.
 */
public final class CduRenderer implements IKitDriver<CduBlockEntity> {

    private static final String WORKING = "working";
    private static final double BLEND = 0.3;

    private CduRenderer() {
    }

    /** The renderer of the CDUs. */
    public static BlockEntityRenderer<CduBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<CduBlockEntity>of(CoolingPlant.CDU_MODEL).driver(new CduRenderer()).build();
    }

    @Override
    public void drive(final CduBlockEntity cdu, final ModelAnimator animator, final double seconds) {
        if (cdu.status() == MachineStatus.WORKING) {
            animator.play("main", WORKING, seconds);
        } else {
            animator.controller("main").stop(seconds, BLEND);
        }
    }

    @Override
    public void move(final CduBlockEntity cdu, final ModelPose pose, final double seconds) {
    }
}
