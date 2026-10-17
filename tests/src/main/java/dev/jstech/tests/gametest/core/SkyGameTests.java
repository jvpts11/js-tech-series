/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.DayClocks;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkyMath;
import dev.jstech.core.sky.SkySpec;
import dev.jstech.core.sky.StarKind;
import dev.jstech.core.time.GameCalendar;
import dev.jstech.core.time.Season;
import dev.jstech.core.time.WorldCalendar;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Each dimension's own day, year and sky: a land whose day runs twice as fast moves twice as far as the overworld,
 * whose own day is never touched; a land locked to its star holds its sun still; a land's time is kept when it is
 * closed and made again; its calendar follows its own year and its world goes round its orbit; its wind blows as hard
 * as its rules say; and its sky is read from data, its copies taking their template's, its moon eclipsing its sun at
 * noon as the geometry says. The clock's tests run the daylight cycle, so they share a batch of their own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SkyGameTests {

    private static final String ARENA = "empty";
    private static final String CLOCK_BATCH = "jscore_day_clock";
    private static final int WATCHED = 100;
    private static final long NOON_OF_DAY_ONE = 30_000L;
    private static final long DAWN_OF_DAY_ONE = 24_000L;

    private SkyGameTests() {
    }

    @GameTest(template = ARENA, batch = CLOCK_BATCH, timeoutTicks = 400)
    public static void clock_movesADimensionsDayAtItsOwnPaceAndLeavesTheOverworlds(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("clock_sky_land");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.SKY_LAND.id());
        final GameRules.BooleanValue daylight = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        final boolean was = daylight.get();
        daylight.set(true, server);
        final long overworldFrom = server.overworld().getDayTime();
        final long landFrom = land.getDayTime();
        helper.assertTrue(DayClocks.keepsOwn(land) && !DayClocks.keepsOwn(server.overworld()),
                "the land keeps a clock of its own, the overworld none of the Core's");
        helper.startSequence()
                .thenExecuteAfter(WATCHED, () -> {
                    final long overworld = server.overworld().getDayTime() - overworldFrom;
                    final long moved = land.getDayTime() - landFrom;
                    daylight.set(was, server);
                    RuntimeDimensions.remove(server, key);
                    helper.assertTrue(Math.abs(moved - 2 * overworld) <= 4, "the land's day, twice as fast, moved "
                            + moved + " while the overworld's moved " + overworld);
                    helper.assertTrue(overworld > 0 && overworld <= WATCHED + 2, "the overworld's day moved at its "
                            + "own pace, " + overworld);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = CLOCK_BATCH, timeoutTicks = 200)
    public static void clock_holdsTheSunOfALandLockedToItsStar(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("clock_locked_land");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.LOCKED_LAND.id());
        final GameRules.BooleanValue daylight = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        final boolean was = daylight.get();
        daylight.set(true, server);
        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final long time = land.getDayTime();
                    final float perTick = land.getDayTimePerTick();
                    daylight.set(was, server);
                    RuntimeDimensions.remove(server, key);
                    helper.assertTrue(time == 6_000L, "the sun is held at noon; the clock shows " + time);
                    helper.assertTrue(perTick == 0F, "and the players are told it does not move; " + perTick);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = CLOCK_BATCH, timeoutTicks = 200)
    public static void clock_keepsALandsTimeWhenItIsClosedAndMadeAgain(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("clock_kept_land");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.SKY_LAND.id());
        land.setDayTime(12_345L);
        DayClocks.advance(land);
        RuntimeDimensions.remove(server, key);
        final ServerLevel again = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.SKY_LAND.id());
        final long time = again.getDayTime();
        RuntimeDimensions.remove(server, key);
        helper.assertTrue(time >= 12_345L && time < 12_400L, "the land comes back at the time it had; " + time);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void calendar_followsALandsOwnYearRoundItsOrbit(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("calendar_sky_land");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.SKY_LAND.id());
        try {
            land.setDayTime(4L * GameCalendar.DAY_TICKS);
            helper.assertTrue(WorldCalendar.calendarOf(land).daysPerSeason() == 2,
                    "a year of eight days has seasons of two");
            helper.assertTrue(Math.abs(WorldCalendar.orbit(land) - 0.5) < 1e-9, "four days in, it is halfway round");
            helper.assertTrue(WorldCalendar.now(land).season() == Season.at(2), "and in its third season");
            helper.assertTrue(WorldCalendar.calendarOf(server.overworld()).equals(WorldCalendar.calendar()),
                    "the overworld keeps the calendar of the world's balance");
        } finally {
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wind_blowsAsHardAsALandsRulesSay(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("wind_sky_land");
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.SKY_LAND.id());
        try {
            final Vec3 at = new Vec3(0, 100, 0);
            final double here = Winds.at(land, at).speed();
            final double there = Winds.at(server.overworld(), at).speed();
            helper.assertTrue(there > 0 && Math.abs(here / there - 2) < 0.01,
                    "the land's wind blows twice as hard; " + here + " against " + there);
        } finally {
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void skies_areReadFromDataAndTheirMoonEclipsesTheSunAtNoon(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = key("skies_sky_land");
        RuntimeDimensions.getOrCreate(server, key, TestWorldGen.SKY_LAND.id());
        try {
            final SkySpec sky = Skies.of(key);
            helper.assertTrue(sky.suns().size() == 2 && sky.suns().get(1).kind() == StarKind.M,
                    "a copy takes its template's sky: a yellow sun and a red dwarf; got " + sky.suns());
            helper.assertTrue(sky.bodies().size() == 2 && sky.bodies().getFirst().rings().isPresent()
                    && sky.galaxy().isPresent() && sky.nebulae().size() == 2 && sky.aurora().isPresent()
                    && sky.clouds().isPresent() && sky.haze().isPresent(), "with its planet's rings, its galaxy, "
                    + "its nebulae, its aurora, its clouds and its haze");
            helper.assertTrue(Skies.of(Level.OVERWORLD).equals(SkySpec.DEFAULT), "the overworld keeps its own sky");
            helper.assertTrue(eclipse(sky, NOON_OF_DAY_ONE) > 0.99, "the moon covers the sun at noon; "
                    + eclipse(sky, NOON_OF_DAY_ONE));
            helper.assertTrue(eclipse(sky, DAWN_OF_DAY_ONE) == 0, "and is a quarter of the sky away at dawn");
            helper.assertTrue(SkyMath.sunlight(sky.suns().get(1).distance()) < 0.2,
                    "the red dwarf, two and a half times as far, gives a sixth of the light");
        } finally {
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    /* How much of the first sun the second body hides at {@code dayTime}. */
    private static double eclipse(final SkySpec sky, final long dayTime) {
        final double celestial = SkyMath.celestial(dayTime);
        final SkySpec.Sun sun = sky.suns().getFirst();
        final SkySpec.Body moon = sky.bodies().get(1);
        final double[] towards = SkyMath.direction(celestial + sun.offset(), 0);
        final double[] at = SkyMath.direction(celestial + SkyMath.drift(moon.offset(), moon.orbitDays(),
                dayTime / SkyMath.DAY), moon.tilt());
        return SkyMath.coverage(SkyMath.sunRadius(sun.apparentSize()), SkyMath.bodyRadius(moon.size()),
                SkyMath.separation(towards, at));
    }

    private static ResourceKey<Level> key(final String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path));
    }
}
