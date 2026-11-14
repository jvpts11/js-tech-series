/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.universe.MarsWeather;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.tests.JsTests;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Mars' dust storms and the server's "Dust on Mars" setting: one blows over the whole planet, and with the setting
 * turned off it ends and none is planned, by the days or by an operator.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MarsWeatherGameTests {

    private static final String ARENA = "empty";
    /* Its own batch, so the setting turned off is seen by no other test, and put back after it. */
    private static final String BATCH = "jsspace_mars_dust";

    private MarsWeatherGameTests() {
    }

    @AfterBatch(batch = BATCH)
    public static void dustBack(final ServerLevel level) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.DUST_ON_MARS, true);
        final ServerLevel mars = mars(level);
        WorldEvents.stop(mars, MarsWeather.DUST_STORM);
        WorldEvents.stop(mars, MarsWeather.GLOBAL_DUST_STORM);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void dustOnMarsOff_endsTheStormBlowingAndPlansNone(final GameTestHelper helper) {
        final ServerLevel mars = mars(helper.getLevel());
        WorldEvents.stop(mars, MarsWeather.DUST_STORM);
        WorldEvents.stop(mars, MarsWeather.GLOBAL_DUST_STORM);
        helper.assertTrue(WorldEvents.start(mars, MarsWeather.GLOBAL_DUST_STORM, 0, Optional.empty())
                && WorldEvents.activeAt(mars, MarsWeather.GLOBAL_DUST_STORM, BlockPos.ZERO.east(5000)),
                "a global dust storm blows over the whole planet");
        SpaceServerConfig.FILE.set(SpaceServerConfig.DUST_ON_MARS, false);
        helper.assertTrue(!WorldEvents.plan(mars, MarsWeather.DUST_STORM, 40, Optional.of(BlockPos.ZERO)),
                "with dust on Mars off, no regional storm is planned");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(WorldEvents.planned(mars).isEmpty(),
                        "the storm blowing has not ended: " + WorldEvents.planned(mars)))
                .thenExecute(() -> {
                    helper.assertTrue(!WorldEvents.start(mars, MarsWeather.GLOBAL_DUST_STORM, 0, Optional.empty()),
                            "nor started by an operator");
                    SpaceServerConfig.FILE.set(SpaceServerConfig.DUST_ON_MARS, true);
                    helper.assertTrue(WorldEvents.plan(mars, MarsWeather.DUST_STORM, 40, Optional.of(BlockPos.ZERO)),
                            "turned back on, one is planned again");
                    WorldEvents.stop(mars, MarsWeather.DUST_STORM);
                })
                .thenSucceed();
    }

    /* Mars, made from its dimension's file under its own name: the test server makes only its flat preset's. */
    private static ServerLevel mars(final ServerLevel any) {
        final ResourceKey<Level> key = MarsWorld.world().dimension();
        return RuntimeDimensions.getOrCreate(any.getServer(), key, key.location());
    }
}
