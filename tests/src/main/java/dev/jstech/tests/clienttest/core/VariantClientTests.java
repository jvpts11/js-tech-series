/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.variant.Variant;
import dev.jstech.core.variant.Variants;
import dev.jstech.tests.TestVariants;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * The look of the variants on a player's game: the oak's test frame is drawn with the oak planks' own texture, and the
 * iron's casing with the block of iron's, through the files the Core makes as the resources load.
 */
public final class VariantClientTests {

    private VariantClientTests() {
    }

    @ClientTest(timeoutTicks = 100)
    public static void variants_areDrawnWithTheirMaterialsTextures(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final Variant oak = Variants.of(TestVariants.FRAMES, ResourceLocation.withDefaultNamespace("oak"))
                    .orElseThrow();
            ctx.assertTrue(texture(oak).equals(ResourceLocation.withDefaultNamespace("block/oak_planks")),
                    "the oak's frame is drawn with the oak planks; got " + texture(oak));
            final Variant iron = Variants.of(TestVariants.CASINGS, ResourceLocation.fromNamespaceAndPath("c", "iron"))
                    .orElseThrow();
            ctx.assertTrue(texture(iron).equals(ResourceLocation.withDefaultNamespace("block/iron_block")),
                    "the iron's casing with the block of iron; got " + texture(iron));
        });
    }

    /* The texture the block's top face is drawn with, as the game baked it. */
    private static ResourceLocation texture(final Variant variant) {
        final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(variant.block()
                .defaultBlockState());
        for (final BakedQuad quad : model.getQuads(variant.block().defaultBlockState(), Direction.UP,
                RandomSource.create())) {
            return quad.getSprite().contents().name();
        }
        return model.getParticleIcon().contents().name();
    }
}
