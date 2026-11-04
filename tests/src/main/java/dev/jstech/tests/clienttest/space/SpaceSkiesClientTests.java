/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.sky.SkyMeteors;
import dev.jstech.core.client.weather.WeatherClient;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.MarsWeather;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.testkit.client.ClientTestHelper;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The skies of the Moon and Mars as their approved views drew them, read from the picture: the Milky Way across the
 * Moon's night, with the full Overworld and the planets; before the Moon's sunrise the zodiacal light standing up over
 * the east, brighter there than over the west; Mars' blue sunset round its Sun; a meteor burning across Mars' night; a
 * global dust storm hiding Mars' sky in brown, the Sun dim behind it.
 */
public final class SpaceSkiesClientTests {

    private static final int ARRIVE = 600;
    private static final int SETTLE = 40;
    private static final int ABOVE = 2;
    /* The hours at the middle of each world's map, on its clock in the game's day. */
    private static final long MIDNIGHT = 18_000L;
    private static final long BEFORE_SUNRISE = 22_900L;
    private static final long DUSK = 12_600L;
    /* Facing the four ways, as the game turns a player. */
    private static final float NORTH = 180F;
    private static final float EAST = -90F;
    private static final float WEST = 90F;
    /* Where the horizon's glow is read, in the GUI's points over the middle, the eye level, and how wide round. */
    private static final int LOW = 20;
    private static final int PATCH = 6;
    /* A meteor made to fall across the middle of the view, and when it is read, in ticks after it starts. */
    private static final double METEOR_FROM = 8.0;
    private static final double METEOR_TO = -8.0;
    private static final float METEOR_LIFE = 20F;
    private static final int METEOR_READ = 13;
    /*
     * How far round the middle a meteor is looked for, in the GUI's points; how bright a pixel of it is, red, green and
     * blue summed; and how many more such pixels it lights, at the least.
     */
    private static final int METEOR_REACH = 30;
    private static final int METEOR_LIT = 150;
    private static final int METEOR_TEXELS = 10;
    /*
     * A global dust storm in Mars' morning, looked at towards the Sun: the sky read this many GUI points over the
     * middle; how long it lasts and how long its weather takes to reach the player's game, in ticks; how much darker
     * the sky it hides is than the clear one, at the most.
     */
    private static final long MORNING = 2_500L;
    private static final float STORM_PITCH = -25F;
    private static final int STORM_SKY = -30;
    private static final int STORM_LASTS = 4_000;
    private static final int STORM_ARRIVES = 60;
    private static final double STORM_DARKER = 0.8;
    private static final int CHANNEL = 0xFF;

    private SpaceSkiesClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void skies_showTheMilkyWayZodiacalLightBlueSunsetAndMeteorsAsTheirViewsDrewThem(
            final ClientTestContext ctx) {
        final boolean[] daylight = new boolean[1];
        final int[] east = new int[1];
        final int[] background = new int[1];
        final int[][] clear = new int[1][];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> back(ctx, daylight[0]));
        });
        ctx.thenServer(0, level -> {
                    final GameRules.BooleanValue rule = level.getServer().getGameRules()
                            .getRule(GameRules.RULE_DAYLIGHT);
                    daylight[0] = rule.get();
                    rule.set(false, level.getServer());
                    final ServerLevel moon = world(ctx, SolarSystemWorlds.MOON);
                    moon.setDayTime(MIDNIGHT);
                    look(ctx, moon, NORTH, -60F);
                })
                .thenWaitUntil(() -> in(ctx, SolarSystemWorlds.MOON), ARRIVE, "the player's game to reach the Moon")
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> ctx.mc().options.hideGui = true)
                .thenScreenshot(2, "moon_night_up")
                .thenServer(0, level -> look(ctx, world(ctx, SolarSystemWorlds.MOON), NORTH, 25F))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "moon_night_ground")
                .thenServer(0, level -> {
                    final ServerLevel moon = world(ctx, SolarSystemWorlds.MOON);
                    moon.setDayTime(BEFORE_SUNRISE);
                    look(ctx, moon, EAST, 0F);
                })
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> east[0] = bright(ctx, 0, -LOW, PATCH))
                .thenScreenshot(0, "moon_dawn_east")
                .thenServer(0, level -> look(ctx, world(ctx, SolarSystemWorlds.MOON), WEST, 0F))
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> {
                    final int west = bright(ctx, 0, -LOW, PATCH);
                    ctx.assertTrue(east[0] > west + 4, "before the Moon's sunrise the zodiacal light stands over the"
                            + " east: " + east[0] + " there against " + west + " over the west");
                })
                .thenScreenshot(0, "moon_dawn_west")
                .thenServer(0, level -> {
                    final ServerLevel mars = world(ctx, SolarSystemWorlds.MARS);
                    mars.setDayTime(DUSK);
                    look(ctx, mars, WEST, 0F);
                })
                .thenWaitUntil(() -> in(ctx, SolarSystemWorlds.MARS), ARRIVE, "the player's game to reach Mars")
                .thenAwaitLandDrawn(ARRIVE)
                .thenServer(0, level -> calm(world(ctx, SolarSystemWorlds.MARS)))
                .thenWaitUntil(() -> WeatherClient.around().isEmpty(), ARRIVE, "Mars' sky to be clear of dust")
                .then(SETTLE, () -> {
                    final int[] rgb = colour(ctx, 0, -LOW, PATCH);
                    ctx.assertTrue(rgb[2] > rgb[0], "Mars' sunset is blue round its Sun: red " + rgb[0] + ", blue "
                            + rgb[2]);
                })
                .thenScreenshot(0, "mars_dusk")
                .thenServer(0, level -> {
                    final ServerLevel mars = world(ctx, SolarSystemWorlds.MARS);
                    mars.setDayTime(MIDNIGHT);
                    look(ctx, mars, NORTH, -45F);
                    calm(mars);
                })
                .thenAwaitLandDrawn(ARRIVE)
                .thenWaitUntil(() -> WeatherClient.around().isEmpty(), ARRIVE, "Mars' night to be clear of dust")
                .then(SETTLE, () -> background[0] = lit(ctx, METEOR_REACH, METEOR_LIT))
                .thenScreenshot(0, "mars_night")
                .then(0, () -> SkyMeteors.fall(NORTH + METEOR_FROM, 45 + METEOR_FROM, NORTH + METEOR_TO,
                        45 + METEOR_TO, METEOR_LIFE))
                .then(METEOR_READ, () -> {
                    final int meteor = lit(ctx, METEOR_REACH, METEOR_LIT);
                    ctx.assertTrue(meteor >= background[0] + METEOR_TEXELS, "a meteor burns across Mars' night: "
                            + meteor + " bright pixels round the middle of the view where there were "
                            + background[0]);
                })
                .thenScreenshot(0, "mars_meteor")
                .thenServer(0, level -> {
                    final ServerLevel mars = world(ctx, SolarSystemWorlds.MARS);
                    mars.setDayTime(MORNING);
                    look(ctx, mars, EAST, STORM_PITCH);
                })
                .then(SETTLE, () -> clear[0] = colour(ctx, 0, STORM_SKY, PATCH))
                .thenScreenshot(0, "mars_clear")
                .thenServer(0, level -> {
                    // A storm the day's roll planned for later would keep this one from starting: it goes first.
                    final ServerLevel mars = world(ctx, SolarSystemWorlds.MARS);
                    WorldEvents.stop(mars, MarsWeather.GLOBAL_DUST_STORM);
                    ctx.assertTrue(WorldEvents.start(mars, MarsWeather.GLOBAL_DUST_STORM, STORM_LASTS,
                            Optional.empty()), "a global dust storm starts on Mars");
                })
                .then(STORM_ARRIVES, () -> {
                    final int[] storm = colour(ctx, 0, STORM_SKY, PATCH);
                    final int before = clear[0][0] + clear[0][1] + clear[0][2];
                    final int after = storm[0] + storm[1] + storm[2];
                    ctx.assertTrue(after < before * STORM_DARKER && storm[0] > storm[2], "a global dust storm hides"
                            + " Mars' sky in brown: " + rgb(storm) + " where the clear sky was " + rgb(clear[0]));
                })
                .thenScreenshot(0, "mars_storm")
                .thenServer(0, level -> WorldEvents.stop(world(ctx, SolarSystemWorlds.MARS),
                        MarsWeather.GLOBAL_DUST_STORM));
    }

    /*
     * Ends any dust storm on {@code mars}, planned or under way: the day's roll brings a regional one now and then,
     * and its dust veils the sky the test reads, the sunset and the stars a meteor is told from.
     */
    private static void calm(final ServerLevel mars) {
        WorldEvents.stop(mars, MarsWeather.DUST_STORM);
        WorldEvents.stop(mars, MarsWeather.GLOBAL_DUST_STORM);
    }

    private static String rgb(final int[] colour) {
        return colour[0] + "/" + colour[1] + "/" + colour[2];
    }

    /* Puts the player over the middle of {@code world}'s map, flying, facing {@code yaw} with {@code pitch}. */
    private static void look(final ClientTestContext ctx, final ServerLevel world, final float yaw,
                             final float pitch) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        world.getChunk(BlockPos.containing(0, 0, 0));
        final int ground = world.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        player.teleportTo(world, 0.5, ground + ABOVE, 0.5, yaw, pitch);
    }

    private static void back(final ClientTestContext ctx, final boolean daylight) {
        ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, ctx.server());
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        final BlockPos origin = ctx.origin().above(2);
        player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F, 0F);
        forget(ctx, SolarSystemWorlds.MOON);
        forget(ctx, SolarSystemWorlds.MARS);
    }

    /* The world, made as a flat land under its own sky, so the sky is all that is looked at. */
    private static ServerLevel world(final ClientTestContext ctx, final ResourceLocation id) {
        return RuntimeDimensions.getOrCreate(ctx.server(), key(ctx, id), TestWorldGen.SKY_LAND.id());
    }

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

    /* How many pixels {@code reach} GUI points round the middle are at least {@code least} bright, the three summed. */
    private static int lit(final ClientTestContext ctx, final int reach, final int least) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int ox = -reach; ox <= reach; ox++) {
                for (int oy = -reach; oy <= reach; oy++) {
                    final int argb = frame.pixel(x + ox, y + oy);
                    if ((argb >> 16 & CHANNEL) + (argb >> 8 & CHANNEL) + (argb & CHANNEL) >= least) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /* The mean of red, green and blue summed round a point {@code dx, dy} GUI points from the middle. */
    private static int bright(final ClientTestContext ctx, final int dx, final int dy, final int reach) {
        final int[] rgb = colour(ctx, dx, dy, reach);
        return rgb[0] + rgb[1] + rgb[2];
    }

    /* The mean red, green and blue round a point {@code dx, dy} GUI points from the middle. */
    private static int[] colour(final ClientTestContext ctx, final int dx, final int dy, final int reach) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 + dx;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + dy;
        final int[] sum = new int[3];
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int ox = -reach; ox <= reach; ox++) {
                for (int oy = -reach; oy <= reach; oy++) {
                    final int argb = frame.pixel(x + ox, y + oy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                    count++;
                }
            }
        }
        return new int[] {sum[0] / count, sum[1] / count, sum[2] / count};
    }
}
