/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import dev.jstech.core.connect.texture.CtmTexture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * A block's model drawn with the packs' connected textures: as the world's mesh is built it reads what stands around
 * the block, and each of its faces is drawn as the packs' rules pick, in the layer they say. In a screen or a hand,
 * where nothing stands around it, it is drawn as its own model.
 */
final class ConnectedPackModel extends BakedModelWrapper<BakedModel> {

    private final Map<Integer, CtmTexture> overrides;
    private final ChunkRenderTypeSet layers;
    private final boolean withBiome;
    private final boolean withName;

    /**
     * @param base      the block's own model
     * @param overrides CTM's overrides of its model, by tint index
     * @param layers    the layers its rules may draw in besides its own
     * @param withBiome whether a rule asks the block's biome
     * @param withName  whether a rule asks the block's name
     */
    ConnectedPackModel(final BakedModel base, final Map<Integer, CtmTexture> overrides,
                       final ChunkRenderTypeSet layers, final boolean withBiome, final boolean withName) {
        super(base);
        this.overrides = Map.copyOf(overrides);
        this.layers = layers;
        this.withBiome = withBiome;
        this.withName = withName;
    }

    @Override
    public ModelData getModelData(final BlockAndTintGetter level, final BlockPos pos, final BlockState state,
                                  final ModelData data) {
        final ModelData own = super.getModelData(level, pos, state, data);
        return PackSurroundings.of(level, pos, state, this.withBiome, this.withName).into(own);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                                    final RandomSource random, final ModelData data,
                                    @Nullable final RenderType renderType) {
        final PackSurroundings around = data.get(PackSurroundings.PROPERTY);
        if (state == null || around == null) {
            return super.getQuads(state, side, random, data, renderType);
        }
        final List<BakedQuad> out = new ArrayList<>();
        if (renderType == null) {
            for (final BakedQuad quad : super.getQuads(state, side, random, data, null)) {
                PackFaces.draw(quad, state, around, this.overrides).forEach(drawn -> out.add(drawn.quad()));
            }
            return out;
        }
        // A face moved to another layer is drawn there, so every layer of the model is read for each.
        for (final RenderType own : super.getRenderTypes(state, random, data)) {
            for (final BakedQuad quad : super.getQuads(state, side, random, data, own)) {
                for (final PackFaces.Drawn drawn : PackFaces.draw(quad, state, around, this.overrides)) {
                    if ((drawn.layer() == null ? own : drawn.layer()) == renderType) {
                        out.add(drawn.quad());
                    }
                }
            }
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(final BlockState state, final RandomSource random,
                                             final ModelData data) {
        return ChunkRenderTypeSet.union(super.getRenderTypes(state, random, data), this.layers);
    }
}
