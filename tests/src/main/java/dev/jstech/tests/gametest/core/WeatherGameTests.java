/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.weather.WeatherKind;
import dev.jstech.core.weather.Weathers;
import dev.jstech.core.world.event.WorldEventBalance;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSkies;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The weather kit: a weather comes and goes with its world event, round its place; out in it a body is harmed and
 * poisoned while one under a roof is spared, unless the server makes it harmless; it blows the wind three times as
 * hard within its reach and no harder beyond; and a weather a data pack writes is read with those declared in code.
 * Its tests start and stop events and change the server's settings, so they share a batch of their own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WeatherGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_weather";
    private static final BlockPos MIDDLE = new BlockPos(2, 2, 2);
    private static final BlockPos OUT = new BlockPos(1, 2, 1);
    private static final BlockPos SHELTERED = new BlockPos(3, 2, 3);
    private static final int BEYOND = 40;

    private WeatherGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void weather_harmsWhatStandsOutInItAndSparesWhatIsSheltered(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos centre = helper.absolutePos(MIDDLE);
        final Cow out = helper.spawn(EntityType.COW, OUT);
        final Cow sheltered = helper.spawn(EntityType.COW, SHELTERED);
        helper.setBlock(SHELTERED.above(3), Blocks.STONE);
        helper.assertTrue(WorldEvents.start(level, TestSkies.ACID_RAIN, 1_000, Optional.of(centre)),
                "the acid rain starts round the arena");
        helper.runAfterDelay(190, () -> WorldEvents.stop(level, TestSkies.ACID_RAIN));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!Weathers.at(level, centre).isEmpty(),
                        "the weather is under way at the arena"))
                .thenExecute(() -> {
                    // Each cow's health before the weather is what it is weighed against: tests running beside
                    // this one may have touched either since it was spawned.
                    final float before = out.getHealth();
                    final float shelteredBefore = sheltered.getHealth();
                    Weathers.weather(level, out);
                    Weathers.weather(level, sheltered);
                    helper.assertTrue(out.hasEffect(MobEffects.POISON) && out.getHealth() < before,
                            "the cow out in it is harmed and poisoned");
                    helper.assertTrue(!sheltered.hasEffect(MobEffects.POISON)
                            && sheltered.getHealth() == shelteredBefore, "the cow under a roof is spared");
                    helper.assertTrue(Weathers.windAt(level, centre) == 3, "the wind blows three times as hard in it");
                    helper.assertTrue(Weathers.windAt(level, centre.east(BEYOND)) == 1, "and as it was beyond");
                })
                .thenExecute(() -> {
                    out.removeAllEffects();
                    WorldEventBalance.setHarmless(List.of(TestSkies.ACID_RAIN.toString()));
                    Weathers.weather(level, out);
                    WorldEventBalance.setHarmless(List.of());
                    helper.assertTrue(!out.hasEffect(MobEffects.POISON), "made harmless, it poisons no one");
                })
                .thenExecute(() -> WorldEvents.stop(level, TestSkies.ACID_RAIN))
                .thenWaitUntil(() -> helper.assertTrue(Weathers.at(level, centre).isEmpty(),
                        "stopped, the weather is gone"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void weathers_areReadFromDataBesideThoseDeclared(final GameTestHelper helper) {
        final WeatherKind snow = Weathers.kind(helper.getLevel(),
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_methane_snow"));
        helper.assertTrue(snow != null && snow.falls() == WeatherKind.Fall.SNOW && snow.colour() == 0xE0D8FF
                && snow.event().equals(TestSkies.SPARK_STORM), "the data pack's methane snow comes with the storm");
        final WeatherKind rain = Weathers.kind(helper.getLevel(), TestSkies.ACID_RAIN_WEATHER);
        helper.assertTrue(rain != null && rain.harm().isPresent() && rain.effect().isPresent()
                && rain.sight() == 0.5, "the declared acid rain harms, poisons and closes the view in");
        helper.assertTrue(Weathers.all(helper.getLevel()).containsKey(TestSkies.SPARK_STORM_WEATHER),
                "and every kind is listed");
        helper.succeed();
    }
}
