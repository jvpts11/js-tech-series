/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import com.mojang.logging.LogUtils;
import dev.jstech.core.light.LightFlood;
import dev.jstech.core.light.LightMath;
import dev.jstech.core.light.LightingSettings;
import dev.jstech.core.light.TintGrid;
import dev.jstech.core.stepaside.StepAside;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * A block's model lit by coloured light: as the world's mesh is built it works out the colour the light around the
 * block gives each place beside it ({@link TintGrid}), and each corner of its faces is coloured by the places it looks
 * into. The colour rides in the corners of the faces, which the game, Sodium, Embeddium and Iris all draw, so it looks
 * the same with them as without. Where no coloured light is near, or in a hand, it is drawn as its own model.
 */
final class ColouredLightModel extends BakedModelWrapper<BakedModel> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModelProperty<TintGrid> TINTS = new ModelProperty<>();
    /* The block format's ints per corner, where a corner keeps its place and colour, and a channel's bits. */
    private static final int CORNERS = 4;
    private static final int STRIDE = 8;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final int COLOUR = 3;
    private static final int LIGHT = 6;
    private static final int CHANNEL = 0xFF;
    private static final int GREEN_SHIFT = 8;
    private static final int BLUE_SHIFT = 16;
    private static final int ALPHA_SHIFT = 24;
    private static final double HALF = 0.5;

    ColouredLightModel(final BakedModel base) {
        super(base);
    }

    /** The block's own model, which it draws lit. */
    BakedModel base() {
        return this.originalModel;
    }

    /** Wraps the model of every block drawn as a model, which gives no light of its own, as the models are baked. */
    static void wrap(final ModelEvent.ModifyBakingResult event) {
        if (StepAside.COLOURED_LIGHT.steps()) {
            return;
        }
        final Map<ModelResourceLocation, BakedModel> models = event.getModels();
        final Set<ModelResourceLocation> done = new HashSet<>();
        for (final Block block : BuiltInRegistries.BLOCK) {
            for (final BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (state.getRenderShape() != RenderShape.MODEL || state.getLightEmission() > 0
                        || state.hasDynamicLightEmission()) {
                    continue;
                }
                final ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                final BakedModel model = models.get(location);
                if (model != null && done.add(location) && !(model instanceof ColouredLightModel)) {
                    models.put(location, new ColouredLightModel(model));
                }
            }
        }
        LOGGER.debug("Coloured light reaches {} block models", done.size());
    }

    @Override
    public ModelData getModelData(final BlockAndTintGetter level, final BlockPos pos, final BlockState state,
                                  final ModelData data) {
        final ModelData own = super.getModelData(level, pos, state, data);
        if (!LightingSettings.coloured()) {
            return own;
        }
        final long section = SectionPos.asLong(pos);
        final List<ColouredLights.Source> sources = ColouredLights.near(section);
        if ((sources.isEmpty() || !within(sources, pos)) && !ColouredLights.shaded(section)) {
            return own;
        }
        final TintGrid grid = grid(level, pos, sources);
        return grid.plain() ? own : own.derive().with(TINTS, grid).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                                    final RandomSource random, final ModelData data,
                                    @Nullable final RenderType renderType) {
        final List<BakedQuad> quads = super.getQuads(state, side, random, data, renderType);
        final TintGrid grid = data.get(TINTS);
        if (grid == null || quads.isEmpty()) {
            return quads;
        }
        final List<BakedQuad> out = new ArrayList<>(quads.size());
        for (final BakedQuad quad : quads) {
            out.add(tinted(quad, grid));
        }
        return out;
    }

    /* Whether any source's light can arrive within a block of the place, so its colour may show there. */
    private static boolean within(final List<ColouredLights.Source> sources, final BlockPos pos) {
        for (final ColouredLights.Source source : sources) {
            final int steps = Math.abs(source.x() - pos.getX()) + Math.abs(source.y() - pos.getY())
                    + Math.abs(source.z() - pos.getZ());
            if (LightMath.arriving(source.level(), steps - 3) > 0) {
                return true;
            }
        }
        return false;
    }

    /*
     * The colour the sources give each place around the block, by the way their light goes there, round walls and
     * through tinting blocks; and daylight's colour below tinting blocks. A source whose light the worker has not yet
     * spread goes straight, where the game's light shows it reaches.
     */
    private static TintGrid grid(final BlockAndTintGetter level, final BlockPos pos,
                                 final List<ColouredLights.Source> sources) {
        final int[] colours = new int[TintGrid.SIZE];
        final boolean[] lit = new boolean[TintGrid.SIZE];
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final List<LightMath.Arrival> arrivals = new ArrayList<>();
        final float daylight = ColouredLights.sky();
        final boolean shaded = ColouredLights.shaded(SectionPos.asLong(pos));
        final LightFlood[] floods = new LightFlood[sources.size()];
        for (int i = 0; i < floods.length; i++) {
            floods[i] = ColouredLights.flood(sources.get(i));
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    at.setWithOffset(pos, dx, dy, dz);
                    final int index = TintGrid.index(dx, dy, dz);
                    final int block = level.getBrightness(LightLayer.BLOCK, at);
                    final int sky = level.getBrightness(LightLayer.SKY, at);
                    lit[index] = block > 0 || sky > 0;
                    arrivals.clear();
                    for (int i = 0; i < floods.length; i++) {
                        final ColouredLights.Source source = sources.get(i);
                        final LightFlood flood = floods[i];
                        final int arriving = flood != null ? flood.level(at.getX(), at.getY(), at.getZ())
                                : LightMath.arriving(source.level(), Math.abs(source.x() - at.getX())
                                + Math.abs(source.y() - at.getY()) + Math.abs(source.z() - at.getZ()));
                        if (arriving > 0) {
                            arrivals.add(new LightMath.Arrival(arriving, flood != null ? flood.colour(at.getX(),
                                    at.getY(), at.getZ()) : source.colour()));
                        }
                    }
                    final int daylit = Math.round(sky * daylight);
                    int colour = arrivals.isEmpty() ? LightMath.WHITE : LightMath.tint(arrivals, block, daylit);
                    if (shaded && sky > 0) {
                        colour = LightMath.skyThrough(colour, ColouredLights.skyTint(at), daylit);
                    }
                    colours[index] = colour;
                }
            }
        }
        return new TintGrid(colours, lit);
    }

    /* The face with each corner's colour multiplied by the light's colour where the corner looks. */
    private static BakedQuad tinted(final BakedQuad quad, final TintGrid grid) {
        final int[] data = quad.getVertices();
        if (data.length < CORNERS * STRIDE || data[LIGHT] == LightTexture.FULL_BRIGHT) {
            // A face that glows of itself, as a pack's glowing texture, gives light rather than takes it.
            return quad;
        }
        final Direction facing = quad.getDirection();
        final int[] out = data.clone();
        boolean changed = false;
        for (int i = 0; i < CORNERS; i++) {
            final int tint = grid.at(Float.intBitsToFloat(data[i * STRIDE + X]) + facing.getStepX() * HALF,
                    Float.intBitsToFloat(data[i * STRIDE + Y]) + facing.getStepY() * HALF,
                    Float.intBitsToFloat(data[i * STRIDE + Z]) + facing.getStepZ() * HALF);
            if (tint != LightMath.WHITE) {
                out[i * STRIDE + COLOUR] = multiplied(data[i * STRIDE + COLOUR], tint);
                changed = true;
            }
        }
        return changed ? new BakedQuad(out, quad.getTintIndex(), facing, quad.getSprite(), quad.isShade(),
                quad.hasAmbientOcclusion()) : quad;
    }

    /* A corner's colour, kept red, green, blue and alpha from the lowest byte up, multiplied by {@code 0xRRGGBB}. */
    private static int multiplied(final int abgr, final int rgb) {
        final int red = (abgr & CHANNEL) * (rgb >> BLUE_SHIFT & CHANNEL) / CHANNEL;
        final int green = (abgr >> GREEN_SHIFT & CHANNEL) * (rgb >> GREEN_SHIFT & CHANNEL) / CHANNEL;
        final int blue = (abgr >> BLUE_SHIFT & CHANNEL) * (rgb & CHANNEL) / CHANNEL;
        return abgr & CHANNEL << ALPHA_SHIFT | blue << BLUE_SHIFT | green << GREEN_SHIFT | red;
    }
}
