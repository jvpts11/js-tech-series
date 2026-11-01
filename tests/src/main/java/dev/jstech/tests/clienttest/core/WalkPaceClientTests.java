/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.DimensionTravel;
import dev.jstech.core.dimension.IWalkPace;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * A walk on a player's game swings its arms and legs at the pace the ground's pull sets: the same walk on the
 * Overworld and on a moon of a sixth of its pull, the moon's limbs swinging at the square root of that, four tenths of
 * the pace, as wide as on the Overworld.
 */
public final class WalkPaceClientTests {

    /* Where the walk starts in the test's place, and on the moon, high over its flat ground to land on it. */
    private static final BlockPos START = new BlockPos(3, 2, 8);
    private static final BlockPos ON_THE_MOON = new BlockPos(0, 20, 0);
    private static final int ARRIVE = 600;
    /* How long the walk is under way before it is read, and for how long it is read, in ticks. */
    private static final int UNDER_WAY = 10;
    private static final int READ_FOR = 20;
    /* How far from the pace its pull sets the moon's walk may be, and how much less wide it may swing. */
    private static final double NEAR = 0.05;
    private static final double AS_WIDE = 0.8;

    private WalkPaceClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void walk_swingsSlowerWhereTheGroundPullsLessAsWide(final ClientTestContext ctx) {
        final ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(
                JsTests.MODID, "walk_" + UUID.randomUUID().toString().substring(0, 8)));
        final float[] home = new float[2];
        final float[] moon = new float[2];
        final float[] started = new float[1];
        ctx.afterTest(() -> {
            ctx.mc().options.keyUp.setDown(false);
            ctx.server().execute(() -> {
                final ServerLevel overworld = ctx.server().overworld();
                if (ctx.serverPlayer().level() != overworld) {
                    DimensionTravel.travel(ctx.serverPlayer(), overworld, ctx.abs(START), null);
                }
                RuntimeDimensions.remove(ctx.server(), key);
            });
        });
        ctx.thenTeleport(0, START, Direction.NORTH);
        walk(ctx, home, started);
        ctx.thenServer(0, level -> {
            final ServerLevel there = RuntimeDimensions.getOrCreate(ctx.server(), key, TestWorldGen.MOON.id());
            ctx.serverPlayer().setYRot(Direction.NORTH.toYRot());
            DimensionTravel.travel(ctx.serverPlayer(), there, ON_THE_MOON, null);
        })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(key)
                        && ctx.mc().screen == null && ctx.mc().player.onGround(), ARRIVE,
                        "the player to stand on the moon");
        walk(ctx, moon, started);
        ctx.then(0, () -> {
            final double pace = IWalkPace.of(DimensionRulesData.of(ctx.mc().level).gravityAt(ctx.mc().player.getY()));
            final double ratio = moon[0] / home[0];
            ctx.assertTrue(Math.abs(ratio - pace) < NEAR, String.format(Locale.ROOT, "on the moon the limbs swing at"
                    + " %.3f of the Overworld's pace, the square root of its pull being %.3f: %.3f a tick against"
                    + " %.3f", ratio, pace, moon[0], home[0]));
            ctx.assertTrue(moon[1] > home[1] * AS_WIDE, "and as wide: " + moon[1] + " against " + home[1]);
        });
    }

    /*
     * Walks north and reads the walk once under way: how far its swing goes on a tick, into {@code into}[0], and how
     * wide it swings, into {@code into}[1].
     */
    private static void walk(final ClientTestContext ctx, final float[] into, final float[] started) {
        ctx.then(0, () -> ctx.mc().options.keyUp.setDown(true))
                .then(UNDER_WAY, () -> started[0] = ctx.mc().player.walkAnimation.position())
                .then(READ_FOR, () -> {
                    into[0] = (ctx.mc().player.walkAnimation.position() - started[0]) / READ_FOR;
                    into[1] = ctx.mc().player.walkAnimation.speed();
                    ctx.mc().options.keyUp.setDown(false);
                });
    }
}
