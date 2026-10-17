/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.logging.LogUtils;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.BenchReport;
import java.io.IOException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Land made from nothing reaches the player's game and is drawn: the test land, with every kind of generation the
 * Core has, then the game's own overworld ground, each at a place nobody has been, the clock running from the player's
 * arrival until every chunk round them is in their game, and on until every section of it is built for drawing. The
 * test land must come no slower than the game's own ground. The numbers go to {@code local/bench/}.
 */
public final class LandDrawingClientTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceKey<Level> TEST = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "drawn_test_land"));
    private static final ResourceKey<Level> VANILLA = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "drawn_vanilla_land"));
    /** Where the player is put, in blocks: far from where any test has been. */
    private static final int FAR = 20_000;
    private static final int HIGH = 140;
    private static final int WAIT = 1200;
    /** How much slower than the game's own ground the test land may come, for the noise of one run. */
    private static final double SLACK = 1.5;
    private static final double NANOS_A_MILLI = 1_000_000.0;

    private LandDrawingClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void land_reachesThePlayerAndIsDrawnNoSlowerThanTheGamesOwn(final ClientTestContext ctx) {
        final long[] test = new long[3];
        final long[] vanilla = new long[3];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            final BlockPos home = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), TEST);
            RuntimeDimensions.remove(ctx.server(), VANILLA);
        }));
        ctx.thenServer(0, level -> go(ctx, RuntimeDimensions.getOrCreate(level.getServer(), TEST,
                TestWorldGen.TEST_LAND.id()), test))
                .thenWaitUntil(() -> timed(ctx, TEST, test), WAIT, "the test land to reach the player and be drawn")
                .thenServer(1, level -> go(ctx, RuntimeDimensions.getOrCreate(level.getServer(), VANILLA,
                        TestWorldGen.VANILLA_LAND.id()), vanilla))
                .thenWaitUntil(() -> timed(ctx, VANILLA, vanilla), WAIT,
                        "the game's own ground to reach the player and be drawn")
                .then(1, () -> {
                    final BenchReport report = new BenchReport("land_drawing");
                    report.put("test_land_arrived_ms", (test[1] - test[0]) / NANOS_A_MILLI)
                            .put("test_land_drawn_ms", (test[2] - test[0]) / NANOS_A_MILLI)
                            .put("vanilla_arrived_ms", (vanilla[1] - vanilla[0]) / NANOS_A_MILLI)
                            .put("vanilla_drawn_ms", (vanilla[2] - vanilla[0]) / NANOS_A_MILLI)
                            .put("view_distance", ctx.mc().options.getEffectiveRenderDistance());
                    LOGGER.info("[JSC-BENCH] land_drawing {}", report.summary());
                    try {
                        report.write();
                    } catch (final IOException failed) {
                        LOGGER.warn("[JSC-BENCH] the land drawing numbers could not be written", failed);
                    }
                    ctx.assertTrue(test[2] - test[0] <= (vanilla[2] - vanilla[0]) * SLACK, "the test land came in "
                            + report.get("test_land_drawn_ms") + " ms, the game's own ground in "
                            + report.get("vanilla_drawn_ms") + " ms");
                });
    }

    /* Puts the player high over a place of {@code land} nobody has been, starting its clock. */
    private static void go(final ClientTestContext ctx, final ServerLevel land, final long[] clock) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(land, FAR + 0.5, HIGH, FAR + 0.5, 0F, 30F);
        clock[0] = System.nanoTime();
    }

    /*
     * Stops the clock's laps as the land arrives and as it is drawn: every chunk within the view distance (its round
     * view, less the ring at its edge sent last) in the player's game, then nothing left to build; whether both are.
     */
    private static boolean timed(final ClientTestContext ctx, final ResourceKey<Level> dimension, final long[] clock) {
        final ClientLevel level = ctx.mc().level;
        if (level == null || ctx.mc().player == null || !level.dimension().equals(dimension)) {
            return false;
        }
        final int reach = Math.max(1, ctx.mc().options.getEffectiveRenderDistance() - 1);
        final ChunkPos at = ctx.mc().player.chunkPosition();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz <= reach * reach && !level.getChunkSource().hasChunk(at.x + dx, at.z + dz)) {
                    return false;
                }
            }
        }
        if (clock[1] == 0) {
            clock[1] = System.nanoTime();
        }
        if (!ctx.mc().levelRenderer.hasRenderedAllSections()) {
            return false;
        }
        clock[2] = System.nanoTime();
        return true;
    }
}
