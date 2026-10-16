/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.core.multiblock.ShapePreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A multiblock drawn in a screen, in 3D, as the Structure Projector's screen, the recipe viewers and the manuals show
 * it: the layer shown whole, the layers below it as ghosts, those above it left out, every layer whole when every
 * layer is shown; turned as the player drags it or clicks it. One for each view, which keeps how it is turned.
 */
public final class MultiblockPreview {

    private float yaw = START_YAW;
    private float pitch = START_PITCH;

    /** How far a click turns it, in degrees, where it cannot be dragged. */
    public static final float CLICK_TURN = 45.0F;
    private static final float START_YAW = 225.0F;
    private static final float START_PITCH = 30.0F;
    private static final float DEPTH = 150.0F;
    private static final float FILL = 0.5F;
    private static final float BELOW_ALPHA = 0.35F;

    /** Turns it as a drag of ({@code dx}, {@code dy}) does. */
    public void drag(final double dx, final double dy) {
        this.yaw += (float) dx;
        this.pitch = Math.max(-90.0F, Math.min(90.0F, this.pitch + (float) dy));
    }

    /** Turns it a step round, as a click does where it cannot be dragged. */
    public void step() {
        this.yaw += CLICK_TURN;
    }

    /** Draws {@code preview} within ({@code x}, {@code y}, {@code w}, {@code h}), up to {@code layer} (0 for all). */
    public void draw(final GuiGraphics g, final int x, final int y, final int w, final int h,
                     final ShapePreview preview, final int layer) {
        final PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + w / 2.0F, y + h / 2.0F, DEPTH);
        final float span = Math.max(preview.sizeX(), Math.max(preview.sizeY(), preview.sizeZ()));
        final float scale = Math.min(w, h) * FILL / span;
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(this.pitch));
        pose.mulPose(Axis.YP.rotationDegrees(this.yaw));
        pose.translate(-(preview.minX() + preview.sizeX() / 2.0F), -(preview.minY() + preview.sizeY() / 2.0F),
                -(preview.minZ() + preview.sizeZ() / 2.0F));
        RenderSystem.enableDepthTest();
        Lighting.setupForEntityInInventory();
        final BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        final VertexConsumer faded = new FadedConsumer(g.bufferSource().getBuffer(Sheets.translucentCullBlockSheet()),
                BELOW_ALPHA);
        try {
            for (final ShapePreview.Ghost ghost : preview.ghosts()) {
                final int shown = ghost.layer() + 1;
                if (layer > 0 && shown > layer) {
                    continue;
                }
                final BlockState state = GhostBlocks.stateOf(ghost);
                if (state == null) {
                    continue;
                }
                pose.pushPose();
                pose.translate(ghost.x(), ghost.y(), ghost.z());
                if (layer > 0 && shown < layer) {
                    blocks.getModelRenderer().renderModel(pose.last(), faded, state, blocks.getBlockModel(state),
                            1F, 1F, 1F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                } else {
                    blocks.renderSingleBlock(state, pose, g.bufferSource(), LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY);
                }
                pose.popPose();
            }
            g.flush();
        } finally {
            pose.popPose();
            Lighting.setupFor3DItems();
        }
    }
}
