/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSpaceWorlds;
import dev.jstech.tests.TestWorldGen;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A round world's size as J's Space fixes it the first time its dimension loads: the Overworld's border times its
 * width over the Earth's, its border at its far side and at its poles, half as tall as it is wide, and the Overworld's
 * own border left alone. The test mod's world, a thousand kilometres wide, is made for the test and taken away.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldSizeGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1e-6;

    private WorldSizeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void roundWorld_isSizedByItsWidthOverTheEarthsAsItLoads(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final SpaceWorld world = SpaceWorlds.get(helper.getLevel(), TestSpaceWorlds.WORLD).orElseThrow();
        final double overworld = server.overworld().getWorldBorder().getSize();
        final ServerLevel level = RuntimeDimensions.getOrCreate(server, world.dimension(), TestWorldGen.MOON.id());
        try {
            final DimensionBorders.Bounds bounds = DimensionBorders.of(level).orElse(null);
            final double width = SphereMap.widthOf(overworld, world.diameter());
            helper.assertTrue(bounds != null && Math.abs(bounds.maxX() - bounds.minX() - width) < CLOSE,
                    "the world is the Overworld's width times its own over the Earth's: " + bounds);
            helper.assertTrue(Math.abs(bounds.maxZ() - bounds.minZ() - width / 2) < CLOSE,
                    "and half as tall, its north and south ends at its poles");
            helper.assertTrue(server.overworld().getWorldBorder().getSize() == overworld,
                    "while the Overworld keeps its own border");
        } finally {
            RuntimeDimensions.remove(server, world.dimension());
        }
        helper.succeed();
    }
}
