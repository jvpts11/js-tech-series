/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * A block's model with the packs' glowing textures drawn over its faces: each face whose texture has a glowing one
 * ({@link PackEmissive}) is drawn again, the same corners with the glowing texture laid where the face's own lies, at
 * full light, unshaded and uncoloured. A face of the solid or cut-out layers is drawn again in the cut-out layer, so
 * the glowing texture's clear pixels show the face beneath; a see-through face, in its own layer.
 */
final class PackEmissiveModel extends BakedModelWrapper<BakedModel> {

    /* The colour a glowing face is drawn in: its texture's own, white with nothing taken from it. */
    private static final int WHITE = -1;

    PackEmissiveModel(final BakedModel base) {
        super(base);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                                    final RandomSource random, final ModelData data,
                                    @Nullable final RenderType renderType) {
        if (renderType == null) {
            final List<BakedQuad> own = super.getQuads(state, side, random, data, null);
            final List<BakedQuad> out = new ArrayList<>(own);
            own.forEach(quad -> glowing(quad).ifPresent(out::add));
            return out;
        }
        final ChunkRenderTypeSet layers = super.getRenderTypes(state, random, data);
        final List<BakedQuad> out = new ArrayList<>();
        if (layers.contains(renderType)) {
            out.addAll(super.getQuads(state, side, random, data, renderType));
        }
        // A face's glowing texture goes in the layer that draws its clear pixels, read from every layer of the model.
        for (final RenderType own : layers) {
            if (glowLayer(own) != renderType) {
                continue;
            }
            for (final BakedQuad quad : super.getQuads(state, side, random, data, own)) {
                glowing(quad).ifPresent(out::add);
            }
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(final BlockState state, final RandomSource random,
                                             final ModelData data) {
        final ChunkRenderTypeSet own = super.getRenderTypes(state, random, data);
        final List<RenderType> glows = new ArrayList<>();
        for (final RenderType layer : own) {
            glows.add(glowLayer(layer));
        }
        return ChunkRenderTypeSet.union(own, ChunkRenderTypeSet.of(glows));
    }

    /* The layer a face of {@code layer} has its glowing texture drawn in. */
    private static RenderType glowLayer(final RenderType layer) {
        return layer == RenderType.translucent() ? layer : RenderType.cutout();
    }

    /* The face drawn again with its texture's glowing one, at full light; none for a texture that has none. */
    private static Optional<BakedQuad> glowing(final BakedQuad quad) {
        final Optional<TextureAtlasSprite> found = PackEmissive.glowingOf(quad.getSprite());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        final TextureAtlasSprite own = quad.getSprite();
        final TextureAtlasSprite glow = found.get();
        final int[] data = quad.getVertices().clone();
        for (int corner = 0; corner < 4; corner++) {
            final int at = corner * IQuadTransformer.STRIDE;
            final float u = Float.intBitsToFloat(data[at + IQuadTransformer.UV0]);
            final float v = Float.intBitsToFloat(data[at + IQuadTransformer.UV0 + 1]);
            data[at + IQuadTransformer.UV0] = Float.floatToRawIntBits(glow.getU(own.getUOffset(u)));
            data[at + IQuadTransformer.UV0 + 1] = Float.floatToRawIntBits(glow.getV(own.getVOffset(v)));
            data[at + IQuadTransformer.COLOR] = WHITE;
            data[at + IQuadTransformer.UV2] = LightTexture.FULL_BRIGHT;
        }
        return Optional.of(new BakedQuad(data, -1, quad.getDirection(), glow, false, false));
    }
}
