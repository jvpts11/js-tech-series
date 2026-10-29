/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.client.rocket.RocketPartsRenderer;
import dev.jstech.space.rocket.RocketPartModels;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestRockets;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The rocket stand's renderer: the rocket's formed parts standing on the stand, as J's Space draws them. */
@EventBusSubscriber(modid = JsTests.MODID, value = Dist.CLIENT)
public final class TestRocketRenderers {

    /* How wide and how tall a rocket on the stand may be drawn, in blocks. */
    private static final double WIDE = 8;
    private static final double TALL = 64;
    private static final int SEEN_FROM = 160;

    private TestRocketRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TestRockets.DISPLAY_ENTITY.get(), context -> new DisplayRenderer());
    }

    /* Draws the stand's rocket, its foot on the middle of the stand's floor. */
    private static final class DisplayRenderer implements BlockEntityRenderer<TestRockets.DisplayEntity> {

        @Override
        public void render(final TestRockets.DisplayEntity stand, final float partialTick, final PoseStack poses,
                           final MultiBufferSource buffers, final int light, final int overlay) {
            if (stand.getLevel() == null) {
                return;
            }
            poses.pushPose();
            poses.translate(0.5, 0, 0.5);
            RocketPartsRenderer.draw(stand, RocketPartModels.of(RocketParts.all(stand.getLevel()),
                    RocketKits.all(stand.getLevel()), stand.stack(), stand.legsDown()), poses, buffers, light,
                    overlay);
            poses.popPose();
        }

        @Override
        public AABB getRenderBoundingBox(final TestRockets.DisplayEntity stand) {
            return new AABB(stand.getBlockPos()).inflate(WIDE, 0, WIDE).expandTowards(0, TALL, 0);
        }

        @Override
        public int getViewDistance() {
            return SEEN_FROM;
        }
    }
}
