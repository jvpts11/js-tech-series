/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.SceneRenderer;
import dev.jstech.core.log.CoreLogs;
import java.util.Map;
import net.minecraft.client.Minecraft;
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
import org.slf4j.Logger;

/**
 * Paints the blocks of a manual's scene as the game draws blocks in its world: each block's model shaded as its
 * neighbours shade it, and each block entity through its renderer, the Core's models among them, all in full light.
 * The scene is drawn into its surface by the {@link SceneRenderer}.
 */
final class SceneLevelPainter implements SceneRenderer.IScenePainter {

    private static final Logger LOGGER = CoreLogs.GUIDE.logger();
    /** What every block's random choice of model is made with, so a scene looks the same each time it is drawn. */
    private static final long SEED = 42L;

    private final SceneLevel level;

    SceneLevelPainter(final SceneLevel level) {
        this.level = level;
    }

    @Override
    public void paint(final PoseStack pose, final MultiBufferSource.BufferSource buffers) {
        blocks(pose, buffers);
        entities(pose, buffers);
    }

    /** Every block's model, in each of the kinds of layer it is drawn in, shaded by the blocks round it. */
    private void blocks(final PoseStack pose, final MultiBufferSource buffers) {
        final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        final RandomSource random = RandomSource.create();
        for (final Map.Entry<BlockPos, BlockState> placed : this.level.shownStates().entrySet()) {
            final BlockPos pos = placed.getKey();
            final BlockState state = placed.getValue();
            if (state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            final BakedModel model = dispatcher.getBlockModel(state);
            final BlockEntity entity = this.level.getBlockEntity(pos);
            final ModelData data = model.getModelData(this.level, pos, state,
                    entity == null ? ModelData.EMPTY : entity.getModelData());
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            random.setSeed(SEED);
            for (final RenderType type : model.getRenderTypes(state, random, data)) {
                random.setSeed(SEED);
                dispatcher.renderBatched(state, pos, this.level, pose, buffers.getBuffer(type), true, random, data,
                        type);
            }
            pose.popPose();
        }
    }

    /** Every block entity through its renderer, in full light; one that fails is left out with a line in the log. */
    private void entities(final PoseStack pose, final MultiBufferSource buffers) {
        for (final BlockPos pos : this.level.shownStates().keySet()) {
            final BlockEntity entity = this.level.getBlockEntity(pos);
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
