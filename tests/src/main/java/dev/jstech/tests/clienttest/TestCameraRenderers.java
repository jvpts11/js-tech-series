/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.camera.CameraFeeds;
import dev.jstech.core.client.live.LiveScreen;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.live.LiveRate;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCameras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The test camera screen's renderer, as a mod draws a screen showing a camera: a picture on its north face, painted
 * by the world's screens with the camera's view.
 */
@EventBusSubscriber(modid = JsTests.MODID, value = Dist.CLIENT)
public final class TestCameraRenderers {

    /* The picture's size, in the units it is painted in, and how far it stands off the face. */
    private static final int WIDTH = 64;
    private static final int HEIGHT = 48;
    private static final float OFF_FACE = 0.005F;

    private TestCameraRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TestCameras.SCREEN_ENTITY.get(), context -> new ScreenRenderer());
    }

    /* The camera's view on the north face, whole, upright as seen from the north. */
    private static final class ScreenRenderer implements BlockEntityRenderer<TestCameras.ScreenEntity> {

        @Override
        public void render(final TestCameras.ScreenEntity screen, final float partialTick, final PoseStack pose,
                           final MultiBufferSource buffers, final int light, final int overlay) {
            final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            final double distance = eye.distanceToSqr(Vec3.atCenterOf(screen.getBlockPos()));
            if (!LiveRate.shows(distance)) {
                return;
            }
            final LiveScreen picture = LiveScreens.ask(screen.getBlockPos(), (g, w, h, partial) ->
                    CameraFeeds.show(g, TestCameras.feedOf(screen.getBlockPos()), 0, 0, w, h), WIDTH, HEIGHT, 1,
                    Tube.COLOUR, distance);
            pose.pushPose();
            pose.translate(1.0F, 1.0F, -OFF_FACE);
            pose.scale(-1.0F / WIDTH, -1.0F / HEIGHT, 1.0F);
            LiveScreens.draw(pose, buffers, picture, 0, 0, WIDTH, HEIGHT, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
}
