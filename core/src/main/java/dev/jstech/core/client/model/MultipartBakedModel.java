/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import com.mojang.math.Transformation;
import dev.jstech.core.multipart.ModelLayout;
import dev.jstech.core.multipart.PlacedModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * A multipart block's model: the block's own model, and on top of it what its block entity's {@link ModelLayout}
 * places, its parts and the pieces of its wires. It is drawn into the world's mesh, which is built again only when
 * the block changes, so a block of many parts costs nothing frame to frame.
 *
 * <p>Each placed model's quads are turned and moved once, the first time they are drawn, and kept until the models
 * are baked again. They are drawn in the cutout layer, as parts with holes in them need.
 */
public final class MultipartBakedModel extends BakedModelWrapper<BakedModel> {

    /** Each placed model's quads, turned and moved, kept until the models are baked again. */
    private static final Map<PlacedModel, List<BakedQuad>> PLACED = new ConcurrentHashMap<>();
    private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final float PIXEL = 1F / 16F;

    public MultipartBakedModel(final BakedModel base) {
        super(base);
    }

    /** Forgets the quads it turned and moved, as the models are baked again. */
    public static void forget() {
        PLACED.clear();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                                    final RandomSource random, final ModelData data,
                                    @Nullable final RenderType renderType) {
        final List<BakedQuad> own = super.getQuads(state, side, random, data, renderType);
        final ModelLayout layout = data.get(ModelLayout.PROPERTY);
        // Placed models are never culled by a face: they are drawn with the quads that belong to no side.
        if (layout == null || layout.isEmpty() || side != null
                || renderType != null && renderType != RenderType.cutout()) {
            return own;
        }
        final List<BakedQuad> out = new ArrayList<>(own);
        for (final PlacedModel placed : layout.models()) {
            out.addAll(PLACED.computeIfAbsent(placed, MultipartBakedModel::place));
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(final BlockState state, final RandomSource random,
                                             final ModelData data) {
        final ChunkRenderTypeSet own = super.getRenderTypes(state, random, data);
        final ModelLayout layout = data.get(ModelLayout.PROPERTY);
        return layout == null || layout.isEmpty() ? own : ChunkRenderTypeSet.union(own, CUTOUT);
    }

    /* Every quad of the placed model, turned to its facing about the block's middle and moved by its offset. */
    private static List<BakedQuad> place(final PlacedModel placed) {
        final BakedModel model = Minecraft.getInstance().getModelManager()
                .getModel(ModelResourceLocation.standalone(placed.model()));
        final RandomSource random = RandomSource.create(0L);
        final List<BakedQuad> quads = new ArrayList<>(model.getQuads(null, null, random, ModelData.EMPTY, null));
        for (final Direction side : Direction.values()) {
            quads.addAll(model.getQuads(null, side, random, ModelData.EMPTY, null));
        }
        final Matrix4f matrix = new Matrix4f()
                .translate(placed.dx() * PIXEL, placed.dy() * PIXEL, placed.dz() * PIXEL)
                .translate(0.5F, 0.5F, 0.5F);
        switch (placed.facing()) {
            case SOUTH -> matrix.rotateY((float) Math.PI);
            case WEST -> matrix.rotateY((float) Math.PI / 2F);
            case EAST -> matrix.rotateY((float) -Math.PI / 2F);
            case UP -> matrix.rotateX((float) Math.PI / 2F);
            case DOWN -> matrix.rotateX((float) -Math.PI / 2F);
            default -> { }
        }
        matrix.translate(-0.5F, -0.5F, -0.5F);
        final List<BakedQuad> moved = QuadTransformers.applying(new Transformation(matrix)).process(quads);
        // The transform moves the vertices and their normals; each quad's own face turns with them, for its shading.
        final List<BakedQuad> out = new ArrayList<>(moved.size());
        for (final BakedQuad quad : moved) {
            final Direction turned = Direction.rotate(matrix, quad.getDirection());
            out.add(new BakedQuad(quad.getVertices(), quad.getTintIndex(), turned, quad.getSprite(), quad.isShade(),
                    quad.hasAmbientOcclusion()));
        }
        return List.copyOf(out);
    }
}
