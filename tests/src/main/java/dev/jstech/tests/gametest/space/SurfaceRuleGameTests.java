/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.geology.SpaceRocks;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.worldgen.MoonBiomes;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The surfaces of the worlds of space as the game reads them, block by block down every column: a biome's or a noise's
 * test, far costlier than a depth's, is made only as deep under the floor as the rules under it lay anything, and a
 * biome is read once a column. Tested down the whole column, it made the Moon and Mars several times slower to
 * generate than the Overworld.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SurfaceRuleGameTests {

    private static final String ARENA = "empty";
    /* The conditions costly to test, and the test of how deep under the floor a block lies. */
    private static final List<String> COSTLY = List.of("minecraft:biome", "jscore:column_biome",
            "minecraft:noise_threshold");
    private static final String DEPTH = "minecraft:stone_depth";
    /* The game's own test of the biome, which reads it anew at every block. */
    private static final String EVERY_BLOCK = "\"minecraft:biome\"";
    /* The grid a biome's column is looked for on, its side in points; where on the Moon, longitude and latitude. */
    private static final int GRID = 21;
    private static final double[] TRANQUILLITY = {31, 8};
    private static final double[] SOUTHERN_HIGHLANDS = {20, -45};

    private SurfaceRuleGameTests() {
    }

    @GameTest(template = ARENA)
    public static void surface_ofTheMoonAndMarsTestsItsBiomesOnlyNearTheTop(final GameTestHelper helper) {
        for (final SolarBody body : List.of(SolarBody.MOON, SolarBody.MARS)) {
            final ServerLevel level = levelOf(helper.getLevel(), body);
            final SurfaceRules.RuleSource rule = ((NoiseBasedChunkGenerator) level.getChunkSource().getGenerator())
                    .generatorSettings().value().surfaceRule();
            final JsonElement read = SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE, rule).getOrThrow();
            final List<String> deep = new ArrayList<>();
            costlyDown(read, false, deep);
            helper.assertTrue(deep.isEmpty(), body + "'s surface tests these down the whole column, not only as deep "
                    + "as its rules lay anything: " + deep);
            helper.assertTrue(!read.toString().contains(EVERY_BLOCK), body + "'s surface reads its biomes once a "
                    + "column, not anew at every block");
        }
        helper.succeed();
    }

    /*
     * The biome read once a column lays each biome's own ground: a column of a mare made has the mare's regolith or
     * basalt at its top, and one of the highlands neither.
     */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void surface_ofAMareIsTheMaresOwnAndOfTheHighlandsNot(final GameTestHelper helper) {
        final ServerLevel moon = levelOf(helper.getLevel(), SolarBody.MOON);
        final double width = moon.getWorldBorder().getSize();
        // Round the Sea of Tranquillity, and round the highlands south of the Sea of Nectar.
        final BlockPos mare = columnOf(moon, MoonBiomes.MARE.key(), SphereMap.x(TRANQUILLITY[0], width),
                SphereMap.z(TRANQUILLITY[1], width), width);
        final BlockPos highland = columnOf(moon, MoonBiomes.HIGHLANDS.key(), SphereMap.x(SOUTHERN_HIGHLANDS[0],
                width), SphereMap.z(SOUTHERN_HIGHLANDS[1], width), width);
        final Block mareTop = topOf(moon, mare);
        final Block highlandTop = topOf(moon, highland);
        final Set<Block> mareGround = Set.of(SpaceRocks.MARE_REGOLITH.get(), SpaceRocks.MARE_BASALT.get());
        helper.assertTrue(mareGround.contains(mareTop), "the mare at " + mare + " has its own ground on top: "
                + mareTop);
        helper.assertTrue(!mareGround.contains(highlandTop), "the highlands at " + highland + " have not: "
                + highlandTop);
        helper.succeed();
    }

    /* A column of {@code biome} round ({@code x0}, {@code z0}), found on a grid a degree of the world apart. */
    private static BlockPos columnOf(final ServerLevel level, final ResourceKey<Biome> biome, final double x0,
                                     final double z0, final double width) {
        final Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        final double step = SphereMap.blocksPerDegree(width);
        for (int i = 0; i < GRID * GRID; i++) {
            final int x = (int) (x0 + (i % GRID - GRID / 2) * step);
            final int z = (int) (z0 + (i / GRID - GRID / 2) * step);
            if (level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                    QuartPos.fromBlock(level.getSeaLevel()), QuartPos.fromBlock(z), sampler).is(biome)) {
                return new BlockPos(x, 0, z);
            }
        }
        throw new IllegalStateException("no column of " + biome.location() + " on the grid");
    }

    /* The block on top of the ground of the column at {@code column}, its chunk made whole. */
    private static Block topOf(final ServerLevel level, final BlockPos column) {
        final ChunkAccess chunk = level.getChunk(column);
        final int top = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, column.getX() & 15, column.getZ() & 15);
        return level.getBlockState(column.atY(top)).getBlock();
    }

    /* Into {@code out}, the costly conditions of {@code rule} tested at any depth; {@code near}, under a depth's. */
    private static void costlyDown(final JsonElement rule, final boolean near, final List<String> out) {
        if (!rule.isJsonObject()) {
            return;
        }
        final JsonObject node = rule.getAsJsonObject();
        if (node.has("sequence")) {
            node.getAsJsonArray("sequence").forEach(each -> costlyDown(each, near, out));
        }
        if (node.has("if_true")) {
            final JsonObject condition = node.getAsJsonObject("if_true");
            final String type = condition.get("type").getAsString();
            if (!near && COSTLY.contains(type)) {
                out.add(condition.toString());
            }
            costlyDown(node.get("then_run"), near || DEPTH.equals(type), out);
        }
    }

    private static ServerLevel levelOf(final ServerLevel any, final SolarBody world) {
        final ResourceKey<Level> key = SpaceWorlds.get(any, SolarSystemWorlds.worldOf(world)).orElseThrow()
                .dimension();
        return RuntimeDimensions.getOrCreate(any.getServer(), key, key.location());
    }
}
