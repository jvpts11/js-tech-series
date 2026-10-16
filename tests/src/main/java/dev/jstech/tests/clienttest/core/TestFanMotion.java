/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.client.model.PartMotions;
import dev.jstech.tests.TestToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * The test fan part's blade, drawn each frame as another mod hands the Core a moving part: a bar of iron across the
 * face, turning while the fan is at work.
 */
public final class TestFanMotion {

    /** How far the blade turns a tick while the fan is at work, in degrees. */
    public static final float DEGREES_A_TICK = 18F;
    private static final float FULL_TURN = 360F;
    private static final float LENGTH = 0.75F;
    private static final float WIDTH = 0.125F;
    private static final float DEPTH = 0.0625F;

    private TestFanMotion() {
    }

    /** Hands the Core the blade's drawing, once the parts are registered, on the client. */
    public static void register(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> PartMotions.register(TestToolkit.FAN_PART.get(),
                TestFanMotion::draw));
    }

    private static void draw(final PoseStack pose, final MultiBufferSource buffers, final boolean busy,
                             final float time, final int light) {
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0F);
        pose.mulPose(Axis.ZP.rotationDegrees(busy ? time * DEGREES_A_TICK % FULL_TURN : 0F));
        pose.scale(LENGTH, WIDTH, DEPTH);
        pose.translate(-0.5F, -0.5F, 0F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.IRON_BLOCK.defaultBlockState(), pose,
                buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
