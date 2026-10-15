/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.jstech.core.log.CoreLogs;
import com.mojang.math.Axis;
import dev.jstech.core.api.client.GpuSurface;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.SceneProjection;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.slf4j.Logger;

/**
 * Draws a scene into a surface of its own as the game draws blocks in its world: each block's model shaded as its
 * neighbours shade it, and each block entity through its renderer, GeckoLib's models among them, all in full light.
 * The surface keeps the picture, and a scene is drawn again only when what it shows changes, so a scene on an open
 * page costs nothing frame to frame.
 *
 * <p>The scene is turned and scaled by the same sums {@link SceneProjection} works with, so what is drawn on the frame
 * over it (the balloons, the outline of a part) lands where the part is drawn.
 */
final class SceneRenderer {

    private static final Logger LOGGER = CoreLogs.GUIDE.logger();
    /** How far before and behind its middle a scene may reach, in the surface's pixels. */
    private static final float DEPTH = 10000.0F;
    /** What every block's random choice of model is made with, so a scene looks the same each time it is drawn. */
    private static final long SEED = 42L;

    private SceneRenderer() {
    }

    /**
     * Draws the blocks a scene's world shows, seen that way, into the surface, wiping what it held; the game's own
     * target is bound again after, with the projection and the lights the screen had.
     */
    static void draw(final SceneLevel level, final GuideBlock.Scene scene, final GuideBlock.SceneView view,
                     final GpuSurface surface) {
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
        final SceneProjection projection = new SceneProjection(scene, view, surface.width(), surface.height());
        final PoseStack pose = new PoseStack();
        pose.translate(surface.width() / 2.0F, surface.height() / 2.0F, 0.0F);
        final float scale = (float) projection.scale();
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(view.pitch()));
        pose.mulPose(Axis.YP.rotationDegrees(view.yaw()));
        final double[] middle = projection.middle();
        pose.translate(-middle[0], -middle[1], -middle[2]);
        final MultiBufferSource.BufferSource buffers = game.renderBuffers().bufferSource();
        try {
            blocks(level, pose, buffers);
            entities(level, pose, buffers);
            buffers.endBatch();
        } finally {
            game.gameRenderer.lightTexture().turnOffLightLayer();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            game.getMainRenderTarget().bindWrite(true);
        }
    }

    /** Every block's model, in each of the kinds of layer it is drawn in, shaded by the blocks round it. */
    private static void blocks(final SceneLevel level, final PoseStack pose, final MultiBufferSource buffers) {
        final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        final RandomSource random = RandomSource.create();
        for (final Map.Entry<BlockPos, BlockState> placed : level.shownStates().entrySet()) {
            final BlockPos pos = placed.getKey();
            final BlockState state = placed.getValue();
            if (state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            final BakedModel model = dispatcher.getBlockModel(state);
            final BlockEntity entity = level.getBlockEntity(pos);
            final ModelData data = model.getModelData(level, pos, state,
                    entity == null ? ModelData.EMPTY : entity.getModelData());
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            random.setSeed(SEED);
            for (final RenderType type : model.getRenderTypes(state, random, data)) {
                random.setSeed(SEED);
                dispatcher.renderBatched(state, pos, level, pose, buffers.getBuffer(type), true, random, data, type);
            }
            pose.popPose();
        }
    }

    /** Every block entity through its renderer, in full light; one that fails is left out with a line in the log. */
    private static void entities(final SceneLevel level, final PoseStack pose, final MultiBufferSource buffers) {
        for (final BlockPos pos : level.shownStates().keySet()) {
            final BlockEntity entity = level.getBlockEntity(pos);
            final BlockEntityRenderer<BlockEntity> renderer = entity == null ? null
                    : Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
            if (renderer == null) {
                continue;
            }
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            try {
                renderer.render(entity, 0.0F, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (final RuntimeException e) {
                LOGGER.warn("A manual's scene could not draw the block entity of {}", entity.getBlockState(), e);
            }
            pose.popPose();
        }
    }
}
