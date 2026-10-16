/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.worldgen.CoreFeatures;
import dev.jstech.core.worldgen.biome.BiomeClimate;
import dev.jstech.core.worldgen.biome.BiomeClimates;
import dev.jstech.core.worldgen.landform.LandformConfig;
import dev.jstech.core.worldgen.landform.LandformField;
import dev.jstech.core.worldgen.landform.LandformKind;
import dev.jstech.core.worldgen.landform.SeaConfig;
import dev.jstech.core.worldgen.strata.StrataConfig;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.testkit.LandSurvey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Core's terrain, biomes and shapes of the land declared in code: they reach the game's registries through the
 * files the generator wrote, biomes keep their climates in real values, which heat meets, the configurations travel
 * through their files unchanged, and a land made by them is generated as they say.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TerrainGameTests {

    private static final String ARENA = "empty";
    /* The game keeps a biome's temperature and downfall as floats, so its values read back to about this. */
    private static final double CLOSE = 1e-4;
    /* Searched along a row of the land's cells for a crater standing clear of every other landform. */
    private static final int CELLS_SEARCHED = 64;

    private TerrainGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declarations_reachTheRegistries(final GameTestHelper helper) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        helper.assertTrue(registries.registryOrThrow(Registries.BIOME).containsKey(TestWorldGen.STEPPE.id())
                && registries.registryOrThrow(Registries.BIOME).containsKey(TestWorldGen.FROST.id()),
                "the declared biomes are registered");
        helper.assertTrue(registries.registryOrThrow(Registries.NOISE_SETTINGS).containsKey(TestWorldGen.TERRAIN.id()),
                "the terrain's noise settings are registered");
        helper.assertTrue(registries.registryOrThrow(Registries.NOISE)
                .containsKey(TestWorldGen.TERRAIN.noise("hills").location()), "and the noises they sample");
        final Registry<ConfiguredFeature<?, ?>> configured = registries.registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (final ResourceLocation id : List.of(TestWorldGen.CRATERS.id(), TestWorldGen.LAKES.id(),
                TestWorldGen.STRATA.id(), TestWorldGen.SEAS.id())) {
            helper.assertTrue(configured.containsKey(id), "the shape of the land " + id + " has its feature");
            helper.assertTrue(registries.registryOrThrow(Registries.PLACED_FEATURE).containsKey(id),
                    "and its placement");
            helper.assertTrue(registries.registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).containsKey(id),
                    "and the biome modifier that adds it");
        }
        helper.assertTrue(configured.get(TestWorldGen.CRATERS.id()).feature() == CoreFeatures.LANDFORM.get(),
                "landforms are the Core's landform feature");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void climates_areTheBiomesOwnOrTheGamesValues(final GameTestHelper helper) {
        final Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        final BiomeClimate plains = BiomeClimates.of(biomes.getHolderOrThrow(Biomes.PLAINS));
        helper.assertTrue(BiomeClimates.declared(biomes.getHolderOrThrow(Biomes.PLAINS)).isEmpty(),
                "a biome of the game's own has no climate of its own");
        helper.assertTrue(Math.abs(plains.temperature() - 14) < CLOSE && Math.abs(plains.rain() - 800) < CLOSE
                && Math.abs(plains.seasons() - 10) < CLOSE, "plains read as a mild 14 °C with 800 mm of rain, not "
                + plains);
        final Holder<Biome> steppe = biomes.getHolderOrThrow(TestWorldGen.STEPPE.key());
        final Optional<BiomeClimate> own = BiomeClimates.declared(steppe);
        helper.assertTrue(own.isPresent() && Math.abs(own.get().temperature() - 9.5) < CLOSE
                && Math.abs(own.get().seasons() - 16) < CLOSE && Math.abs(own.get().rain() - 240) < CLOSE,
                "a declared biome keeps the climate it was given, not " + own);
        helper.assertTrue(Math.abs(steppe.value().getBaseTemperature() - own.get().gameTemperature()) < CLOSE,
                "and its game temperature follows from it");
        final BiomeClimate frost = BiomeClimates.of(biomes.getHolderOrThrow(TestWorldGen.FROST.key()));
        helper.assertTrue(Math.abs(frost.seasons() - 14) < CLOSE, "a frozen biome swings hard with the seasons");
        final BlockPos here = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(Math.abs(HeatClimate.ambient(helper.getLevel(), here)
                        - HeatMath.kelvin(DimensionRulesData.of(helper.getLevel()).temperature())) < CLOSE,
                "heat meets a biome of the game's own in its dimension's air, as it always did");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void configurations_travelThroughTheirFilesUnchanged(final GameTestHelper helper) {
        final LandformConfig landform = new LandformConfig(LandformKind.MESA, 128, 0.5, 6, 30, 9, 0.4,
                Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(), Blocks.ICE.defaultBlockState(),
                List.of(Blocks.RED_TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState()), 260, 380,
                Optional.empty());
        final StrataConfig strata = new StrataConfig(List.of(List.of(new StrataConfig.Layer(
                Blocks.TUFF.defaultBlockState(), 7)), List.of(new StrataConfig.Layer(Blocks.CALCITE.defaultBlockState(),
                3))), 512, 3, 21, BlockTags.BASE_STONE_OVERWORLD);
        final SeaConfig sea = new SeaConfig(Blocks.LAVA.defaultBlockState(), Blocks.BASALT.defaultBlockState(), 1400,
                3000);
        final BiomeClimate climate = new BiomeClimate(-3.5, 12, 410);
        helper.assertTrue(roundTrip(LandformConfig.CODEC, landform).equals(landform), "a landform's file reads back");
        helper.assertTrue(roundTrip(StrataConfig.CODEC, strata).equals(strata), "rock in layers' file reads back");
        helper.assertTrue(roundTrip(SeaConfig.CODEC, sea).equals(sea), "a sea's file reads back");
        helper.assertTrue(roundTrip(BiomeClimate.CODEC, climate).equals(climate), "a biome's climate reads back");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void testLand_isMadeAsItsDeclarationsSay(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "land_" + UUID.randomUUID().toString()
                        .substring(0, 8)));
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final ChunkGenerator generator = land.getChunkSource().getGenerator();
            final RandomState random = land.getChunkSource().randomState();
            final Registry<Biome> biomes = land.registryAccess().registryOrThrow(Registries.BIOME);
            final Holder<Biome> steppe = biomes.getHolderOrThrow(TestWorldGen.STEPPE.key());
            helper.assertTrue(generator.getBiomeSource().possibleBiomes().contains(steppe)
                    && generator.getBiomeSource().possibleBiomes()
                    .contains(biomes.getHolderOrThrow(TestWorldGen.FROST.key())), "the land holds both its biomes");
            final LandformField.Instance crater = LandSurvey.clear(land, TestWorldGen.CRATERS, steppe,
                    List.of(TestWorldGen.CRATERS, TestWorldGen.LAKES), CELLS_SEARCHED);
            helper.assertTrue(crater != null, "some crater of the land lies clear of the others on the steppe");
            land.getChunk(crater.x() >> 4, crater.z() >> 4);
            final int base = generator.getBaseHeight(crater.x(), crater.z(), Heightmap.Types.OCEAN_FLOOR_WG, land,
                    random) - 1;
            final int floor = land.getHeight(Heightmap.Types.OCEAN_FLOOR, crater.x(), crater.z()) - 1;
            helper.assertTrue(floor < base, "the crater is dug into the land's ground: its floor at " + floor
                    + ", the ground at " + base);
            final BlockState bottom = land.getBlockState(new BlockPos(crater.x(), floor, crater.z()));
            helper.assertTrue(bottom.is(Blocks.GRAVEL), "and floored with its gravel, not " + bottom);
            final BlockState under = land.getBlockState(new BlockPos(crater.x(), floor - 1, crater.z()));
            helper.assertTrue(under.is(Blocks.TUFF) || under.is(Blocks.CALCITE),
                    "under it the rock lies in the land's layers, not " + under);
            final BlockPos above = new BlockPos(crater.x(), floor + 1, crater.z());
            helper.assertTrue(Math.abs(HeatClimate.ambient(land, above)
                            - HeatMath.kelvin(BiomeClimates.celsius(land, above))) < CLOSE,
                    "and heat meets the steppe's own climate there");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }

    private static <T> T roundTrip(final Codec<T> codec, final T value) {
        final JsonElement written = codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        return codec.parse(JsonOps.INSTANCE, written).getOrThrow();
    }
}
