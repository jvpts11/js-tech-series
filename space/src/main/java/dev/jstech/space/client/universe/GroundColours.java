/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * The colours the ground is drawn in from far above, as the approved climb paints its far map: each the mean of the
 * block's own texture, tinted as the game tints it in that biome, so the blocks seen from on high, their far map and
 * the world's sphere speak one language. A biome is painted as the ground mostly showing in it from above: water, sand,
 * the grass, the woods' leaves over a little grass, snow, stone. Everything here is on the render thread.
 */
final class GroundColours {

    private final Map<ResourceLocation, Integer> means = new HashMap<>();
    private final Map<Holder<Biome>, Integer> painted = new HashMap<>();
    private final Map<ResourceLocation, Integer> sprites = new HashMap<>();

    private static final int CHANNEL = 0xFF;
    /* How much of a wood's ground shows between its trees, and of a swamp's between its water. */
    private static final double GRASS_UNDER_TREES = 0.25;
    private static final double SWAMP_WATER = 0.35;
    /* How deep under the sea the water is darkest, and how dark; and where snow lies on any high ground. */
    private static final double DEEPEST = 40;
    private static final double DEEP_DARK = 0.45;
    private static final int SNOW_LINE = 210;
    private static final int STONE_LINE = 170;
    /* The blocks whose textures the biomes are painted from. */
    private static final ResourceLocation GRASS = texture("grass_block_top");
    private static final ResourceLocation OAK = texture("oak_leaves");
    private static final ResourceLocation SPRUCE = texture("spruce_leaves");
    private static final ResourceLocation BIRCH = texture("birch_leaves");
    private static final ResourceLocation JUNGLE = texture("jungle_leaves");
    private static final ResourceLocation DARK_OAK = texture("dark_oak_leaves");
    private static final ResourceLocation ACACIA = texture("acacia_leaves");
    private static final ResourceLocation CHERRY = texture("cherry_leaves");
    private static final ResourceLocation MANGROVE = texture("mangrove_leaves");
    private static final ResourceLocation SAND = texture("sand");
    private static final ResourceLocation RED_SAND = texture("red_sand");
    private static final ResourceLocation TERRACOTTA = texture("orange_terracotta");
    private static final ResourceLocation SNOW = texture("snow");
    private static final ResourceLocation STONE = texture("stone");
    private static final ResourceLocation WATER = texture("water_still");
    private static final ResourceLocation MYCELIUM = texture("mycelium_top");
    private static final ResourceLocation PODZOL = texture("podzol_top");
    /* The leaves the game tints with fixed colours, not the biome's. */
    private static final int SPRUCE_TINT = 0x619961;
    private static final int BIRCH_TINT = 0x80A755;
    /* The game's water tint where a biome gives none of its own. */
    private static final int OCEAN_TINT = 0x3F76E4;

    /**
     * The colour of a far map's pixel: its {@code biome} at a ground {@code height} blocks high, under the sea where
     * that is below {@code sea}, as RGB.
     */
    int ofMap(final Holder<Biome> biome, final int height, final int sea) {
        final boolean wet = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
        if (height < sea) {
            return water(biome, sea - height);
        }
        if (wet) {
            // The sea's own ground, just out of it: its shore.
            return biome.is(Tags.Biomes.IS_SNOWY) ? mean(SNOW) : mean(SAND);
        }
        final int ground = this.painted.computeIfAbsent(biome, this::paint);
        if (height >= SNOW_LINE) {
            return mix(ground, mean(SNOW), Math.min(1, (height - SNOW_LINE) / 30.0 + 0.5));
        }
        if (height >= STONE_LINE && biome.is(BiomeTags.IS_MOUNTAIN)) {
            return mix(ground, mean(STONE), Math.min(0.7, (height - STONE_LINE) / 60.0));
        }
        return ground;
    }

    /** The open sea's colour, as RGB: the water's texture tinted as the game tints its oceans. */
    int sea() {
        return multiply(mean(WATER), OCEAN_TINT);
    }

    /** A shore's sand, as RGB. */
    int shore() {
        return mean(SAND);
    }

    /** The colour of the top of the block at {@code top}, as the game draws it there from above, as RGB. */
    int ofBlock(final ClientLevel level, final BlockPos top, final BlockState state) {
        if (state.getFluidState().is(FluidTags.WATER)) {
            return multiply(mean(WATER), BiomeColors.getAverageWaterColor(level, top));
        }
        final List<BakedQuad> quads = facesUp(state);
        if (quads.isEmpty()) {
            return state.getMapColor(level, top).col;
        }
        final BakedQuad face = quads.getFirst();
        final int colour = sprite(face.getSprite());
        return face.isTinted()
                ? multiply(colour, Minecraft.getInstance().getBlockColors().getColor(state, level, top,
                        face.getTintIndex()))
                : colour;
    }

    /* The faces of {@code state}'s model seen from above, its top first, or its loose ones if it has no top. */
    private static List<BakedQuad> facesUp(final BlockState state) {
        final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        final RandomSource random = RandomSource.create(0);
        final List<BakedQuad> up = model.getQuads(state, Direction.UP, random);
        return up.isEmpty() ? model.getQuads(state, null, random) : up;
    }

    private int water(final Holder<Biome> biome, final int depth) {
        final int surface = multiply(mean(WATER), biome.value().getWaterColor());
        return scale(surface, 1 - DEEP_DARK * Math.min(1, depth / DEEPEST));
    }

    /* What {@code biome} shows from above, its tints where it is. */
    private int paint(final Holder<Biome> biome) {
        final Biome value = biome.value();
        final int grass = multiply(mean(GRASS), value.getGrassColor(0, 0));
        final int foliage = value.getFoliageColor();
        if (biome.is(Biomes.MUSHROOM_FIELDS)) {
            return mean(MYCELIUM);
        }
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return mix(mean(RED_SAND), mean(TERRACOTTA), 0.5);
        }
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BEACH)) {
            return mean(SAND);
        }
        if (biome.is(Biomes.STONY_PEAKS) || biome.is(Biomes.STONY_SHORE)) {
            return mean(STONE);
        }
        if (biome.is(Tags.Biomes.IS_SNOWY) || biome.is(Tags.Biomes.IS_ICY)) {
            return biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_FOREST)
                    ? mix(mean(SNOW), multiply(mean(SPRUCE), SPRUCE_TINT), 0.4) : mean(SNOW);
        }
        if (biome.is(Tags.Biomes.IS_SWAMP)) {
            final int leaves = biome.is(Biomes.MANGROVE_SWAMP) ? multiply(mean(MANGROVE), foliage) : grass;
            return mix(leaves, multiply(mean(WATER), value.getWaterColor()), SWAMP_WATER);
        }
        final int leaves = leaves(biome, foliage);
        return leaves < 0 ? grass : mix(leaves, grass, GRASS_UNDER_TREES);
    }

    /* The leaves of {@code biome}'s woods, tinted, or -1 where it has no woods. */
    private int leaves(final Holder<Biome> biome, final int foliage) {
        if (biome.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
            return mix(multiply(mean(SPRUCE), SPRUCE_TINT), mean(PODZOL), 0.3);
        }
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return multiply(mean(SPRUCE), SPRUCE_TINT);
        }
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return multiply(mean(BIRCH), BIRCH_TINT);
        }
        if (biome.is(Biomes.DARK_FOREST)) {
            return multiply(mean(DARK_OAK), foliage);
        }
        if (biome.is(Biomes.CHERRY_GROVE)) {
            return mean(CHERRY);
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return multiply(mean(JUNGLE), foliage);
        }
        if (biome.is(Biomes.SAVANNA_PLATEAU) || biome.is(Biomes.WINDSWEPT_SAVANNA)) {
            return multiply(mean(ACACIA), foliage);
        }
        if (biome.is(BiomeTags.IS_FOREST)) {
            return multiply(mean(OAK), foliage);
        }
        return -1;
    }

    /* The mean colour of a block texture, the clear texels left out; grey if it cannot be read. */
    private int mean(final ResourceLocation path) {
        return this.means.computeIfAbsent(path, GroundColours::read);
    }

    /* The mean colour of a sprite of the blocks' atlas, as RGB. */
    private int sprite(final TextureAtlasSprite sprite) {
        return this.sprites.computeIfAbsent(sprite.contents().name(),
                unused -> meanOf(sprite.contents().getOriginalImage()));
    }

    private static int read(final ResourceLocation path) {
        final Resource resource = Minecraft.getInstance().getResourceManager().getResource(path).orElse(null);
        if (resource == null) {
            return 0x808080;
        }
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, in)) {
            return meanOf(image);
        } catch (final IOException unreadable) {
            return 0x808080;
        }
    }

    /* The mean colour of {@code image}'s texels that are not clear, as RGB. */
    private static int meanOf(@Nullable final NativeImage image) {
        if (image == null) {
            return 0x808080;
        }
        long red = 0;
        long green = 0;
        long blue = 0;
        long weight = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // Kept as ABGR.
                final int pixel = image.getPixelRGBA(x, y);
                final int alpha = pixel >>> 24;
                red += (long) (pixel & CHANNEL) * alpha;
                green += (long) (pixel >> 8 & CHANNEL) * alpha;
                blue += (long) (pixel >> 16 & CHANNEL) * alpha;
                weight += alpha;
            }
        }
        if (weight == 0) {
            return 0x808080;
        }
        return (int) (red / weight) << 16 | (int) (green / weight) << 8 | (int) (blue / weight);
    }

    private static ResourceLocation texture(final String block) {
        return ResourceLocation.withDefaultNamespace("textures/block/" + block + ".png");
    }

    /* {@code colour} tinted by {@code tint}, both RGB, as the game tints a grey texture. */
    private static int multiply(final int colour, final int tint) {
        return (colour >> 16 & CHANNEL) * (tint >> 16 & CHANNEL) / CHANNEL << 16
                | (colour >> 8 & CHANNEL) * (tint >> 8 & CHANNEL) / CHANNEL << 8
                | (colour & CHANNEL) * (tint & CHANNEL) / CHANNEL;
    }

    private static int scale(final int colour, final double by) {
        return (int) ((colour >> 16 & CHANNEL) * by) << 16 | (int) ((colour >> 8 & CHANNEL) * by) << 8
                | (int) ((colour & CHANNEL) * by);
    }

    /* {@code share} of the way from {@code from} to {@code to}, both RGB. */
    private static int mix(final int from, final int to, final double share) {
        final double keep = 1 - share;
        return (int) ((from >> 16 & CHANNEL) * keep + (to >> 16 & CHANNEL) * share) << 16
                | (int) ((from >> 8 & CHANNEL) * keep + (to >> 8 & CHANNEL) * share) << 8
                | (int) ((from & CHANNEL) * keep + (to & CHANNEL) * share);
    }
}
