/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.block.WanGatewayStructure;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a WAN Gateway Computer as a cabinet does ({@link CabinetRenderer}), and its line bay the way a telecom shelf
 * shows its line cards: a line module for each port whose WAN Interface Card is in, a blank where none is, and on each
 * module the green lamp of a line up or, while the gateway runs, the red one of a line down.
 */
public final class WanGatewayRenderer extends CabinetRenderer<WanGatewayBlockEntity> {

    private static final String WORK_CLIP = "animation.wan_gateway.work";
    private static final String[] MODULES = DeviceLamps.numbered("line_", WanGatewayStructure.PORTS, 1);
    private static final String[] BLANKS = DeviceLamps.numbered("line_blank_", WanGatewayStructure.PORTS, 1);
    private static final String[] UP_LAMPS = DeviceLamps.numbered("line_ok_", WanGatewayStructure.PORTS, 1);
    private static final String[] DOWN_LAMPS = DeviceLamps.numbered("line_alarm_", WanGatewayStructure.PORTS, 1);

    private WanGatewayRenderer() {
        super(WORK_CLIP);
    }

    /** The renderer of the gateways; the cabinet spans sixteen blocks, so it is drawn within its whole box. */
    public static BlockEntityRenderer<WanGatewayBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return build(ComputingLooks.WAN_GATEWAY, new WanGatewayRenderer());
    }

    @Override
    protected void moveOwn(final WanGatewayBlockEntity gateway, final ModelPose pose) {
        final boolean running = gateway.visualRunning();
        for (int i = 0; i < WanGatewayStructure.PORTS; i++) {
            final int line = gateway.line(i + 1);
            DeviceLamps.show(pose, MODULES[i], line != WanGatewayBlockEntity.LINE_NONE);
            DeviceLamps.show(pose, BLANKS[i], line == WanGatewayBlockEntity.LINE_NONE);
            DeviceLamps.show(pose, UP_LAMPS[i], line == WanGatewayBlockEntity.LINE_UP);
            DeviceLamps.show(pose, DOWN_LAMPS[i], running && line == WanGatewayBlockEntity.LINE_DOWN);
        }
    }
}
