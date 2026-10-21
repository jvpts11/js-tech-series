/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.client.model.kit.SceneRenderer;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.region.Box;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Paints a moving structure for its diagram of forces: its blocks turned as it stands, about its centre of mass, and
 * the ground below it with its grid, so the view shows its height and its lean. Of a structure of more blocks than
 * {@link #MOST_DRAWN}, the first that many are drawn, which keeps the screen quick however big the structure.
 */
final class ForceDiagramPainter implements SceneRenderer.IScenePainter {

    private final ClientStructure structure;
    private final Vec3 centre;
    private final double ground;
    private final int groundColour;
    private final int gridColour;

    /** The most blocks of a structure drawn in its diagram. */
    static final int MOST_DRAWN = 8_192;
    /* What every block's random choice of model is made with, so the view looks the same each time it is drawn. */
    private static final long SEED = 42L;
    /* How far round the structure the ground is drawn, as a share of its size, and its grid's spacing, in blocks. */
    private static final double GROUND_REACH = 1.6;
    private static final int GRID = 4;
    private static final float LINE = 0.04F;

    ForceDiagramPainter(final ClientStructure structure, final Vec3 centre, final double ground,
                        final int groundColour, final int gridColour) {
        this.structure = structure;
        this.centre = centre;
        this.ground = ground;
        this.groundColour = groundColour;
        this.gridColour = gridColour;
    }

    @Override
    public void paint(final PoseStack pose, final MultiBufferSource.BufferSource buffers) {
        blocks(pose, buffers);
        ground(pose, buffers);
    }

    /* How far the structure reaches from its middle, in blocks, for how big its view is drawn. */
    static double extent(final ClientStructure structure) {
        final Box box = structure.bounds();
        return Math.max(1, Math.max(box.maxX() - box.minX() + 1, Math.max(box.maxY() - box.minY() + 1,
                box.maxZ() - box.minZ() + 1)));
    }

    /* Every block of the structure, from its blocks as the player's game has them, turned about its pivot. */
    private void blocks(final PoseStack pose, final MultiBufferSource buffers) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        final StructurePose at = structure.frame().pose();
        final Quaternionf turn = new Quaternionf((float) at.qx(), (float) at.qy(), (float) at.qz(), (float) at.qw());
        final Box box = structure.bounds();
        final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        final RandomSource random = RandomSource.create();
        int drawn = 0;
        for (final BlockPos place : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(),
                box.maxY(), box.maxZ())) {
            final BlockState state = level.getBlockState(place);
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            if (++drawn > MOST_DRAWN) {
                break;
            }
            final BakedModel model = dispatcher.getBlockModel(state);
            final ModelData data = model.getModelData(level, place, state, ModelData.EMPTY);
            pose.pushPose();
            pose.translate((float) (at.x() - centre.x), (float) (at.y() - centre.y), (float) (at.z() - centre.z));
            pose.mulPose(turn);
            pose.translate((float) (place.getX() - structure.pivotX()), (float) (place.getY() - structure.pivotY()),
                    (float) (place.getZ() - structure.pivotZ()));
            random.setSeed(SEED);
            for (final RenderType type : model.getRenderTypes(state, random, data)) {
                random.setSeed(SEED);
                dispatcher.renderBatched(state, place, level, pose, buffers.getBuffer(type), true, random, data,
                        type);
            }
            pose.popPose();
        }
    }

    /* The ground below its centre, as far below as the screen draws it, with a grid on it. */
    private void ground(final PoseStack pose, final MultiBufferSource buffers) {
        if (ground <= 0) {
            return;
        }
        final float reach = (float) (extent(structure) * GROUND_REACH);
        final float y = (float) -ground;
        final VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        final Matrix4f matrix = pose.last().pose();
        quads.addVertex(matrix, -reach, y, -reach).setColor(groundColour);
        quads.addVertex(matrix, -reach, y, reach).setColor(groundColour);
        quads.addVertex(matrix, reach, y, reach).setColor(groundColour);
        quads.addVertex(matrix, reach, y, -reach).setColor(groundColour);
        final float lift = LINE / 2;
        for (float along = -reach; along <= reach; along += GRID) {
            strip(quads, matrix, along - LINE, along + LINE, -reach, reach, y + lift);
            strip(quads, matrix, -reach, reach, along - LINE, along + LINE, y + lift);
        }
    }

    private void strip(final VertexConsumer quads, final Matrix4f matrix, final float x0, final float x1,
                       final float z0, final float z1, final float y) {
        quads.addVertex(matrix, x0, y, z0).setColor(gridColour);
        quads.addVertex(matrix, x0, y, z1).setColor(gridColour);
        quads.addVertex(matrix, x1, y, z1).setColor(gridColour);
        quads.addVertex(matrix, x1, y, z0).setColor(gridColour);
    }
}
