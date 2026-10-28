/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

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
 * daylight; and over Mars the ground is red.
 */
public final class SystemSpaceClientTests {

    private static final ResourceKey<Level> SPACE = SystemSpaces.keyOf(StarSystems.SOLAR_SYSTEM);
    private static final int ARRIVE = 600;
    /* The Overworld's maps are drawn from the seed away from the game's thread, which takes a few seconds. */
    private static final int DRAWN = 600;
    private static final int SETTLE = 10;
    /* How far west or east of a world's middle the player looks down from, in blocks. */
    private static final double ASIDE = 600;
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
    private static final int SUN = 400;
    /* White concrete's top in full daylight, and how far under the player it is put. */
    private static final int DAYLIT = 450;
    private static final double BELOW = 3;
    private static final float SOUTH = 0F;
    private static final float WEST = 90F;
    private static final float NORTH = 180F;
    private static final float DOWN = 90F;
    private static final int CHANNEL = 0xFF;

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
                    ctx.assertTrue(west > LIT && east < DARK, "the line of dawn runs below, day west and night east: "
                            + west + " against " + east);
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

    /* The sum of red, green and blue a little off the screen's middle. */
    private static int bright(final ClientTestContext ctx, final int dx, final int dy) {
        final int[] colour = colour(ctx, dx, dy);
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

    private static String rgb(final int[] colour) {
        return colour[0] + "," + colour[1] + "," + colour[2];
    }
}
