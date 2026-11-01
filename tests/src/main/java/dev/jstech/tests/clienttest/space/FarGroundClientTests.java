/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.space.network.FarMapPayload;
import dev.jstech.space.universe.ClimbView;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Locale;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * The way up from the ground of the Overworld, the Moon and Mars on a player's game, read from the picture at four
 * heights of the climb, the GUI hidden, each world under its own air: the Moon has none, so no haze, no veil and no
 * cloud over its ground, and none of the Overworld's blue; Mars' haze is its own butterscotch, never the Overworld's
 * blue, and its loaded blocks are drawn under it, not lost in its fog. Each photograph is told in the log with what
 * the climb and the far map are there.
 */
public final class FarGroundClientTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    /* The heights the climb is photographed at, over every world's build limit of 320. */
    private static final int[] HEIGHTS = {400, 569, 700, 900};
    private static final long NOON = 6_000L;
    private static final float NORTH = 180F;
    private static final float DOWN = 60F;
    private static final int ARRIVE = 600;
    /* The far map is drawn on the server away from its thread and sent: a few seconds. */
    private static final int FAR_MAP = 120;
    /* The part of the frame read, in hundredths of its width and height: the ground below the horizon. */
    private static final int LEFT = 5;
    private static final int RIGHT = 95;
    private static final int TOP = 45;
    private static final int BOTTOM = 95;
    /* Where the loaded blocks lie on the frame low in the climb: under the middle, toward the bottom. */
    private static final int LOADED_LEFT = 42;
    private static final int LOADED_RIGHT = 58;
    private static final int LOADED_TOP = 80;
    private static final int LOADED_BOTTOM = 98;
    private static final int HUNDRED = 100;
    private static final int STEP = 2;
    private static final int CHANNEL = 0xFF;
    private static final int CHANNELS = 3;
    /*
     * A pixel as white as a cloud, every channel over this; the most share of them a cloudless ground shows; how far
     * apart red and blue may be on the Moon's grey; and how much the loaded blocks must vary, where a fog drawn
     * over them whole is one flat colour.
     */
    private static final int CLOUD_WHITE = 190;
    private static final double FEW_WHITE = 0.01;
    private static final double GREY = 20;
    private static final double TEXTURED = 3;

    private FarGroundClientTests() {
    }

    @ClientTest(timeoutTicks = 12_000)
    public static void climb_showsEachWorldsGroundUnderItsOwnAir(final ClientTestContext ctx) {
        final boolean[] daylight = new boolean[1];
        ctx.afterTest(() -> {
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight[0], ctx.server());
                final ServerPlayer player = ctx.serverPlayer();
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                final BlockPos origin = ctx.origin().above(2);
                player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5,
                        0F, 0F);
            });
        });
        ctx.thenServer(0, level -> {
            final GameRules.BooleanValue rule = level.getServer().getGameRules().getRule(GameRules.RULE_DAYLIGHT);
            daylight[0] = rule.get();
            rule.set(false, level.getServer());
        }).then(0, () -> ctx.mc().options.hideGui = true);
        for (final SolarBody body : SolarBody.values()) {
            for (final int height : HEIGHTS) {
                ctx.thenServer(0, level -> {
                    final ServerLevel world = world(ctx, body);
                    world.setDayTime(NOON);
                    climb(ctx, world, body, height);
                })
                        .thenWaitUntil(() -> ctx.mc().level != null
                                && ctx.mc().level.dimension().equals(world(ctx, body).dimension())
                                && ctx.mc().player.getY() > height - 2, ARRIVE, body + " climbed to " + height)
                        .then(FAR_MAP, () -> check(ctx, body, height, read(ctx, body, height)))
                        .thenScreenshot(0, body.name().toLowerCase(Locale.ROOT) + "_" + height);
            }
        }
    }

    /* Fails unless the ground below looks as {@code body}'s air lets it at {@code height}. */
    private static void check(final ClientTestContext ctx, final SolarBody body, final int height,
                              final Ground seen) {
        final String where = body + " at " + height + ": ";
        if (body == SolarBody.MOON) {
            ctx.assertTrue(seen.white() < FEW_WHITE, where + "no cloud over a world with no air, but "
                    + Math.round(seen.white() * HUNDRED) + "% of the ground is cloud white");
            ctx.assertTrue(Math.abs(seen.red() - seen.blue()) < GREY, where + "its grey under no air, not the "
                    + "Overworld's blue: " + seen);
        }
        if (body == SolarBody.MARS) {
            ctx.assertTrue(seen.red() >= seen.blue(), where + "its own butterscotch air, never the Overworld's blue: "
                    + seen);
            if (height == HEIGHTS[0]) {
                ctx.assertTrue(seen.loadedVaries() > TEXTURED, where + "the loaded blocks drawn under its haze, not"
                        + " lost in one flat fog: " + seen);
            }
        }
    }

    /* Puts the player flying over the middle of {@code body}'s world, or the test's place on the Overworld. */
    private static void climb(final ClientTestContext ctx, final ServerLevel world, final SolarBody body,
                              final int height) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        final BlockPos at = body == SolarBody.OVERWORLD ? ctx.origin() : BlockPos.ZERO;
        player.teleportTo(world, at.getX() + 0.5, height, at.getZ() + 0.5, NORTH, DOWN);
    }

    /* The world {@code body} is drawn as: the Overworld, or the Moon's or Mars' own dimension, made from its file. */
    private static ServerLevel world(final ClientTestContext ctx, final SolarBody body) {
        final ResourceKey<Level> key = switch (body) {
            case OVERWORLD -> Level.OVERWORLD;
            case MOON -> MoonWorld.world().dimension();
            case MARS -> MarsWorld.world().dimension();
        };
        return body == SolarBody.OVERWORLD ? ctx.server().overworld()
                : RuntimeDimensions.getOrCreate(ctx.server(), key, key.location());
    }

    /* Reads the ground below from the frame and tells it, with what the climb and the far map are there. */
    private static Ground read(final ClientTestContext ctx, final SolarBody body, final int height) {
        final ServerLevel world = world(ctx, body);
        final double width = Math.max(1, world.getWorldBorder().getSize());
        final double x = ctx.mc().player.getX();
        final double z = ctx.mc().player.getZ();
        final int sea = FarMaps.seaOf(world);
        final ClimbView.Climb climb = ClimbView.at(body, SphereMap.longitude(x, width), SphereMap.latitude(z, width),
                height, world.getMaxBuildHeight(), sea, width);
        final Ground seen = ground(ctx);
        final FarMapPayload map = FarMaps.draw(world, 0, (int) Math.floor(x), (int) Math.floor(z));
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (final byte packed : map.heights()) {
            lowest = Math.min(lowest, FarMapPayload.blocks(packed));
            highest = Math.max(highest, FarMapPayload.blocks(packed));
        }
        LOGGER.info(String.format(Locale.ROOT, "[FAR] %s y=%d limit=%d sea=%d share=%.3f eye=%.0f ground=%.2f"
                        + " veil=%.2f sphere=%.2f blocksGone=%s far map %d..%d %s; %s", body, height,
                world.getMaxBuildHeight(), sea, climb.share(), climb.eyeHeight(), climb.ground(), climb.veil(),
                climb.sphere(), climb.blocksGone(), lowest, highest, map.biomes(), seen));
        return seen;
    }

    /* The ground on the frame: its mean grey and colour, its share of cloud white, and how the loaded blocks vary. */
    private static Ground ground(final ClientTestContext ctx) {
        try (NativeImage frame = Screenshot.takeScreenshot(ctx.mc().getMainRenderTarget())) {
            final double[] all = read(frame, LEFT, RIGHT, TOP, BOTTOM);
            final double[] loaded = read(frame, LOADED_LEFT, LOADED_RIGHT, LOADED_TOP, LOADED_BOTTOM);
            return new Ground(all[0], all[1], all[2], all[3], all[4], all[5], loaded[1]);
        }
    }

    /*
     * Over a part of the frame, in hundredths of it: the mean grey, how much it varies, the mean red, green and blue,
     * and the share of cloud white pixels.
     */
    private static double[] read(final NativeImage frame, final int left, final int right, final int top,
                                 final int bottom) {
        double sum = 0;
        double squares = 0;
        final double[] rgb = new double[CHANNELS];
        int white = 0;
        int count = 0;
        for (int y = frame.getHeight() * top / HUNDRED; y < frame.getHeight() * bottom / HUNDRED; y += STEP) {
            for (int x = frame.getWidth() * left / HUNDRED; x < frame.getWidth() * right / HUNDRED; x += STEP) {
                final int abgr = frame.getPixelRGBA(x, y);
                final int r = abgr & CHANNEL;
                final int g = abgr >> 8 & CHANNEL;
                final int b = abgr >> 16 & CHANNEL;
                final double grey = (r + g + b) / (double) CHANNELS;
                sum += grey;
                squares += grey * grey;
                rgb[0] += r;
                rgb[1] += g;
                rgb[2] += b;
                white += r > CLOUD_WHITE && g > CLOUD_WHITE && b > CLOUD_WHITE ? 1 : 0;
                count++;
            }
        }
        final double mean = sum / count;
        return new double[] {mean, Math.sqrt(Math.max(0, squares / count - mean * mean)), rgb[0] / count,
            rgb[1] / count, rgb[2] / count, (double) white / count};
    }

    /* The ground as the frame shows it. */
    private record Ground(double grey, double varies, double red, double green, double blue, double white,
                          double loadedVaries) {

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "grey %.1f varies %.1f rgb %.0f,%.0f,%.0f white %.1f%% loaded varies"
                    + " %.1f", grey, varies, red, green, blue, white * HUNDRED, loadedVaries);
        }
    }
}
