/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.heat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.heat.Blackbody;
import dev.jstech.core.heat.IHeatHolder;
import dev.jstech.core.heat.ThermalSight;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Hot blocks glowing as hot things do: a block that holds heat ({@link IHeatHolder}) past the Draper point glows in
 * the colour of a black body at its temperature, a dull red first, then orange and yellow going white, more strongly
 * the hotter it is, its own light laid over its block's face whatever light falls on it, so it glows as brightly by
 * night as by day. A run of hot blocks glows as one shape. While the player sees heat, the thermal view shows them
 * instead.
 */
public final class HeatGlowClient {

    /* How much of its block's face a glow at its whole covers, and how far out from the block it stands, in blocks. */
    private static final float MOST = 0.75F;
    private static final double GROW = 0.004;
    private static final int RGB = 0xFFFFFF;
    private static final int ALPHA_SHIFT = 24;
    private static final int CHANNEL = 255;

    private HeatGlowClient() {
    }

    /** Hands the game's bus what this draws, once, from the Core's client. */
    public static void listen() {
        HeatScan.listen();
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, HeatGlowClient::onRender);
    }

    /** The blocks near the player hot enough to glow, and how hot each is in kelvin, as last looked for. */
    public static Map<BlockPos, Double> hot() {
        return HeatScan.hot();
    }

    private static void onRender(final RenderLevelStageEvent event) {
        final List<HeatScan.Found> glowing = HeatScan.glowing();
        final Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || glowing.isEmpty()
                || minecraft.player == null || ThermalSight.sees(minecraft.player)) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack poses = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        final PoseStack.Pose pose = poses.last();
        for (final HeatScan.Found hot : glowing) {
            final int alpha = Math.round((float) Blackbody.glow(hot.kelvin()) * MOST * CHANNEL);
            final int colour = alpha << ALPHA_SHIFT | Blackbody.colour(hot.kelvin()) & RGB;
            HeatBoxes.block(consumer, pose, hot.pos(), GROW, colour, HeatScan::glowsAt);
        }
        poses.popPose();
        buffers.endBatch(RenderType.debugQuads());
    }
}
