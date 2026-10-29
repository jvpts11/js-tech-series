/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The Moon's ground on a player's game, as its sphere shows it from orbit: looked down on from above, the middle of
 * Mare Imbrium is the maria's dark regolith and the far side's highlands their bright one, as the map that lays both
 * draws them dark and bright.
 */
public final class MoonClientTests {

    /* Places of the Moon's map, by longitude and latitude: the middle of Mare Imbrium, far-side highlands. */
    private static final double[] IMBRIUM = {-15.6, 32.8};
    private static final double[] FAR_HIGHLANDS = {120, -20};
    /* How high over the ground the player hangs looking down, and how long the land takes to be drawn. */
    private static final int ABOVE = 18;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    /* The patch of the screen's middle read, in scaled pixels either way, and how much brighter the highlands are. */
    private static final int PATCH = 30;
    private static final int BRIGHTER = 60;
    private static final int CHANNEL = 0xFF;
    private static final int SEEING = 20 * 60 * 5;

    private MoonClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void ground_isTheMariasDarkRegolithAndTheHighlandsBrightOne(final ClientTestContext ctx) {
        final int[] mare = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.removeEffect(MobEffects.NIGHT_VISION);
            final BlockPos origin = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F,
                    0F);
        }));
        ctx.thenServer(0, level -> lookDown(ctx, IMBRIUM))
                .thenWaitUntil(() -> ctx.mc().level != null
                        && ctx.mc().level.dimension().equals(MoonWorld.world().dimension()), ARRIVE,
                        "the player's game to reach the Moon")
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> mare[0] = bright(ctx))
                .thenScreenshot(0, "mare_imbrium_from_above")
                .thenServer(0, level -> lookDown(ctx, FAR_HIGHLANDS))
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    final int highlands = bright(ctx);
                    ctx.assertTrue(highlands - mare[0] > BRIGHTER, "the highlands' regolith is brighter than the"
                            + " mare's, as the map draws them: " + highlands + " over " + mare[0]);
                })
                .thenScreenshot(0, "far_side_highlands_from_above");
    }

    /* Puts the player over the Moon's place {@code at}, by longitude and latitude, seeing in the dark, looking down. */
    private static void lookDown(final ClientTestContext ctx, final double[] at) {
        final ServerLevel moon = moon(ctx);
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(moon.dimension()).orElseThrow();
        final double perDegree = (bounds.maxX() - bounds.minX()) / 360;
        final int x = (int) Math.round(at[0] * perDegree);
        final int z = (int) Math.round(-at[1] * perDegree);
        moon.getChunk(new BlockPos(x, 0, z));
        final int ground = moon.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SEEING, 0, false, false));
        player.teleportTo(moon, x + 0.5, ground + ABOVE, z + 0.5, 0F, 90F);
    }

    /*
     * The Moon, its dimension's own when the world has it, else made from its dimension's file under its own name, as
     * a world makes it.
     */
    private static ServerLevel moon(final ClientTestContext ctx) {
        final ResourceKey<Level> key = MoonWorld.world().dimension();
        return RuntimeDimensions.getOrCreate(ctx.server(), key, key.location());
    }

    /* How bright the middle of the screen is, the three channels summed, on the mean. */
    private static int bright(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int sum = 0;
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH; dx <= PATCH; dx++) {
                for (int dy = -PATCH; dy <= PATCH; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    sum += (argb >> 16 & CHANNEL) + (argb >> 8 & CHANNEL) + (argb & CHANNEL);
                    count++;
                }
            }
        }
        return sum / count;
    }
}
