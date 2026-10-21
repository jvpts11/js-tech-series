/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.gui.tooltip.TooltipPreview;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A 3D preview of a tooltip drawn: its blocks as the game draws a block in a slot, lit the same, seen from above at
 * a slant and turning slowly about their middle, sized to fit the square, in the middle of the tooltip. For a player
 * who asked for less movement it stands still, seen from a corner.
 */
final class ClientTooltipPreview implements ClientTooltipComponent {

    private final TooltipPreview preview;
    private final float middleX;
    private final float middleY;
    private final float middleZ;
    private final float across;

    /** The room under a preview, before the next line. */
    private static final int UNDER = 3;
    /** How far the view looks down, and the corner it stands at when still, in degrees. */
    private static final float TILT = 30.0F;
    private static final float CORNER = 225.0F;
    /** How long a whole turn takes, in milliseconds. */
    private static final long TURN = 12_000L;
    private static final float WHOLE = 360.0F;
    /** How far in front of the tooltip's ground the blocks are drawn, so that none of them sinks into it. */
    private static final float IN_FRONT = 150.0F;
    /** A cube's widest look from any side, its diagonal, as a share of its side. */
    private static final float DIAGONAL = 1.75F;
    /** How far outside its faces a block's edges are drawn, so that the faces do not hide them. */
    private static final double OUTSIDE = 0.002;

    ClientTooltipPreview(final TooltipPreview preview) {
        this.preview = preview;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos pos : preview.blocks().keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }
        this.middleX = (minX + maxX) / 2.0F;
        this.middleY = (minY + maxY) / 2.0F;
        this.middleZ = (minZ + maxZ) / 2.0F;
        this.across = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
    }

    @Override
    public int getHeight() {
        return preview.size() + UNDER;
    }

    @Override
    public int getWidth(final Font font) {
        return preview.size();
    }

    @Override
    public void renderImage(final Font font, final int x, final int y, final GuiGraphics g) {
        final int size = preview.size();
        final float scale = size / (across * DIAGONAL);
        final float turn = MotionClock.reduced() ? CORNER
                : CORNER + WHOLE * (Util.getMillis() % TURN) / (float) TURN;
        final BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        final PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(TooltipPieces.centred(x, size) + size / 2.0F, y + size / 2.0F, IN_FRONT);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(TILT));
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        pose.translate(-middleX, -middleY, -middleZ);
        Lighting.setupFor3DItems();
        for (final Map.Entry<BlockPos, BlockState> placed : preview.blocks().entrySet()) {
            pose.pushPose();
            pose.translate(placed.getKey().getX(), placed.getKey().getY(), placed.getKey().getZ());
            blocks.renderSingleBlock(placed.getValue(), pose, g.bufferSource(), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
        g.flush();
        // Each block's edges, faint, as the faces of alike blocks run into one another without them.
        final int edge = TooltipPalette.get().edge();
        final VertexConsumer lines = g.bufferSource().getBuffer(RenderType.lines());
        for (final BlockPos pos : preview.blocks().keySet()) {
            LevelRenderer.renderLineBox(pose, lines, pos.getX() - OUTSIDE, pos.getY() - OUTSIDE,
                    pos.getZ() - OUTSIDE, pos.getX() + 1 + OUTSIDE, pos.getY() + 1 + OUTSIDE, pos.getZ() + 1 + OUTSIDE,
                    FastColor.ARGB32.red(edge) / 255.0F, FastColor.ARGB32.green(edge) / 255.0F,
                    FastColor.ARGB32.blue(edge) / 255.0F, FastColor.ARGB32.alpha(edge) / 255.0F);
        }
        g.flush();
        pose.popPose();
    }
}
