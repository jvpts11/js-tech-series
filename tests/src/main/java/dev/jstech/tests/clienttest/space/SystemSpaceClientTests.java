/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * The Solar System's space on a player's game, read from the picture: over the Overworld's day side the ground below is
 * lit, over its night side it is the deep blue of its night and not the black of space, and straight over its middle
 * the line of dawn runs below, day towards the Sun and night away from it; the Sun is the game's bright square where it
 * stands; the Moon is lit on its side towards the Sun and dark away from it; a block in space stands in full
 * daylight; and over Mars the ground is red, while from the Overworld's orbit Mars is a bright point.
 */
public final class SystemSpaceClientTests {

    private static final ResourceKey<Level> SPACE = SystemSpaces.keyOf(StarSystems.SOLAR_SYSTEM);
    private static final int ARRIVE = 600;
    /* The Overworld's maps are drawn from the seed away from the game's thread, which takes a few seconds. */
    private static final int DRAWN = 600;
    private static final int SETTLE = 10;
    /* How far west or east of a world's middle the player looks down from, in blocks: 3/5 of the Overworld's radius. */
    private static final double ASIDE = 0.6 * SystemLayout.OVERWORLD_RADIUS;
    /* Where the screen is read, in the GUI's points from its middle: clear of the crosshair, and to either side. */
    private static final int OFF_CROSSHAIR = 12;
    private static final int WIDE = 100;
    /*
     * The Moon is about eight points round from the Overworld's orbit and seen gibbous, its dark part a crescent on the
     * side away from the Sun: read half way out on the lit side and near the edge on the dark, just under the
     * crosshair.
     */
    private static final int MOON_LIT = 4;
    private static final int MOON_DARK = -7;
    private static final int MOON_DOWN = 3;
    /* The sum of red, green and blue over which a pixel is lit, under which it is night. */
    private static final int LIT = 120;
    private static final int DARK = 45;
    /*
     * Near the line of dawn the day is dimmed, and what lies there turns with the world, a dark forest as well as
     * bright plains: the day side is told from the night by being this many times brighter than it.
     */
    private static final int DAY_OVER_NIGHT = 3;
    private static final int SUN = 400;
    /* White concrete's top in full daylight, and how far under the player it is put. */
    private static final int DAYLIT = 450;
    private static final double BELOW = 3;
    private static final float SOUTH = 0F;
    private static final float WEST = 90F;
    private static final float NORTH = 180F;
    private static final float DOWN = 90F;
    private static final int CHANNEL = 0xFF;
    /*
     * The world below slid by in orbit: a patch of the screen under the crosshair read twice, fifteen seconds apart;
     * it slides by a turn a day, some twelve points of the picture every five seconds, so the patch's picture moves
     * this many points or more, across and down at once since the world turns about its leaning poles, sought this
     * far each way.
     */
    private static final int SLIDING = 300;
    private static final int PATCH = 30;
    private static final int PATCH_DOWN = 24;
    private static final int MOST_SHIFT = 60;
    private static final int SLID = 20;
    /*
     * Mars looked at from the Overworld's orbit: how far beside the crosshair it is put, in degrees, and how far round
     * the crosshair it is sought, in the GUI's points; how red a pixel of it is, and the least and the most of them a
     * point covers, more than a star's and where its disc would cover hundreds; and how high over the player's feet
     * their eye is.
     */
    private static final float BESIDE = 2.5F;
    private static final int AHEAD = 15;
    private static final int RED = 200;
    private static final int REDDER = 60;
    private static final int LEAST_POINT_PIXELS = 4;
    private static final int POINT_PIXELS = 60;
    private static final double EYE = 1.62;

    private SystemSpaceClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void systemSpace_drawsTheWorldsAsLitSpheresAndTheSunWhereTheyStand(final ClientTestContext ctx) {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] mars = SystemLayout.centre(SolarBody.MARS);
        final double[] marsSunward = SystemLayout.sunward(SolarBody.MARS);
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final BlockPos origin = ctx.origin().above(2);
            ctx.serverPlayer().teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(),
                    origin.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), SPACE);
        }));
        ctx.thenServer(0, level -> look(ctx, SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM),
                        home[0] - ASIDE, home[2], NORTH, DOWN))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(SPACE), ARRIVE,
                        "the player's game to reach the Solar System's space")
                .thenWaitUntil(() -> bright(ctx, 0, OFF_CROSSHAIR) > LIT, DRAWN,
                        "the Overworld's day side to be drawn lit below")
                .thenScreenshot(SETTLE, "overworld_day")
                .thenServer(0, level -> look(ctx, space(ctx), home[0] + ASIDE, home[2], NORTH, DOWN))
                .then(SETTLE, () -> {
                    final int[] night = colour(ctx, 0, OFF_CROSSHAIR);
                    ctx.assertTrue(night[0] + night[1] + night[2] < DARK && night[2] > night[0],
                            "over the night side the ground is the Overworld's deep blue night, not black: "
                                    + rgb(night));
                })
                .thenScreenshot(0, "overworld_night")
                .thenServer(0, level -> look(ctx, space(ctx), home[0], home[2], NORTH, DOWN))
                .then(SETTLE, () -> {
                    // Looking down with the north up the screen, the Sun's side, the west, is on the left.
                    final int west = bright(ctx, -WIDE, OFF_CROSSHAIR);
                    final int east = bright(ctx, WIDE, OFF_CROSSHAIR);
                    ctx.assertTrue(east < DARK && west > DARK && west > DAY_OVER_NIGHT * east,
                            "the line of dawn runs below, day west and night east: " + west + " against " + east);
                })
                .thenScreenshot(0, "overworld_dawn")
                .thenServer(0, level -> {
                    final double[] sun = SystemLayout.sun();
                    final double x = home[0] - ASIDE;
                    final float pitch = (float) Math.toDegrees(Math.atan2(SystemLayout.ORBIT_Y - sun[1], x - sun[0]));
                    look(ctx, space(ctx), x, home[2], WEST, pitch);
                })
                .then(SETTLE, () -> {
                    final int[] sun = colour(ctx, OFF_CROSSHAIR, 0);
                    ctx.assertTrue(sun[0] + sun[1] > SUN, "the Sun is the game's bright square: " + rgb(sun));
                })
                .thenScreenshot(0, "sun")
                .thenServer(0, level -> {
                    final double[] moon = SystemLayout.centre(SolarBody.MOON);
                    final float pitch = (float) Math.toDegrees(Math.atan2(SystemLayout.ORBIT_Y - moon[1],
                            moon[2] - home[2]));
                    look(ctx, space(ctx), home[0], home[2], SOUTH, pitch);
                })
                .then(SETTLE, () -> {
                    // Looking south, the west and the Sun are on the right.
                    final int lit = bright(ctx, MOON_LIT, MOON_DOWN);
                    final int dark = bright(ctx, MOON_DARK, MOON_DOWN);
                    ctx.assertTrue(lit > LIT && dark < DARK, "the Moon is lit on its side towards the Sun and dark"
                            + " away from it: " + lit + " against " + dark);
                })
                .thenScreenshot(0, "moon_quarter")
                .thenServer(0, level -> {
                    // A block in space stands in full daylight: the Sun the sky does not draw still lights it.
                    final BlockPos block = BlockPos.containing(home[0] - ASIDE, SystemLayout.ORBIT_Y - BELOW, home[2]);
                    space(ctx).setBlockAndUpdate(block, Blocks.WHITE_CONCRETE.defaultBlockState());
                    // Right over the block's middle, so all that is read below the crosshair is its top.
                    look(ctx, space(ctx), block.getX() + 0.5, block.getZ() + 0.5, NORTH, DOWN);
                })
                .then(SETTLE * 2, () -> ctx.assertTrue(bright(ctx, 0, OFF_CROSSHAIR) > DAYLIT,
                        "a block in space is lit by the Sun: " + bright(ctx, 0, OFF_CROSSHAIR)))
                .thenServer(0, level -> look(ctx, space(ctx), mars[0] + marsSunward[0] * ASIDE / 2,
                        mars[2] + marsSunward[2] * ASIDE / 2, NORTH, DOWN))
                .then(SETTLE * 4, () -> {
                    final int[] ground = colour(ctx, 0, OFF_CROSSHAIR);
                    ctx.assertTrue(ground[0] > ground[2] + 20 && ground[0] + ground[1] + ground[2] > DARK,
                            "over Mars' day side the ground is red: " + rgb(ground));
                })
                .thenScreenshot(0, "mars_day");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void orbit_slidesTheWorldByBelow(final ClientTestContext ctx) {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final int[][][] patches = new int[2][][];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final BlockPos origin = ctx.origin().above(2);
            ctx.serverPlayer().teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(),
                    origin.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), SPACE);
        }));
        ctx.thenServer(0, level -> look(ctx, SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM),
                        home[0] - ASIDE, home[2], NORTH, DOWN))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(SPACE), ARRIVE,
                        "the player's game to reach the Solar System's space")
                .thenWaitUntil(() -> bright(ctx, 0, OFF_CROSSHAIR) > LIT, DRAWN,
                        "the Overworld's day side to be drawn lit below")
                .then(SETTLE, () -> patches[0] = patch(ctx, 0))
                .thenScreenshot(0, "orbit_before")
                .then(SLIDING, () -> patches[1] = patch(ctx, MOST_SHIFT))
                .thenScreenshot(0, "orbit_after")
                .then(0, () -> {
                    final long moved = Math.round(shift(patches[0], patches[1]));
                    ctx.assertTrue(moved >= SLID, "in orbit the world slides by below, a turn a day: moved "
                            + moved + " points in " + SLIDING + " ticks");
                });
    }

    /*
     * From the Overworld's low orbit Mars lies outside its region: looked at, it is a bright point of its orange red,
     * a few pixels, larger than a star and no disc. Read at the frame's own pixels round the crosshair, clear of the
     * Sun, with Mars a little beside the crosshair, which would hide a point under it.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void mars_fromTheOverworldsOrbitIsABrightPoint(final ClientTestContext ctx) {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] mars = SystemLayout.centre(SolarBody.MARS);
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final BlockPos origin = ctx.origin().above(2);
            ctx.serverPlayer().teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(),
                    origin.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), SPACE);
        }));
        final double dx = mars[0] - home[0];
        final double dz = mars[2] - home[2];
        final double dy = mars[1] - (SystemLayout.ORBIT_Y + EYE);
        final float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)) + BESIDE;
        final float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        ctx.thenServer(0, level -> look(ctx, SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM),
                        home[0], home[2], yaw, pitch))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.dimension().equals(SPACE), ARRIVE,
                        "the player's game to reach the Solar System's space")
                .thenWaitUntil(() -> red(ctx) > 0, DRAWN, "Mars to be drawn ahead")
                .then(SETTLE, () -> {
                    final int seen = red(ctx);
                    ctx.assertTrue(seen >= LEAST_POINT_PIXELS && seen <= POINT_PIXELS, "from the Overworld's orbit "
                            + "Mars is a bright point, larger than a star and no disc: " + seen + " of its orange red "
                            + "pixels by the crosshair");
                })
                .thenScreenshot(0, "mars_a_point_from_the_overworld");
    }

    /* Puts the player flying in the space over ({@code x}, {@code z}), at the height of the low orbits. */
    private static void look(final ClientTestContext ctx, final ServerLevel space, final double x, final double z,
                             final float yaw, final float pitch) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(space, x, SystemLayout.ORBIT_Y, z, yaw, pitch);
    }

    private static ServerLevel space(final ClientTestContext ctx) {
        final ServerLevel space = ctx.server().getLevel(SPACE);
        if (space == null) {
            throw new IllegalStateException("the Solar System's space is gone");
        }
        return space;
    }

    /* How many of the frame's pixels round the crosshair are a bright orange red, as Mars is. */
    private static int red(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 - AHEAD;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 - AHEAD;
        int count = 0;
        try (NativeImage frame = ctx.capture(x, y, 2 * AHEAD, 2 * AHEAD)) {
            for (int py = 0; py < frame.getHeight(); py++) {
                for (int px = 0; px < frame.getWidth(); px++) {
                    final int abgr = frame.getPixelRGBA(px, py);
                    final int r = abgr & CHANNEL;
                    final int b = abgr >> 16 & CHANNEL;
                    count += r > RED && r - b > REDDER ? 1 : 0;
                }
            }
        }
        return count;
    }

    /* The sum of red, green and blue a little off the screen's middle. */
    private static int bright(final ClientTestContext ctx, final int dx, final int dy) {
        final int[] colour = colour(ctx, dx, dy);
        return colour[0] + colour[1] + colour[2];
    }

    /*
     * How bright the screen is over a square under the crosshair, point by point, row by row, {@code margin} points
     * wider than the patch all round.
     */
    private static int[][] patch(final ClientTestContext ctx, final int margin) {
        final int side = 2 * (PATCH + margin) + 1;
        final int left = ctx.mc().getWindow().getGuiScaledWidth() / 2 - PATCH - margin;
        final int top = ctx.mc().getWindow().getGuiScaledHeight() / 2 + PATCH_DOWN - PATCH - margin;
        final int[][] out = new int[side][side];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    final int argb = frame.pixel(left + x, top + y);
                    out[y][x] = (argb >> 16 & CHANNEL) + (argb >> 8 & CHANNEL) + (argb & CHANNEL);
                }
            }
        }
        return out;
    }

    /*
     * How far the patch {@code before} has moved in {@code after}, read {@link #MOST_SHIFT} points wider all round: the
     * length of the shift, across and down, by which they differ least.
     */
    private static double shift(final int[][] before, final int[][] after) {
        int bestX = 0;
        int bestY = 0;
        long least = Long.MAX_VALUE;
        for (int dy = -MOST_SHIFT; dy <= MOST_SHIFT; dy++) {
            for (int dx = -MOST_SHIFT; dx <= MOST_SHIFT; dx++) {
                long sum = 0;
                for (int y = 0; y < before.length; y++) {
                    final int[] was = before[y];
                    final int[] now = after[y + MOST_SHIFT + dy];
                    for (int x = 0; x < was.length; x++) {
                        sum += Math.abs(was[x] - now[x + MOST_SHIFT + dx]);
                    }
                }
                if (sum < least) {
                    least = sum;
                    bestX = dx;
                    bestY = dy;
                }
            }
        }
        return Math.hypot(bestX, bestY);
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

    private static String rgb(final int[] colour) {
        return colour[0] + "," + colour[1] + "," + colour[2];
    }
}
