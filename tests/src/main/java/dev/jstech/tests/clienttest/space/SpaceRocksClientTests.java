/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The ground of the Moon and of Mars on a player's game: every block and its item is drawn with the model made for
 * it, never the missing one; each block where it stands wears one of its four pictures, its sides with the very
 * picture of that variant's texture, texel by texel, and the rocks that have a top of their own wear the same
 * variant's top, a picture other than their sides'; and every one of the four is worn somewhere.
 */
public final class SpaceRocksClientTests {

    private static final List<BlockEntry<?>> GROUND = List.of(SpaceRocks.HIGHLAND_REGOLITH, SpaceRocks.MARE_REGOLITH,
            SpaceRocks.ANORTHOSITE, SpaceRocks.MARE_BASALT, SpaceRocks.ICY_REGOLITH, SpaceRocks.MARTIAN_REGOLITH,
            SpaceRocks.HEMATITE_REGOLITH, SpaceRocks.BASALT_SAND, SpaceRocks.WATER_ICE, SpaceRocks.DRY_ICE,
            SpaceRocks.MARTIAN_BASALT, SpaceRocks.COLUMNAR_MARTIAN_BASALT, SpaceRocks.VESICULAR_MARTIAN_BASALT,
            SpaceRocks.MUDSTONE, SpaceRocks.BUFF_MUDSTONE, SpaceRocks.DUSTED_MUDSTONE);
    private static final Set<BlockEntry<?>> OWN_TOPS = Set.of(SpaceRocks.MARTIAN_BASALT,
            SpaceRocks.COLUMNAR_MARTIAN_BASALT, SpaceRocks.VESICULAR_MARTIAN_BASALT, SpaceRocks.MUDSTONE,
            SpaceRocks.BUFF_MUDSTONE, SpaceRocks.DUSTED_MUDSTONE);
    private static final String TOP = "_top";
    /* The pictures each block comes in, the first named after the block and the rest numbered from 1. */
    private static final int VARIANTS = 4;
    /* The places a block is looked at in, a square of them this wide, each its own pick of a variant and a turn. */
    private static final int PLACES = 8;

    private SpaceRocksClientTests() {
    }

    @ClientTest(timeoutTicks = 100)
    public static void ground_isDrawnWithItsOwnPictures(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final ModelManager models = ctx.mc().getModelManager();
            final BakedModel missing = models.getMissingModel();
            for (final BlockEntry<?> block : GROUND) {
                final ResourceLocation id = block.getId();
                final BlockState state = block.get().defaultBlockState();
                final BakedModel model = models.getBlockModelShaper().getBlockModel(state);
                ctx.assertTrue(model != missing, id + " has the model made for it");
                ctx.assertTrue(ctx.mc().getItemRenderer().getItemModelShaper().getItemModel(block.item()) != missing,
                        id + " and so has its item");
                final Set<Integer> worn = new HashSet<>();
                for (int place = 0; place < PLACES * PLACES; place++) {
                    final BlockPos at = new BlockPos(place % PLACES, 0, place / PLACES);
                    final SpriteContents side = sprite(model, state, Direction.NORTH, at);
                    final SpriteContents top = sprite(model, state, Direction.UP, at);
                    final int variant = variantOf(id, side.name());
                    ctx.assertTrue(variant >= 0, id + "'s sides wear one of its own pictures; got " + side.name());
                    worn.add(variant);
                    final ResourceLocation sideName = named(id, "", variant);
                    final ResourceLocation topName = OWN_TOPS.contains(block) ? named(id, TOP, variant) : sideName;
                    ctx.assertTrue(top.name().equals(topName), id + "'s top wears " + topName + "; got " + top.name());
                    ctx.assertTrue(Arrays.equals(texels(side), texels(ctx, sideName)),
                            id + "'s sides are drawn with the picture of its texture, texel by texel");
                    ctx.assertTrue(Arrays.equals(texels(top), texels(ctx, topName)),
                            id + "'s top is drawn with the picture of its own");
                    if (OWN_TOPS.contains(block)) {
                        ctx.assertTrue(!Arrays.equals(texels(top), texels(side)),
                                id + "'s top is not its sides' picture");
                    }
                }
                ctx.assertTrue(worn.size() == VARIANTS, id + " wears all its " + VARIANTS + " pictures; " + worn);
            }
        });
    }

    /* The picture on the face {@code face} of {@code state} standing at {@code at}, picked as the game picks it. */
    private static SpriteContents sprite(final BakedModel model, final BlockState state, final Direction face,
                                         final BlockPos at) {
        final RandomSource random = RandomSource.create();
        random.setSeed(state.getSeed(at));
        return model.getQuads(state, face, random, ModelData.EMPTY, null).getFirst().getSprite().contents();
    }

    /* The texture {@code block/<id><part>} of variant {@code v}: the name itself for the first, numbered after. */
    private static ResourceLocation named(final ResourceLocation id, final String part, final int v) {
        final String name = "block/" + id.getPath() + part;
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), v == 0 ? name : name + "_" + v);
    }

    /* Which of {@code id}'s variants the side picture {@code sprite} is, or -1 for a picture not of its own. */
    private static int variantOf(final ResourceLocation id, final ResourceLocation sprite) {
        for (int v = 0; v < VARIANTS; v++) {
            if (named(id, "", v).equals(sprite)) {
                return v;
            }
        }
        return -1;
    }

    /* The texels of a sprite as the game holds it, as the game reads them, row by row. */
    private static int[] texels(final SpriteContents sprite) {
        return texels(sprite.getOriginalImage());
    }

    /* The texels of the texture {@code name} as the player's game reads its file. */
    private static int[] texels(final ClientTestContext ctx, final ResourceLocation name) {
        final ResourceLocation file = name.withPath(path -> "textures/" + path + ".png");
        final Resource resource = ctx.mc().getResourceManager().getResource(file)
                .orElseThrow(() -> new IllegalStateException("no texture " + file));
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, in)) {
            return texels(image);
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static int[] texels(final NativeImage image) {
        final int[] out = new int[image.getWidth() * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                out[y * image.getWidth() + x] = image.getPixelRGBA(x, y);
            }
        }
        return out;
    }
}
