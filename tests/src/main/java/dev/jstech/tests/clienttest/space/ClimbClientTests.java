/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.dimension.TravelScreens;
import dev.jstech.core.client.sky.GroundCovers;
import dev.jstech.space.universe.ClimbView;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestHelper;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * The way up from the Overworld to its space on a player's game, read from the picture: low in the climb the sky is
 * the day's, half way up it is navy; just under the line the picture is the very one the space shows once the line is
 * crossed, the player's hand as it was; and the passage up, and the one back down, open no loading screen at all.
 * Low in the climb the loaded ground is drawn however far below it lies, and from halfway up its own picture stands in
 * for it.
 */
public final class ClimbClientTests {

    private static final ResourceKey<Level> SPACE = SystemSpaces.keyOf(StarSystems.SOLAR_SYSTEM);
    private static final int ARRIVE = 600;
    private static final int DRAWN = 600;
    private static final int SETTLE = 20;
    private static final int FAR_MAP = 120;
    private static final long NOON = 6_000L;
    private static final float NORTH = 180F;
    /* Looking well down at the world below, and nearly level at the horizon. */
    private static final float DOWN = 60F;
    private static final float LEVEL = 10F;
    /* How far from the line the climber stands under it, and goes over it. */
    private static final int UNDER = 2;
    private static final int OVER = 5;
    /* Half way up, and low in the climb, in blocks over the build limit. */
    private static final int HALF = SpaceClimbs.ABOVE_THE_LIMIT / 2;
    private static final int LOW = 80;
    /* Past where the loaded blocks give way to the far map's picture of them. */
    private static final int HIGH = (int) (SpaceClimbs.ABOVE_THE_LIMIT * (ClimbView.BLOCKS_GIVE_WAY + 0.1));
    /* A swing all but whole, as a player's who has not struck in a while. */
    private static final float WHOLE_SWING = 0.99F;
    /* Where the picture is read below the horizon, in the GUI's points from its middle, clear of the hand. */
    private static final int[][] GROUND = {{0, 40}, {-60, 60}, {-30, 90}, {-90, 30}};
    /* Where the sky is read, high over the middle. */
    private static final int[] SKY = {0, -110};
    /* How far a point's red, green and blue together may differ across the line. */
    private static final int SAME = 60;
    private static final int SEEN = 60;
    private static final int NAVY = 250;
    private static final int CHANNEL = 0xFF;

    private ClimbClientTests() {
    }

    @ClientTest(timeoutTicks = 4800)
    public static void climb_crossesIntoSpaceAndBackWithNoLoadingScreenAndTheSamePicture(
            final ClientTestContext ctx) {
        final boolean[] daylight = new boolean[1];
        final boolean[] screen = new boolean[1];
        final int[] passages = new int[1];
        final int[][] under = new int[GROUND.length][];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight[0], ctx.server());
            land(ctx);
        }));
        ctx.thenServer(0, level -> {
            final GameRules.BooleanValue rule = level.getServer().getGameRules().getRule(GameRules.RULE_DAYLIGHT);
            daylight[0] = rule.get();
            rule.set(false, level.getServer());
            level.getServer().overworld().setDayTime(NOON);
            look(ctx, level.getServer().overworld(), level.getMaxBuildHeight() + LOW, LEVEL);
        })
                .thenWaitUntil(() -> ctx.mc().player != null && ctx.mc().player.getY() > 300, ARRIVE,
                        "the player's game to follow the climber up")
                // The far map is drawn on the server away from its thread and sent: give it a few seconds.
                .thenScreenshot(FAR_MAP, "climb_low")
                .thenServer(0, level -> look(ctx, level.getServer().overworld(),
                        level.getMaxBuildHeight() + HALF, LEVEL))
                .then(SETTLE, () -> {
                    final int[] sky = colour(ctx, SKY[0], SKY[1]);
                    ctx.assertTrue(sky[0] + sky[1] + sky[2] < NAVY && sky[2] > sky[0],
                            "half way up the sky is navy: " + sky[0] + "," + sky[1] + "," + sky[2]);
                })
                .thenScreenshot(0, "climb_half")
                .thenServer(0, level -> look(ctx, level.getServer().overworld(),
                        SpaceClimbs.ceiling(level.getServer().overworld()) - UNDER, DOWN))
                .thenWaitUntil(() -> bright(colour(ctx, GROUND[0][0], GROUND[0][1])) > SEEN, DRAWN,
                        "the world's sphere to be drawn below, just under the line")
                .then(SETTLE, () -> {
                    for (int i = 0; i < GROUND.length; i++) {
                        under[i] = colour(ctx, GROUND[i][0], GROUND[i][1]);
                    }
                    passages[0] = TravelScreens.seamlessPassages();
                })
                .thenScreenshot(0, "climb_under_the_line")
                .thenServer(0, level -> look(ctx, level.getServer().overworld(),
                        SpaceClimbs.ceiling(level.getServer().overworld()) + OVER, DOWN))
                .thenWaitUntil(() -> arrived(ctx, SPACE, screen), ARRIVE, "the climber to cross into space")
                .then(SETTLE, () -> {
                    ctx.assertTrue(!screen[0] && TravelScreens.seamlessPassages() > passages[0],
                            "the passage up opened no loading screen: a screen seen " + screen[0] + ", seamless "
                                    + "passages " + passages[0] + " to " + TravelScreens.seamlessPassages()
                                    + ", still expected " + TravelScreens.expected());
                    // The player made anew for the space kept the old one's swing: the hand did not drop.
                    ctx.assertTrue(ctx.mc().player.getAttackStrengthScale(0F) > WHOLE_SWING,
                            "the hand as it was across the line: swing " + ctx.mc().player.getAttackStrengthScale(0F));
                    for (int i = 0; i < GROUND.length; i++) {
                        final int[] over = colour(ctx, GROUND[i][0], GROUND[i][1]);
                        final int differs = Math.abs(over[0] - under[i][0]) + Math.abs(over[1] - under[i][1])
                                + Math.abs(over[2] - under[i][2]);
                        ctx.assertTrue(differs < SAME, "the picture is the same across the line at point " + i
                                + ": " + differs + " apart");
                    }
                    passages[0] = TravelScreens.seamlessPassages();
                })
                .thenScreenshot(0, "climb_over_the_line")
                .thenServer(0, level -> {
                    final ServerPlayer player = ctx.serverPlayer();
                    player.teleportTo(player.serverLevel(), player.getX(), SpaceClimbs.floor() - OVER, player.getZ(),
                            NORTH, DOWN);
                })
                .thenWaitUntil(() -> arrived(ctx, Level.OVERWORLD, screen), ARRIVE,
                        "the climber to come back down into the Overworld")
                .then(SETTLE, () -> ctx.assertTrue(!screen[0] && TravelScreens.seamlessPassages() > passages[0],
                        "the passage down opened no loading screen"))
                .thenScreenshot(0, "descent_under_the_line");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void climb_drawsTheLoadedGroundFarBelowThenLeavesItToItsPicture(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> land(ctx)));
        ctx.thenServer(0, level -> look(ctx, level.getServer().overworld(), level.getMaxBuildHeight() + LOW, DOWN))
                // The flat land lies far further below than the game's sight reaches up and down.
                .thenWaitUntil(() -> ctx.mc().player != null && ctx.mc().player.getY() > 300
                        && ctx.mc().levelRenderer.countRenderedSections() > 0, DRAWN,
                        "the loaded ground far below to be drawn")
                .then(SETTLE, () -> ctx.assertTrue(!GroundCovers.covered(),
                        "low in the climb the game draws its own blocks"))
                .thenScreenshot(0, "climb_ground_far_below")
                .thenServer(0, level -> look(ctx, level.getServer().overworld(),
                        level.getMaxBuildHeight() + HIGH, DOWN))
                .thenWaitUntil(GroundCovers::covered, DRAWN, "the loaded blocks to give way to their picture")
                // The far map is drawn on the server away from its thread and sent, with the height of the sea.
                .thenWaitUntil(() -> {
                    final int[] seen = colour(ctx, GROUND[0][0], GROUND[0][1]);
                    return bright(seen) > SEEN && seen[1] > seen[2];
                }, DRAWN, "the ground's picture to be drawn below")
                .then(SETTLE, () -> {
                    // The flat land lies over its own sea, far under the game's guess at a sea: grass, not water.
                    for (final int[] point : GROUND) {
                        final int[] seen = colour(ctx, point[0], point[1]);
                        ctx.assertTrue(seen[1] > seen[2], "the flat land's picture is its grass at " + point[0] + ","
                                + point[1] + ": " + seen[0] + "," + seen[1] + "," + seen[2]);
                    }
                })
                .thenScreenshot(0, "climb_blocks_given_way");
    }

    /* Puts the player back on the ground of the test's place, no longer flying, as the next test expects them. */
    private static void land(final ClientTestContext ctx) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        final BlockPos origin = ctx.origin().above(2);
        player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F, 0F);
    }

    /* Puts the player flying over the test's place, {@code height} high, looking north {@code pitch} down. */
    private static void look(final ClientTestContext ctx, final ServerLevel level, final double height,
                             final float pitch) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        final BlockPos origin = ctx.origin();
        player.teleportTo(level, origin.getX() + 0.5, height, origin.getZ() + 0.5, NORTH, pitch);
    }

    /* Whether the player's game is in {@code dimension}; any loading screen seen on the way is noted. */
    private static boolean arrived(final ClientTestContext ctx, final ResourceKey<Level> dimension,
                                   final boolean[] screen) {
        if (ctx.mc().screen instanceof ReceivingLevelScreen) {
            screen[0] = true;
        }
        return ctx.mc().level != null && ctx.mc().level.dimension().equals(dimension);
    }

    private static int bright(final int[] colour) {
        return colour[0] + colour[1] + colour[2];
    }

    /* The mean red, green and blue of three by three of the GUI's points, {@code dx} and {@code dy} off the middle. */
    private static int[] colour(final ClientTestContext ctx, final int dx, final int dy) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 + dx;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + dy;
        final int[] sum = new int[3];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    final int argb = frame.pixel(x + ox, y + oy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                }
            }
        }
        return new int[] {sum[0] / 9, sum[1] / 9, sum[2] / 9};
    }
}
