/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import dev.jstech.core.api.client.GpuSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

/**
 * Draws a scene into a surface of its own, seen from a turn and a height and sized to fit, in full light and with no
 * fog: the manual's scenes of blocks, and the models in the preview of the Core's models. The surface keeps the
 * picture; whoever asks for it draws it again when what it shows changes, so a still scene costs nothing frame to
 * frame. What is drawn over the picture, as the balloons on a manual's scene or a locator's name, is put where the
 * scene shows it by {@link #project}.
 */
public final class SceneRenderer {

    /** How far before and behind its middle a scene may reach, in the surface's pixels. */
    private static final float DEPTH = 10000.0F;

    private SceneRenderer() {
    }

    /**
     * Draws what {@code painter} paints, seen as {@code view} says, into the surface, wiping what it held; the game's
     * own target is bound again after, with the projection and the lights the screen had.
     */
    public static void draw(final GpuSurface surface, final View view, final IScenePainter painter) {
        final Minecraft game = Minecraft.getInstance();
        final RenderTarget target = surface.target();
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, surface.width(), surface.height(), 0.0F,
                -DEPTH, DEPTH), VertexSorting.ORTHOGRAPHIC_Z);
        final Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();
        // A world's fog would grey a scene seen from this far; a page has none.
        FogRenderer.setupNoFog();
        Lighting.setupFor3DItems();
        game.gameRenderer.lightTexture().turnOnLightLayer();
        final PoseStack pose = new PoseStack();
        pose.translate(surface.width() / 2.0F + view.panX(), surface.height() / 2.0F + view.panY(), 0.0F);
        pose.scale(view.scale(), -view.scale(), view.scale());
        pose.mulPose(Axis.XP.rotationDegrees(view.pitch()));
        pose.mulPose(Axis.YP.rotationDegrees(view.yaw()));
        pose.translate(-view.middleX(), -view.middleY(), -view.middleZ());
        final MultiBufferSource.BufferSource buffers = game.renderBuffers().bufferSource();
        try {
            painter.paint(pose, buffers);
            buffers.endBatch();
        } finally {
            game.gameRenderer.lightTexture().turnOffLightLayer();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            game.getMainRenderTarget().bindWrite(true);
        }
    }

    /**
     * Where a point of the scene, in blocks, lands on a surface {@code width} by {@code height} pixels drawn as
     * {@code view} says, written into {@code out}: x and y in the surface's pixels from its top left, z its depth.
     */
    public static Vector3f project(final View view, final int width, final int height, final float x, final float y,
                                   final float z, final Vector3f out) {
        final Matrix4f matrix = new Matrix4f()
                .translate(width / 2.0F + view.panX(), height / 2.0F + view.panY(), 0.0F)
                .scale(view.scale(), -view.scale(), view.scale())
                .rotateX((float) Math.toRadians(view.pitch()))
                .rotateY((float) Math.toRadians(view.yaw()))
                .translate(-view.middleX(), -view.middleY(), -view.middleZ());
        return matrix.transformPosition(out.set(x, y, z));
    }

    /** Paints a scene into the buffers, in blocks, about the scene's own origin. */
    @FunctionalInterface
    public interface IScenePainter {

        void paint(PoseStack pose, MultiBufferSource.BufferSource buffers);
    }

    /**
     * How a scene is seen: its middle, in blocks, put at the middle of the surface and moved by a pan, in pixels; the
     * turn about its upright and the tilt toward the viewer, in degrees; how many pixels a block is.
     *
     * @param middleX the middle, in blocks
     * @param middleY the middle's height
     * @param middleZ the middle's depth
     * @param yaw     the turn about the upright
     * @param pitch   the tilt
     * @param scale   pixels to a block
     * @param panX    the pan across, in pixels
     * @param panY    the pan down, in pixels
     */
    public record View(float middleX, float middleY, float middleZ, float yaw, float pitch, float scale, float panX,
                       float panY) {

        /** A scene seen with its middle at {@code middle}, unpanned. */
        public static View of(final double[] middle, final float yaw, final float pitch, final float scale) {
            return new View((float) middle[0], (float) middle[1], (float) middle[2], yaw, pitch, scale, 0, 0);
        }
    }
}
