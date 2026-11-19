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
    /*
     * Noon on the clock, a day's ticks and a whole turn in degrees: the Moon's hour follows the longitude, and seeing
     * in the dark does not take the night's blue out of a place's light, so each place is looked at in its own noon.
     */
    private static final long NOON = 6_000L;
    private static final long DAY = 24_000L;
    private static final double FULL_TURN = 360.0;
    /* The Moon's clock not read yet. */
    private static final long NO_TIME = Long.MIN_VALUE;
    /*
     * Stood on the ground: how far down the player looks, in degrees; how bright a row of the black sky is at most, with
     * a star in it; how many rows of the farthest land are read; where down the screen the near land is read, as a
     * share of its height; and how bright the far land is at least, as a share of the near.
     */
    private static final float LOOKING_DOWN = 8F;
    private static final int SKY_MOST = 60;
    private static final int LAND_ROWS = 4;
    private static final double NEAR_ROW = 0.85;
    private static final int FAR_SHARE = 6;
    private static final int FAR_SHARE_OF = 10;
    /*
     * Coming down: the heights looked from, three under the build limit and one just over it; how far down the player
     * looks, in degrees; how long the far map takes to come and be drawn, in ticks; and how bright lit land is at
     * least.
     */
    private static final int[] ARRIVAL_HEIGHTS = {150, 250, 310, 330};
    private static final float ARRIVAL_DOWN = 30F;
    private static final int FAR_MAP = 120;
    private static final int LIT = 120;
    /* A row of the screen just under where the horizon stands looking that far down, as a share of its height. */
    private static final double UNDER_THE_HORIZON = 0.3;

    private MoonClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void ground_isTheMariasDarkRegolithAndTheHighlandsBrightOne(final ClientTestContext ctx) {
        final int[] mare = new int[1];
        final long[] time = {NO_TIME};
        ctx.afterTest(() -> home(ctx, time));
        ctx.thenServer(0, level -> {
            time[0] = moon(ctx).getDayTime();
            lookDown(ctx, IMBRIUM);
        })
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

    /*
     * The land under the Moon's black sky, stood on the mare at its noon looking out over it: with no air to fade it,
     * the land at the end of sight is lit as the land at the player's feet is, not faded into the dark of a fog.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void farLand_isLitAsTheNearWithNoAirToFadeIt(final ClientTestContext ctx) {
        final int[] seen = new int[2];
        final long[] time = {NO_TIME};
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            home(ctx, time);
        });
        ctx.thenServer(0, level -> {
            time[0] = moon(ctx).getDayTime();
            standOn(ctx, IMBRIUM);
        })
                .thenWaitUntil(() -> ctx.mc().level != null
                        && ctx.mc().level.dimension().equals(MoonWorld.world().dimension()), ARRIVE,
                        "the player's game to reach the Moon")
                .thenAwaitLandDrawn(ARRIVE)
                .then(0, () -> ctx.mc().options.hideGui = true)
                .then(SETTLE, () -> {
                    try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
                        seen[0] = farLand(ctx, frame);
                        seen[1] = rowBright(ctx, frame,
                                (int) (ctx.mc().getWindow().getGuiScaledHeight() * NEAR_ROW));
                    }
                })
                .thenScreenshot(0, "moon_land_to_the_end_of_sight")
                .thenAssert(0, () -> seen[0] * FAR_SHARE_OF >= seen[1] * FAR_SHARE, "the land at the end of sight is "
                        + "lit as the near land is, no fog fading it: " + seen[0] + " against " + seen[1]);
    }

    /*
     * Puts the player over the Moon's place {@code at}, by longitude and latitude, at its noon, seeing in the dark,
     * looking down.
     */
    private static void lookDown(final ClientTestContext ctx, final double[] at) {
        final ServerLevel moon = moon(ctx);
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(moon.dimension()).orElseThrow();
        final double perDegree = (bounds.maxX() - bounds.minX()) / 360;
        final int x = (int) Math.round(at[0] * perDegree);
        final int z = (int) Math.round(-at[1] * perDegree);
        // Noon there: later the farther east.
        moon.setDayTime(Math.floorMod(NOON - Math.round(at[0] / FULL_TURN * DAY), DAY));
        moon.getChunk(new BlockPos(x, 0, z));
        final int ground = moon.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SEEING, 0, false, false));
        player.teleportTo(moon, x + 0.5, ground + ABOVE, z + 0.5, 0F, 90F);
    }

    /*
     * Coming down over the mare at its noon, looking ahead and down, at each height under the build limit and just over
     * it: the land in the middle of the view is lit, and so is the farthest land under the black sky. With no air
     * there is no fog to fade the land into the dark, and the far map goes on from the loaded land to the horizon:
     * a fog the black of the sky once turned the land black from a few hundred blocks up.
     */
    @ClientTest(timeoutTicks = 4000)
    public static void arrival_showsTheLandLitToTheHorizonFromEveryHeight(final ClientTestContext ctx) {
        final long[] time = {NO_TIME};
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            home(ctx, time);
        });
        ctx.thenServer(0, level -> {
            time[0] = moon(ctx).getDayTime();
            standOn(ctx, IMBRIUM);
        })
                .thenWaitUntil(() -> ctx.mc().level != null
                        && ctx.mc().level.dimension().equals(MoonWorld.world().dimension()), ARRIVE,
                        "the player's game to reach the Moon")
                .then(0, () -> ctx.mc().options.hideGui = true);
        for (final int height : ARRIVAL_HEIGHTS) {
            ctx.thenServer(0, level -> {
                final ServerPlayer player = ctx.serverPlayer();
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(moon(ctx), player.getX(), height, player.getZ(), 0F, ARRIVAL_DOWN);
            }).thenWaitUntil(() -> ctx.mc().player.getY() > height - 2, ARRIVE, "the player up at " + height)
                    .thenAwaitLandDrawn(ARRIVE)
                    .then(FAR_MAP, () -> {
                        ctx.assertTrue(ctx.mc().player.getY() > height - 2, "the player still hangs at " + height
                                + ", at " + ctx.mc().player.getY());
                        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
                            final int screen = ctx.mc().getWindow().getGuiScaledHeight();
                            final int middle = rowBright(ctx, frame, screen / 2);
                            final int horizon = rowBright(ctx, frame, (int) (screen * UNDER_THE_HORIZON));
                            final int far = farLand(ctx, frame);
                            ctx.assertTrue(middle >= LIT, "at " + height + " the land below is lit, not black: "
                                    + middle);
                            ctx.assertTrue(horizon >= LIT, "at " + height + " the land reaches the horizon, no black"
                                    + " between the loaded land and the sky: " + horizon);
                            ctx.assertTrue(far * FAR_SHARE_OF >= middle * FAR_SHARE, "at " + height + " the land is "
                                    + "lit to the horizon, not faded into the dark: " + far + " against " + middle);
                        }
                    })
                    .thenScreenshot(0, "arrival_" + height);
        }
    }

    /* Puts the player on the Moon's ground at its place {@code at}, at its noon, looking out level and a little down. */
    private static void standOn(final ClientTestContext ctx, final double[] at) {
        final ServerLevel moon = moon(ctx);
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(moon.dimension()).orElseThrow();
        final double perDegree = (bounds.maxX() - bounds.minX()) / 360;
        final int x = (int) Math.round(at[0] * perDegree);
        final int z = (int) Math.round(-at[1] * perDegree);
        moon.setDayTime(Math.floorMod(NOON - Math.round(at[0] / FULL_TURN * DAY), DAY));
        moon.getChunk(new BlockPos(x, 0, z));
        final int ground = moon.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        ctx.serverPlayer().teleportTo(moon, x + 0.5, ground, z + 0.5, 0F, LOOKING_DOWN);
    }

    /*
     * Back home: on the overworld at the test's place, walking, seeing as one does; the Moon's clock put back to
     * {@code time} when the test had read it, so the tests after see the Moon as they would have.
     */
    private static void home(final ClientTestContext ctx, final long[] time) {
        ctx.server().execute(() -> {
            if (time[0] != NO_TIME) {
                moon(ctx).setDayTime(time[0]);
            }
            final ServerPlayer player = ctx.serverPlayer();
            // Flying, and let fly, as its game mode has it.
            player.gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.removeEffect(MobEffects.NIGHT_VISION);
            final BlockPos origin = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F,
                    0F);
        });
    }

    /*
     * How bright the farthest land in sight is: down the screen's middle from its top, past the black sky, the first
     * rows the land fills, the three channels summed, on the mean.
     */
    private static int farLand(final ClientTestContext ctx, final ClientTestHelper.FrameSampler frame) {
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        for (int y = 0; y < height - LAND_ROWS; y++) {
            if (rowBright(ctx, frame, y) > SKY_MOST) {
                int sum = 0;
                for (int k = 1; k <= LAND_ROWS; k++) {
                    sum += rowBright(ctx, frame, y + k);
                }
                return sum / LAND_ROWS;
            }
        }
        return 0;
    }

    /* How bright the row {@code y} of {@code frame} is across the middle, the three channels summed, on the mean. */
    private static int rowBright(final ClientTestContext ctx, final ClientTestHelper.FrameSampler frame, final int y) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        int sum = 0;
        for (int dx = -PATCH; dx <= PATCH; dx++) {
            final int argb = frame.pixel(x + dx, y);
            sum += (argb >> 16 & CHANNEL) + (argb >> 8 & CHANNEL) + (argb & CHANNEL);
        }
        return sum / (2 * PATCH + 1);
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
