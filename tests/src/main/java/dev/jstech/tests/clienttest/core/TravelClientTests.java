/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.dimension.DimensionTravel;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.TestPassageScreen;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Travelling between dimensions on a player's game: the passage is shown through the screen the mod named, standing in
 * for the game's "loading terrain", and the player arrives on firm ground once the land is there to be seen.
 */
public final class TravelClientTests {

    private static final int WAIT = 400;

    private TravelClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void passage_isShownThroughTheModsScreenAndLandsOnFirmGround(final ClientTestContext ctx) {
        final ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(
                JsTests.MODID, "travel_" + UUID.randomUUID().toString().substring(0, 8)));
        final int shown = TestPassageScreen.SHOWN.get();
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerLevel home = ctx.server().overworld();
            if (ctx.serverPlayer().level() != home) {
                DimensionTravel.travel(ctx.serverPlayer(), home, ctx.abs(new BlockPos(0, 2, 0)), null);
            }
            RuntimeDimensions.remove(ctx.server(), key);
        }));
        ctx.then(0, () -> ctx.server().execute(() -> {
                    final ServerLevel moon = RuntimeDimensions.getOrCreate(ctx.server(), key,
                            TestWorldGen.MOON.id());
                    DimensionTravel.travel(ctx.serverPlayer(), moon, new BlockPos(0, 20, 0), TestPassageScreen.ID);
                }))
                .thenWaitUntil(() -> TestPassageScreen.SHOWN.get() > shown, WAIT,
                        "the test mod's screen to stand in for the game's loading terrain")
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(key)
                        && ctx.mc().screen == null, WAIT, "the player to arrive and the land to be seen")
                /*
                 * Where the passage put the player is the server's to say: below where they were sent, on a block
                 * that is not air. The client's own sense of the ground waits on its chunks, which a busy machine
                 * can keep from it for a long while.
                 */
                .thenWaitUntilServer(level -> landed(ctx, key), WAIT,
                        "the player standing on the Moon's ground below where they were sent",
                        level -> "at " + ctx.serverPlayer().position() + " in " + ctx.serverPlayer().level()
                                .dimension().location())
                .thenScreenshot(20, "arrived");
    }

    /* Whether the player stands in the new dimension below where they were sent, on something solid. */
    private static boolean landed(final ClientTestContext ctx, final ResourceKey<Level> key) {
        final ServerPlayer player = ctx.serverPlayer();
        return player.level().dimension().equals(key) && player.getY() < 20
                && !player.level().getBlockState(player.blockPosition().below()).isAir();
    }
}
