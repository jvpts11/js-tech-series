/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A dimension's own border, kept by the Core: its bounds fixed once and kept; its game border across their width; the
 * overworld's changes handed on to every other dimension but it; and a player held inside its north and south ends.
 * Each test makes a copy of the test moon of its own, so the overworld's border is never touched.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DimensionBorderGameTests {

    private static final String ARENA = "empty";
    private static final DimensionBorders.Bounds BOUNDS = new DimensionBorders.Bounds(-100, 100, -50, 50);
    private static final double WIDTH = 200;
    private static final double OTHER_SIZE = 1_000;

    private DimensionBorderGameTests() {
    }

    @GameTest(template = ARENA)
    public static void fix_keepsTheFirstBoundsAndSetsTheGameBorderAcrossTheirWidth(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = copyKey();
        final ServerLevel level = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.MOON.id());
        try {
            helper.assertTrue(DimensionBorders.fix(level, BOUNDS).equals(BOUNDS), "the bounds are fixed");
            helper.assertTrue(level.getWorldBorder().getSize() == WIDTH && level.getWorldBorder().getCenterX() == 0
                    && level.getWorldBorder().getCenterZ() == 0, "and its game border spans their width");
            helper.assertTrue(DimensionBorders.fix(level, new DimensionBorders.Bounds(-1, 1, -1, 1)).equals(BOUNDS),
                    "fixing them again keeps the first");
        } finally {
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void overworldBorder_leavesADimensionWithItsOwnAlone(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = copyKey();
        final ServerLevel level = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.MOON.id());
        try {
            DimensionBorders.fix(level, BOUNDS);
            // What the game does to every other dimension when the overworld's border changes, as it hands it on.
            final WorldBorder overworld = new WorldBorder();
            new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder())
                    .onBorderSizeSet(overworld, OTHER_SIZE);
            helper.assertTrue(level.getWorldBorder().getSize() == WIDTH,
                    "the dimension that keeps its own border keeps it");
            final WorldBorder other = new WorldBorder();
            new BorderChangeListener.DelegateBorderChangeListener(other).onBorderSizeSet(overworld, OTHER_SIZE);
            helper.assertTrue(other.getSize() == OTHER_SIZE, "while any other border follows the overworld's");
        } finally {
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void poles_holdAPlayerInside(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> key = copyKey();
        final ServerLevel level = RuntimeDimensions.getOrCreate(server, key, TestWorldGen.MOON.id());
        final ServerPlayer player = GameTestPlayers.connected(helper, "pole_walker");
        try {
            DimensionBorders.fix(level, BOUNDS);
            player.teleportTo(level, 0, 20, BOUNDS.maxZ() + 30, 0, 0);
            player.doTick();
            helper.assertTrue(player.getZ() < BOUNDS.maxZ() && player.getZ() > BOUNDS.maxZ() - 1,
                    "a player past the south end is put back just inside it: " + player.getZ());
            player.teleportTo(level, 0, 20, 0, 0, 0);
            player.doTick();
            helper.assertTrue(player.getZ() == 0, "and one inside is left where they are");
        } finally {
            GameTestPlayers.leave(player);
            RuntimeDimensions.remove(server, key);
        }
        helper.succeed();
    }

    /* A copy of the test moon of the test's own. */
    private static ResourceKey<Level> copyKey() {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                "bordered_" + UUID.randomUUID().toString().substring(0, 8)));
    }
}
