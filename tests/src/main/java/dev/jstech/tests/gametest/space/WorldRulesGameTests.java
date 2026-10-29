/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.DayClocks;
import dev.jstech.core.dimension.DimensionClock;
import dev.jstech.core.dimension.DimensionEffects;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Each world's rules, by J's Core's kits, on the Moon and Mars themselves: their pull on whatever stands there, a
 * sixth of the Earth's and two fifths; their air, none on the Moon and too thin on Mars, which no one breathes; their
 * cold; and their days, a month long on the Moon and a sol on Mars, as their clocks turn.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldRulesGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_world_rules";
    private static final double CLOSE = 1e-6;
    /* The game's own pull, in blocks a tick each tick; the Moon's and Mars' shares of it. */
    private static final double EARTH_PULL = 0.08;
    private static final double MOON_PULL = 0.165;
    private static final double MARS_PULL = 0.38;
    /* The Moon's day, a synodic month, and a sol, in ticks of a day 24,000 long. */
    private static final int MOON_DAY = 708_734;
    private static final int SOL = 24_660;
    /* Where the player stands to weigh the air, high over any ground. */
    private static final double HIGH = 200;
    private static final int TICKS = 1_000;

    private WorldRulesGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void pull_isASixthOnTheMoonAndTwoFifthsOnMars(final GameTestHelper helper) {
        helper.assertTrue(Math.abs(pull(world(helper, MoonWorld.world().dimension())) - EARTH_PULL * MOON_PULL)
                < CLOSE, "what stands on the Moon is pulled with a sixth of the Earth's pull");
        helper.assertTrue(Math.abs(pull(world(helper, MarsWorld.world().dimension())) - EARTH_PULL * MARS_PULL)
                < CLOSE, "and on Mars with two fifths");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void air_isBreathedOnNeitherAndBothAreCold(final GameTestHelper helper) {
        final Set<DimensionHazardEvent.Hazard> moon = weigh(world(helper, MoonWorld.world().dimension()));
        helper.assertTrue(moon.contains(DimensionHazardEvent.Hazard.AIR), "the Moon has no air; " + moon);
        final Set<DimensionHazardEvent.Hazard> mars = weigh(world(helper, MarsWorld.world().dimension()));
        helper.assertTrue(mars.contains(DimensionHazardEvent.Hazard.AIR) && mars.contains(
                DimensionHazardEvent.Hazard.COLD), "Mars' thin air of carbon dioxide is breathed by no one, and it"
                + " is cold; " + mars);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void days_lastAMonthOnTheMoonAndASolOnMars(final GameTestHelper helper) {
        final ServerLevel moon = world(helper, MoonWorld.world().dimension());
        final ServerLevel mars = world(helper, MarsWorld.world().dimension());
        helper.assertTrue(DayClocks.of(moon).map(DimensionClock::day).orElse(0) == MOON_DAY,
                "the Moon keeps a clock of its own, its day a month long");
        helper.assertTrue(DayClocks.of(mars).map(DimensionClock::day).orElse(0) == SOL, "and Mars, its day a sol");
        final double moonTurned = turned(moon);
        final double marsTurned = turned(mars);
        helper.assertTrue(Math.abs(moonTurned - TICKS * (double) DimensionClock.OVERWORLD_DAY / MOON_DAY) < 1,
                "the Moon's clock turns as slowly as its long day: " + moonTurned + " in " + TICKS + " ticks");
        helper.assertTrue(Math.abs(marsTurned - TICKS * (double) DimensionClock.OVERWORLD_DAY / SOL) < 1,
                "and Mars' a little slower than the Earth's: " + marsTurned + " in " + TICKS + " ticks");
        helper.succeed();
    }

    /* The world {@code key}, made from its dimension's file under its own name: the test server makes only its own. */
    private static ServerLevel world(final GameTestHelper helper, final ResourceKey<Level> key) {
        return RuntimeDimensions.getOrCreate(helper.getLevel().getServer(), key, key.location());
    }

    /* The pull on a stand put in {@code level}, as the game moves it each tick. */
    private static double pull(final ServerLevel level) {
        final ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(0.5, HIGH, 0.5);
        level.addFreshEntity(stand);
        final double pull = stand.getAttributeValue(Attributes.GRAVITY);
        stand.discard();
        return pull;
    }

    /* What a player standing high in {@code level} suffers from its air and its warmth. */
    private static Set<DimensionHazardEvent.Hazard> weigh(final ServerLevel level) {
        final ServerPlayer player = GameTestPlayers.join(level, UUID.randomUUID(), "weigher");
        player.setPos(0.5, HIGH, 0.5);
        final Set<DimensionHazardEvent.Hazard> hazards = DimensionEffects.weigh(player);
        GameTestPlayers.leave(player);
        return hazards;
    }

    /* How far {@code level}'s clock moves in TICKS of its own turning, counted as the game's day is. */
    private static double turned(final ServerLevel level) {
        final long before = level.getDayTime();
        final float fraction = level.getDayTimeFraction();
        for (int i = 0; i < TICKS; i++) {
            DayClocks.advance(level);
        }
        return level.getDayTime() + level.getDayTimeFraction() - before - fraction;
    }
}
