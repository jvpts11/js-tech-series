/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.DimensionClock;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkySpec;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.space.universe.WorldShapes;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSpaceWorlds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Space's world model as the server holds it: the Solar System's three worlds of the skeleton, each its own
 * generator, the rules its dimension's file gives (the Moon's long day and a sixth of the pull, Mars' sol, its thin
 * air of carbon dioxide) and the sky its sky file gives (the Moon's black, Mars' butterscotch with blue sunsets, a
 * system's space black by its template); and a world put together from its layers, its generator's ground with each
 * modifier laid over in order, as the test mod's world is.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceWorldsGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1e-9;
    /* The Moon's day, a synodic month of 29.53 days at the Overworld's 24,000 ticks a day, and Mars' sol. */
    private static final int MOON_DAY = 708_734;
    private static final int SOL = 24_660;
    /* Mars' year, 1.88 of the Earth's, at the game's year of seven of the Overworld's days, in sols. */
    private static final int MARS_YEAR = 13;
    /* The colours of the skies: the Moon's and space's, Mars' at noon and at sunset; how far Mars is from the Sun. */
    private static final int BLACK = 0x000000;
    private static final int BUTTERSCOTCH = 0xC99A6E;
    private static final int BLUE_SUNSET = 0x5E8FD6;
    private static final double MARS_FROM_THE_SUN = 1.52;
    /* A height in the test world's middle, and one near its top. */
    private static final int MIDDLE = 64;
    private static final int HIGH = 127;

    private SpaceWorldsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void solarSystem_holdsTheOverworldTheMoonAndMarsEachItsOwn(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        for (final ResourceLocation id : new ResourceLocation[] {SolarSystemWorlds.OVERWORLD, SolarSystemWorlds.MOON,
                SolarSystemWorlds.MARS}) {
            final SpaceWorld world = SpaceWorlds.get(level, id).orElse(null);
            helper.assertTrue(world != null && world.system().equals(StarSystems.SOLAR_SYSTEM),
                    id + " is a world of the Solar System");
            helper.assertTrue(Language.getInstance().has(world.name()), id + " has its name in the language file");
        }
        helper.assertTrue(SpaceWorlds.ofDimension(level, Level.OVERWORLD).filter(world -> world.generator().isEmpty())
                .isPresent(), "the Overworld is a world whose dimension generates itself");
        helper.assertTrue(SpaceWorlds.get(level, SolarSystemWorlds.MOON).flatMap(SpaceWorld::generator)
                .filter(generator -> generator.getPath().equals("cratered")).isPresent()
                && SpaceWorlds.get(level, SolarSystemWorlds.MARS).flatMap(SpaceWorld::generator)
                .filter(generator -> generator.getPath().equals("arid")).isPresent(),
                "the Moon is shaped by impacts and Mars by wind");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void rules_ofTheMoonAndMarsAreThoseOfTheirFiles(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final DimensionRules moon = DimensionRulesData.of(SpaceWorlds.get(level, SolarSystemWorlds.MOON)
                .orElseThrow().dimension());
        helper.assertTrue(Math.abs(moon.gravity() - 0.165) < CLOSE && !moon.breathable() && moon.pressure() == 0
                && moon.clock().map(DimensionClock::day).orElse(0) == MOON_DAY,
                "the Moon: a sixth of the pull, no air, a day of a month: " + moon);
        final DimensionRules mars = DimensionRulesData.of(SpaceWorlds.get(level, SolarSystemWorlds.MARS)
                .orElseThrow().dimension());
        // Its air is said, so whether it can be breathed is read from its oxygen, as the climate at a height has it.
        helper.assertTrue(Math.abs(mars.gravity() - 0.38) < CLOSE && !mars.at(MIDDLE).breathable()
                && mars.clock().filter(clock -> clock.day() == SOL && clock.year() == MARS_YEAR).isPresent()
                && mars.air().filter(air -> air.share(AirMix.CARBON_DIOXIDE) > 0.9).isPresent(),
                "Mars: two fifths of the pull, a sol, thin air of carbon dioxide: " + mars);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void skies_ofTheMoonMarsAndSpaceAreTheirOwn(final GameTestHelper helper) {
        final Level level = helper.getLevel();
        final SkySpec moon = Skies.of(SpaceWorlds.get(level, SolarSystemWorlds.MOON).orElseThrow().dimension());
        helper.assertTrue(moon.skyColour().filter(colour -> colour == BLACK).isPresent()
                && moon.clouds().filter(clouds -> clouds.density() == 0).isPresent(),
                "the Moon's sky is black, with no air for clouds");
        final SkySpec mars = Skies.of(SpaceWorlds.get(level, SolarSystemWorlds.MARS).orElseThrow().dimension());
        helper.assertTrue(mars.skyColour().filter(colour -> colour == BUTTERSCOTCH).isPresent()
                && mars.sunsetColour().filter(colour -> colour == BLUE_SUNSET).isPresent(),
                "Mars' sky is butterscotch, with blue sunsets");
        helper.assertTrue(Math.abs(mars.suns().getFirst().distance() - MARS_FROM_THE_SUN) < CLOSE
                && mars.suns().getFirst().apparentSize() < 1, "and its sun, farther, looks smaller");
        helper.assertTrue(Skies.of(ResourceKey.create(Registries.DIMENSION, SystemSpaces.TEMPLATE.id())).skyColour()
                .filter(colour -> colour == BLACK).isPresent(), "every system's space is black, by its template");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void world_isItsGeneratorWithEachModifierLaidOverInOrder(final GameTestHelper helper) {
        final SpaceWorld world = SpaceWorlds.get(helper.getLevel(), TestSpaceWorlds.WORLD).orElseThrow();
        final HolderGetter<NormalNoise.NoiseParameters> noises = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.NOISE);
        final HolderGetter<DensityFunction> densities = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.DENSITY_FUNCTION);
        final DensityFunction ground = WorldShapes.ground(world, noises, densities);
        helper.assertTrue(Math.abs(ground.compute(new DensityFunction.SinglePointContext(0, MIDDLE, 0))
                - TestSpaceWorlds.RAISED_BY) < CLOSE, "the generator's ground, raised by the first modifier");
        helper.assertTrue(ground.compute(new DensityFunction.SinglePointContext(0, HIGH, 0)) < 0,
                "and air still near the top");
        helper.assertTrue(WorldShapes.surface(world).equals(SurfaceRules.sequence(
                SurfaceRules.state(Blocks.DEEPSLATE.defaultBlockState()),
                SurfaceRules.state(Blocks.STONE.defaultBlockState()))),
                "the second modifier paves over the generator's surface");
        boolean refused = false;
        try {
            WorldShapes.ground(SpaceWorlds.get(helper.getLevel(), SolarSystemWorlds.OVERWORLD).orElseThrow(), noises,
                    densities);
        } catch (final IllegalArgumentException generatesItself) {
            refused = true;
        }
        helper.assertTrue(refused, "a world whose dimension generates itself has no ground to put together");
        helper.succeed();
    }
}
