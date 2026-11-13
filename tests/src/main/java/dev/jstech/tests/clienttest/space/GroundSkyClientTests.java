/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.sky.SkyPlace;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkyMath;
import dev.jstech.core.sky.SkySpec;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestHelper;
import java.util.ArrayDeque;
import java.util.Objects;
import net.minecraft.client.Screenshot;
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
 * Phobos and Deimos stand where J's Core's sky puts them, at the size the approved views draw them, Phobos a bright
 * oblong a few pixels long and Deimos a point like a star, each brighter than every star round it.
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
    /*
     * Phobos' and Deimos' paths in Mars' sky file, and how they are looked for: high, the Sun well down, Phobos nearly
     * opposite it.
     */
    private static final double PHOBOS_OFFSET = 0.3;
    private static final double PHOBOS_ORBIT = -0.319;
    private static final double PHOBOS_TILT = 1.1;
    private static final double DEIMOS_OFFSET = 0.62;
    private static final double DEIMOS_ORBIT = -1.262;
    private static final double DEIMOS_TILT = 1.8;
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
    /*
     * How wide the approved views draw Phobos and Deimos, in degrees: Phobos five pixels of a frame 960 high, Deimos
     * two; and by how much more, plus a pixel, the game may draw them, its pixels falling where they may.
     */
    private static final double PHOBOS_WIDE = 0.42;
    private static final double DEIMOS_WIDE = 0.17;
    private static final double LEEWAY = 1.5;
    /*
     * A moon is read off the frame's own pixels, the GUI hidden: the brightest pixel this near the middle and the spot
     * of pixels at least this share of it joined to it; the stars round it, out to this far.
     */
    private static final int CORE = 10;
    private static final int RING = 80;
    private static final double SPOT_SHARE = 0.4;
    private static final int CHANNEL = 0xFF;

    private GroundSkyClientTests() {
    }

    @ClientTest(timeoutTicks = 4800)
    public static void groundSkies_showTheOverworldStillOverTheMoonTheHourByLongitudeAndPhobos(
            final ClientTestContext ctx) {
        final int[] near = new int[1];
        final double[] phobos = new double[3];
        final double[] deimos = new double[3];
        final boolean[] daylight = new boolean[1];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> back(ctx, daylight[0]));
        });
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
                    mars.setDayTime(upAtNight(PHOBOS_OFFSET, PHOBOS_ORBIT, PHOBOS_TILT, true, phobos));
                    lookAt(ctx, mars, phobos);
                })
                .thenWaitUntil(() -> in(ctx, SolarSystemWorlds.MARS), ARRIVE, "the player's game to reach Mars")
                .thenAwaitLandDrawn(ARRIVE)
                // Read off the frame with the GUI hidden: the crosshair would stand over a moon so small.
                .then(SETTLE, () -> ctx.mc().options.hideGui = true)
                .then(2, () -> assertMoon(ctx, "Phobos", PHOBOS_WIDE))
                .thenScreenshot(0, "mars_phobos")
                .thenServer(0, level -> {
                    final ServerLevel mars = world(ctx, SolarSystemWorlds.MARS);
                    mars.setDayTime(upAtNight(DEIMOS_OFFSET, DEIMOS_ORBIT, DEIMOS_TILT, false, deimos));
                    lookAt(ctx, mars, deimos);
                })
                // The server tells a player's game the time once a second.
                .then(SETTLE * 2, () -> assertMoon(ctx, "Deimos", DEIMOS_WIDE))
                .thenScreenshot(0, "mars_deimos");
    }

    /* The player back on the ground at home, no longer flying, the clocks going, and the copies made let go. */
    private static void back(final ClientTestContext ctx, final boolean daylight) {
        ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, ctx.server());
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        final BlockPos origin = ctx.origin().above(2);
        player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F, 0F);
    }

    /* Puts the player flying over the middle of {@code world}'s map, looking straight along {@code direction}. */
    private static void lookAt(final ClientTestContext ctx, final ServerLevel world, final double[] direction) {
        final float yaw = (float) Math.toDegrees(Math.atan2(-direction[0], direction[2]));
        final float pitch = (float) -Math.toDegrees(Math.asin(direction[1]));
        look(ctx, world, 0, 0, yaw, pitch);
    }

    /*
     * Fails unless the frame's middle holds a moon: a spot no wider than {@code wide} degrees, as the approved views
     * draw it, by the leeway and a pixel, brighter than every star round it. The spot is the brightest pixel near the
     * middle and every pixel joined to it at least a share as bright, so a star beside it is never taken for its
     * edge.
     */
    private static void assertMoon(final ClientTestContext ctx, final String which, final double wide) {
        try (NativeImage frame = Screenshot.takeScreenshot(ctx.mc().getMainRenderTarget())) {
            final int cx = frame.getWidth() / 2;
            final int cy = frame.getHeight() / 2;
            int bx = cx;
            int by = cy;
            int brightest = -1;
            for (int y = cy - CORE; y <= cy + CORE; y++) {
                for (int x = cx - CORE; x <= cx + CORE; x++) {
                    if (luminance(frame, x, y) > brightest) {
                        brightest = luminance(frame, x, y);
                        bx = x;
                        by = y;
                    }
                }
            }
            final int[] spot = spot(frame, bx, by, (int) Math.ceil(brightest * SPOT_SHARE));
            int star = 0;
            for (int y = cy - RING; y <= cy + RING; y++) {
                for (int x = cx - RING; x <= cx + RING; x++) {
                    if (Math.abs(x - cx) > CORE || Math.abs(y - cy) > CORE) {
                        star = Math.max(star, luminance(frame, x, y));
                    }
                }
            }
            final double perDegree = frame.getHeight() / 2.0
                    / Math.tan(Math.toRadians(ctx.mc().options.fov().get()) / 2) * Math.PI / 180;
            final double most = wide * perDegree * LEEWAY + 1;
            final int across = Math.max(spot[1], spot[2]);
            ctx.assertTrue(across <= most, which + " is drawn at its size: " + across + " pixels across, "
                    + spot[0] + " pixels in all, where the approved views draw it " + Math.round(wide * perDegree)
                    + " across");
            ctx.assertTrue(brightest > star, which + " outshines every star round it: " + brightest + " against "
                    + star);
        }
    }

    /*
     * The spot of pixels joined to ({@code x}, {@code y}), side by side or corner to corner, each at least
     * {@code least} bright, within the middle of the frame: how many there are, how wide and how tall.
     */
    private static int[] spot(final NativeImage frame, final int x, final int y, final int least) {
        final int cx = frame.getWidth() / 2;
        final int cy = frame.getHeight() / 2;
        final boolean[][] seen = new boolean[2 * CORE + 1][2 * CORE + 1];
        final ArrayDeque<int[]> open = new ArrayDeque<>();
        open.add(new int[] {x, y});
        seen[y - cy + CORE][x - cx + CORE] = true;
        int count = 0;
        int minX = x;
        int maxX = x;
        int minY = y;
        int maxY = y;
        while (!open.isEmpty()) {
            final int[] at = open.poll();
            count++;
            minX = Math.min(minX, at[0]);
            maxX = Math.max(maxX, at[0]);
            minY = Math.min(minY, at[1]);
            maxY = Math.max(maxY, at[1]);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    final int nx = at[0] + dx;
                    final int ny = at[1] + dy;
                    if (Math.abs(nx - cx) <= CORE && Math.abs(ny - cy) <= CORE
                            && !seen[ny - cy + CORE][nx - cx + CORE] && luminance(frame, nx, ny) >= least) {
                        seen[ny - cy + CORE][nx - cx + CORE] = true;
                        open.add(new int[] {nx, ny});
                    }
                }
            }
        }
        return new int[] {count, maxX - minX + 1, maxY - minY + 1};
    }

    /* The red, green and blue of the frame's pixel at ({@code x}, {@code y}), summed. */
    private static int luminance(final NativeImage frame, final int x, final int y) {
        final int abgr = frame.getPixelRGBA(x, y);
        return (abgr & CHANNEL) + (abgr >> 8 & CHANNEL) + (abgr >> 16 & CHANNEL);
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
     * A time of Mars' clock, at the middle of its map, when the moon on the path {@code offset}, {@code orbit} and
     * {@code tilt} stands well up and the Sun is down, and, if {@code full}, nearly opposite it; its direction then put
     * in {@code moon}: worked out as J's Core's sky works it, from the clock and the moon's path.
     */
    private static long upAtNight(final double offset, final double orbit, final double tilt, final boolean full,
                                  final double[] moon) {
        for (long time = 0; time < SEARCH_DAYS * (long) SkyMath.DAY; time += SEARCH_STEP) {
            final double celestial = SkyMath.celestial(time);
            final double turns = celestial + SkyMath.drift(offset, orbit, time / SkyMath.DAY);
            final double[] sky = SkyMath.direction(turns, tilt);
            // The sky's directions turned as the game turns its sky: a quarter round about the vertical.
            final double[] world = {-sky[2], sky[1], sky[0]};
            final double[] sun = SkyMath.sky(celestial, 0);
            final double facing = world[0] * sun[0] + world[1] * sun[1] + world[2] * sun[2];
            if (world[1] > HIGH && sun[1] < SUN_DOWN && (!full || facing < OPPOSITE)) {
                System.arraycopy(world, 0, moon, 0, 3);
                return time;
            }
        }
        throw new IllegalStateException("the moon never stands high at night in " + SEARCH_DAYS + " days");
    }

    /* The world {@code id} itself, its own generated land under its sky, as the Space mod declares it. */
    private static ServerLevel world(final ClientTestContext ctx, final ResourceLocation id) {
        return Objects.requireNonNull(ctx.server().getLevel(key(ctx, id)), "no dimension for " + id);
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
