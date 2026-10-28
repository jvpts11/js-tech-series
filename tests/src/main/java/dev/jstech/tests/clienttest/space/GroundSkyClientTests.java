/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.sky.SkyPlace;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkyMath;
import dev.jstech.core.sky.SkySpec;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The skies seen from the ground, read from the picture: on the Moon, the Overworld hangs high in the north over the
 * middle of the map, full at the Moon's midnight there, and is gone from the far side; the hour follows the longitude,
 * so at one moment a block is dark at the middle's midnight and lit by the late morning on the far side; on Mars,
 * Phobos stands where J's Core's sky puts it, lit against the night.
 */
public final class GroundSkyClientTests {

    private static final int ARRIVE = 600;
    private static final int DRAWN = 600;
    private static final int SETTLE = 20;
    /* How high over the ground the player looks from. */
    private static final int ABOVE = 12;
    /* The Moon's midnight at the middle of its map, on its clock in the game's day. */
    private static final long MIDNIGHT = 18_000L;
    /*
     * How far round the Moon the far side is looked at from, as a share of its border from the middle: 162 degrees,
     * where the middle's midnight is the morning, the Sun clear of where the Overworld would hang.
     */
    private static final double FAR_SIDE = 0.45;
    /* Phobos' path in Mars' sky file, and how it is looked for: high, the Sun well down and nearly opposite it. */
    private static final double PHOBOS_OFFSET = 0.3;
    private static final double PHOBOS_ORBIT = -0.319;
    private static final double PHOBOS_TILT = 1.1;
    private static final double HIGH = 0.6;
    private static final double SUN_DOWN = -0.3;
    private static final double OPPOSITE = -0.5;
    private static final int SEARCH_DAYS = 10;
    private static final long SEARCH_STEP = 20;
    private static final float NORTH = 180F;
    private static final float UP = -90F;
    /* Where the screen is read, in the GUI's points from its middle: clear of the crosshair. */
    private static final int OFF_CROSSHAIR = 12;
    /* How far round a patch is averaged, so a star in it hardly counts. */
    private static final int WIDE_PATCH = 4;
    private static final int NARROW_PATCH = 1;
    private static final int LIT = 120;
    private static final int DARK = 45;
    private static final int BLOCK_LIT = 450;
    private static final int BLOCK_DARK = 250;
    /* How much brighter Phobos is than the night sky beside it, and how far beside it the sky is read. */
    private static final int PHOBOS_OVER_SKY = 40;
    private static final int BESIDE = 30;
    private static final int CHANNEL = 0xFF;

    private GroundSkyClientTests() {
    }

    @ClientTest(timeoutTicks = 4800)
    public static void groundSkies_showTheOverworldStillOverTheMoonTheHourByLongitudeAndPhobos(
            final ClientTestContext ctx) {
        final int[] near = new int[1];
        final double[] phobos = new double[3];
        final boolean[] daylight = new boolean[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight[0], ctx.server());
            // Back on the ground and no longer flying, as the next test expects the player.
            final ServerPlayer player = ctx.serverPlayer();
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            final BlockPos origin = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F,
                    0F);
            forget(ctx, SolarSystemWorlds.MOON);
            forget(ctx, SolarSystemWorlds.MARS);
        }));
        ctx.thenServer(0, level -> {
            // The clocks held still, so the Moon's hour and Phobos stay where they are put while the player arrives.
            final GameRules.BooleanValue rule = level.getServer().getGameRules().getRule(GameRules.RULE_DAYLIGHT);
            daylight[0] = rule.get();
            rule.set(false, level.getServer());
            final ServerLevel moon = world(ctx, SolarSystemWorlds.MOON);
            moon.setDayTime(MIDNIGHT);
            final float pitch = (float) -(90 - SystemLayout.OVERWORLD_OVER_THE_MOON);
            look(ctx, moon, 0, 0, NORTH, pitch);
        })
                .thenWaitUntil(() -> in(ctx, SolarSystemWorlds.MOON), ARRIVE, "the player's game to reach the Moon")
                .thenAwaitLandDrawn(ARRIVE)
                .thenWaitUntil(() -> bright(ctx, 0, OFF_CROSSHAIR, NARROW_PATCH) > LIT, DRAWN,
                        "the full Overworld to be drawn high in the north at the Moon's midnight")
                .thenScreenshot(SETTLE, "moon_overworld_full")
                .then(0, () -> near[0] = bright(ctx, 0, OFF_CROSSHAIR, WIDE_PATCH))
                .thenServer(0, level -> {
                    final ServerLevel moon = world(ctx, SolarSystemWorlds.MOON);
                    final float pitch = (float) -(90 - SystemLayout.OVERWORLD_OVER_THE_MOON);
                    look(ctx, moon, farSide(moon), 0, NORTH, pitch);
                })
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    final int far = bright(ctx, 0, OFF_CROSSHAIR, WIDE_PATCH);
                    ctx.assertTrue(far < DARK, "the far side never sees the Overworld: " + far + " where the middle"
                            + " of the map saw " + near[0]);
                })
                .thenScreenshot(0, "moon_far_side")
                .thenServer(0, level -> block(ctx, world(ctx, SolarSystemWorlds.MOON), 0))
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> ctx.assertTrue(bright(ctx, 0, OFF_CROSSHAIR, NARROW_PATCH) < BLOCK_DARK,
                        "at the middle's midnight a block there is dark: "
                                + bright(ctx, 0, OFF_CROSSHAIR, NARROW_PATCH)))
                .thenServer(0, level -> {
                    final ServerLevel moon = world(ctx, SolarSystemWorlds.MOON);
                    block(ctx, moon, farSide(moon));
                })
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "moon_far_side_morning")
                .then(0, () -> {
                    final SkySpec sky = Skies.of(ctx.mc().level);
                    final int lit = bright(ctx, 0, OFF_CROSSHAIR, NARROW_PATCH);
                    ctx.assertTrue(lit > BLOCK_LIT, "at the same moment the far side has its late morning, and a"
                            + " block there is lit: " + lit + "; the sky round: " + sky.round() + ", seen from "
                            + SkyPlace.of(ctx.mc().level, sky));
                })
                .thenServer(0, level -> {
                    final ServerLevel mars = world(ctx, SolarSystemWorlds.MARS);
                    final long time = phobosUpAtNight(phobos);
                    mars.setDayTime(time);
                    final float yaw = (float) Math.toDegrees(Math.atan2(-phobos[0], phobos[2]));
                    final float pitch = (float) -Math.toDegrees(Math.asin(phobos[1]));
                    look(ctx, mars, 0, 0, yaw, pitch);
                })
                .thenWaitUntil(() -> in(ctx, SolarSystemWorlds.MARS), ARRIVE, "the player's game to reach Mars")
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    // Phobos is small: read just off the crosshair's middle, and the sky well beside it.
                    final int moonlet = bright(ctx, 0, 3, NARROW_PATCH);
                    final int sky = bright(ctx, BESIDE, 3, NARROW_PATCH);
                    ctx.assertTrue(moonlet > sky + PHOBOS_OVER_SKY, "Phobos stands lit where the sky kit puts it: "
                            + moonlet + " against the sky's " + sky);
                })
                .thenScreenshot(0, "mars_phobos");
    }

    /* Puts the player flying over the ground of {@code world} at ({@code x}, {@code z}). */
    private static void look(final ClientTestContext ctx, final ServerLevel world, final double x, final double z,
                             final float yaw, final float pitch) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        world.getChunk(BlockPos.containing(x, 0, z));
        final int ground = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        player.teleportTo(world, x, ground + ABOVE, z, yaw, pitch);
    }

    /* Lays white concrete over the ground at the middle's latitude, {@code x} along, and looks down on it. */
    private static void block(final ClientTestContext ctx, final ServerLevel world, final double x) {
        world.getChunk(BlockPos.containing(x, 0, 0));
        final int ground = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), 0);
        final BlockPos at = new BlockPos((int) Math.floor(x), ground + 1, 0);
        world.setBlockAndUpdate(at, Blocks.WHITE_CONCRETE.defaultBlockState());
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(world, at.getX() + 0.5, at.getY() + 4, at.getZ() + 0.5, NORTH, -UP);
    }

    /* How far east of the middle the far side is looked at, in blocks. */
    private static double farSide(final ServerLevel world) {
        return world.getWorldBorder().getSize() * FAR_SIDE;
    }

    /*
     * A time of Mars' clock, at the middle of its map, when Phobos stands well up and the Sun is down, its direction
     * then put in {@code phobos}: worked out as J's Core's sky works it, from the clock and Phobos' path.
     */
    private static long phobosUpAtNight(final double[] phobos) {
        for (long time = 0; time < SEARCH_DAYS * (long) SkyMath.DAY; time += SEARCH_STEP) {
            final double celestial = SkyMath.celestial(time);
            final double turns = celestial + SkyMath.drift(PHOBOS_OFFSET, PHOBOS_ORBIT, time / SkyMath.DAY);
            final double[] sky = SkyMath.direction(turns, PHOBOS_TILT);
            // The sky's directions turned as the game turns its sky: a quarter round about the vertical.
            final double[] world = {-sky[2], sky[1], sky[0]};
            final double[] sun = SkyMath.sky(celestial, 0);
            final double facing = world[0] * sun[0] + world[1] * sun[1] + world[2] * sun[2];
            // High, the Sun down, and opposite it, so nearly full.
            if (world[1] > HIGH && sun[1] < SUN_DOWN && facing < OPPOSITE) {
                System.arraycopy(world, 0, phobos, 0, 3);
                return time;
            }
        }
        throw new IllegalStateException("Phobos never stands high and full at night in " + SEARCH_DAYS + " days");
    }

    /*
     * The dimension of the world {@code id}: until its generator shapes it, a copy of the test land made under its
     * name, which takes the world's own sky, rules and size by that name.
     */
    private static ServerLevel world(final ClientTestContext ctx, final ResourceLocation id) {
        return RuntimeDimensions.getOrCreate(ctx.server(), key(ctx, id), TestWorldGen.SKY_LAND.id());
    }

    /* Lets go of the copy {@code id}'s dimension is, if it is one this test made. */
    private static void forget(final ClientTestContext ctx, final ResourceLocation id) {
        final ResourceKey<Level> key = key(ctx, id);
        if (RuntimeDimensions.templateOf(key.location()).isPresent()) {
            RuntimeDimensions.remove(ctx.server(), key);
        }
    }

    private static ResourceKey<Level> key(final ClientTestContext ctx, final ResourceLocation id) {
        return SpaceWorlds.get(ctx.server().overworld(), id).map(SpaceWorld::dimension).orElseThrow();
    }

    private static boolean in(final ClientTestContext ctx, final ResourceLocation id) {
        return ctx.mc().level != null && SpaceWorlds.get(ctx.mc().level, id).map(SpaceWorld::dimension)
                .filter(ctx.mc().level.dimension()::equals).isPresent();
    }

    /*
     * The mean sum of red, green and blue over a square of the GUI's points, {@code dx} and {@code dy} off the middle.
     */
    private static int bright(final ClientTestContext ctx, final int dx, final int dy, final int reach) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 + dx;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + dy;
        int sum = 0;
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int ox = -reach; ox <= reach; ox++) {
                for (int oy = -reach; oy <= reach; oy++) {
                    final int argb = frame.pixel(x + ox, y + oy);
                    sum += (argb >> 16 & CHANNEL) + (argb >> 8 & CHANNEL) + (argb & CHANNEL);
                    count++;
                }
            }
        }
        return sum / count;
    }
}
