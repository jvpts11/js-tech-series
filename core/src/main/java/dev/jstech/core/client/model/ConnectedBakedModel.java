/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import dev.jstech.core.connect.ConnectedFaces;
import dev.jstech.core.connect.ConnectedQuadrants;
import dev.jstech.core.connect.IJoinRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

/**
 * A full block whose faces run on into the faces of the blocks it joins, drawn from five tiles a quarter of a face at
 * a time, as {@link ConnectedQuadrants} picks them. Which neighbours a face joins is worked out as the world's mesh is
 * built, so the faces change only when a block around them changes.
 *
 * <p>The block's own model still gives its particle and how it is held; its faces are drawn here. Each face of each
 * mask is made once and kept until the models are baked again.
 */
public final class ConnectedBakedModel extends BakedModelWrapper<BakedModel> {

    private final IJoinRule joins;
    private final TextureAtlasSprite[] tiles;
    /* Each side's quads for each mask, made the first time they are drawn. */
    private final Map<Integer, List<BakedQuad>> faces = new ConcurrentHashMap<>();

    /**
     * @param base  the block's own model
     * @param joins which neighbours the block joins
     * @param tiles the five tiles, in the order {@link ConnectedQuadrants} numbers them
     */
    public ConnectedBakedModel(final BakedModel base, final IJoinRule joins, final TextureAtlasSprite[] tiles) {
        super(base);
        if (tiles.length != ConnectedQuadrants.TILES) {
            throw new IllegalArgumentException("a connected texture is drawn from five tiles, not " + tiles.length);
        }
        this.joins = joins;
        this.tiles = tiles.clone();
    }

    @Override
    public ModelData getModelData(final BlockAndTintGetter level, final BlockPos pos, final BlockState state,
                                  final ModelData data) {
        return data.derive().with(ConnectedFaces.MASKS, ConnectedFaces.of(level, pos, state, this.joins)).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                                    final RandomSource random, final ModelData data,
                                    @Nullable final RenderType renderType) {
        if (side == null) {
            return List.of();
        }
        final ConnectedFaces.FaceMasks masks = data.get(ConnectedFaces.MASKS);
        final int mask = masks == null ? 0 : masks.of(side);
        return this.faces.computeIfAbsent(side.get3DDataValue() << Byte.SIZE | mask, key -> face(side, mask));
    }

    /* The four quarters of {@code side}, each from its tile. */
    private List<BakedQuad> face(final Direction side, final int mask) {
        final int[] picked = ConnectedQuadrants.tiles(mask);
        final List<BakedQuad> out = new ArrayList<>(picked.length);
        // Up-left, up-right, down-right, down-left, as fractions of the face across and up.
        final float[][] quarters = {{0F, 0.5F}, {0.5F, 0.5F}, {0.5F, 0F}, {0F, 0F}};
        for (int q = 0; q < picked.length; q++) {
            out.add(quarter(side, quarters[q][0], quarters[q][1], this.tiles[picked[q]]));
        }
        return List.copyOf(out);
    }

    /* The quarter of {@code side} whose lower left corner is at {@code across}, {@code up}, from {@code tile}. */
    private static BakedQuad quarter(final Direction side, final float across, final float up,
                                     final TextureAtlasSprite tile) {
        final Vec3i normal = side.getNormal();
        final Vec3i upward = ConnectedFaces.up(side).getNormal();
        final Vec3i rightward = ConnectedFaces.right(side).getNormal();
        final QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(tile);
        baker.setDirection(side);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(true);
        // Counter-clockwise seen from outside: top left, bottom left, bottom right, top right.
        final float[][] corners = {{across, up + 0.5F}, {across, up}, {across + 0.5F, up}, {across + 0.5F, up + 0.5F}};
        for (final float[] corner : corners) {
            final float r = corner[0] - 0.5F;
            final float u = corner[1] - 0.5F;
            final float x = 0.5F + normal.getX() * 0.5F + rightward.getX() * r + upward.getX() * u;
            final float y = 0.5F + normal.getY() * 0.5F + rightward.getY() * r + upward.getY() * u;
            final float z = 0.5F + normal.getZ() * 0.5F + rightward.getZ() * r + upward.getZ() * u;
            baker.addVertex(x, y, z)
                    .setColor(255, 255, 255, 255)
                    .setUv(tile.getU0() + (tile.getU1() - tile.getU0()) * corner[0],
                            tile.getV0() + (tile.getV1() - tile.getV0()) * (1F - corner[1]))
                    .setUv2(0, 0)
                    .setNormal(normal.getX(), normal.getY(), normal.getZ());
        }
        return baker.bakeQuad();
    }
}
