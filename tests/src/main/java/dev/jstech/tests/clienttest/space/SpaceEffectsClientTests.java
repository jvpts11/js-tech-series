/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.effect.Effects;
import dev.jstech.space.descent.DescentFlight;
import dev.jstech.space.launch.ClimbEffects;
import dev.jstech.space.rocket.RocketStaging;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The rocket's effects as the approved looks draw them, each fired before the camera against the sky: a plume in air,
 * bright flame and smoke; one in a vacuum, pale blue and wide; the trail of a climb, white; the glow of re-entry,
 * orange; a stage's retro rockets, flame; the dust of a touchdown on Mars, rust; on the Moon, grey, flying out flat.
 */
public final class SpaceEffectsClientTests {

    private static final BlockPos EYE = new BlockPos(0, 2, 0);
    private static final BlockPos AT = new BlockPos(0, 9, 14);
    private static final BlockPos GROUND = new BlockPos(0, 1, 10);
    private static final int SETTLE = 20;
    private static final int GROWN = 8;
    private static final int CHANNEL = 0xFF;
    /* How far round the middle of the window it is looked for, every few pixels; the least of it there. */
    private static final int SAMPLE = 140;
    private static final int STEP = 3;
    private static final int LEAST = 25;
    private static final Vec3 DOWN = new Vec3(0, -1, 0);
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final long NOON = 6_000;
    private static final long MIDNIGHT = 18_000;

    private SpaceEffectsClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void effects_inDaylightPlumeIsFlameAndTrailWhite(final ClientTestContext ctx) {
        fired(fired(ctx.thenTeleport(0, EYE, Direction.SOUTH), ctx, ClimbEffects.IN_AIR, AT, DOWN, "plume-in-air",
                        "the plume in air is bright flame", rgb -> red(rgb) > 200 && green(rgb) > 100
                        && blue(rgb) < 120),
                ctx, ClimbEffects.TRAIL, AT, DOWN, "climb-trail", "the climb's trail is white smoke, not the sky",
                rgb -> red(rgb) > 200 && green(rgb) > 200 && blue(rgb) > 200 && blue(rgb) < red(rgb) + 30);
    }

    // High up and in space the sky is dark: the vacuum's plume, re-entry and staging are seen against it.
    @ClientTest(timeoutTicks = 800)
    public static void effects_againstTheDarkVacuumPlumeReentryAndRetroRockets(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().setDayTime(NOON)));
        final ClientTestContext steps = ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> level.setDayTime(MIDNIGHT));
        fired(fired(fired(steps, ctx, ClimbEffects.IN_VACUUM, AT, DOWN, "plume-in-vacuum",
                                "the plume in a vacuum is pale blue", rgb -> blue(rgb) > 170
                                && blue(rgb) > red(rgb) + 10 && green(rgb) > 140),
                        ctx, DescentFlight.REENTRY, AT, UP, "reentry", "re-entry glows orange",
                        rgb -> red(rgb) > 200 && green(rgb) > 60 && green(rgb) < 200 && blue(rgb) < 110),
                ctx, RocketStaging.SEPARATION, AT, UP, "separation", "a stage's retro rockets fire",
                rgb -> red(rgb) > 200 && green(rgb) > 80 && blue(rgb) < 110);
    }

    @ClientTest(timeoutTicks = 600)
    public static void effects_touchdownDustRustOnMarsGreyOnTheMoon(final ClientTestContext ctx) {
        fired(fired(ctx.thenTeleport(0, EYE, Direction.SOUTH), ctx, DescentFlight.MARS_DUST, GROUND, UP, "dust-mars",
                        "Mars' dust is rust", rgb -> red(rgb) > 130 && red(rgb) > green(rgb) + 35
                        && blue(rgb) < green(rgb)),
                ctx, DescentFlight.MOON_DUST, GROUND, new Vec3(0, 0.06, -1), "dust-moon",
                "the Moon's dust is grey, flying out flat", rgb -> Math.abs(red(rgb) - green(rgb)) < 10
                        && Math.abs(green(rgb) - blue(rgb)) < 12 && red(rgb) > 110 && red(rgb) < 200);
    }

    /*
     * Fires {@code effect} at {@code at} facing {@code way}, looks at it, and asserts {@code what}: enough points
     * round the middle of the window match {@code colour}.
     */
    private static ClientTestContext fired(final ClientTestContext steps, final ClientTestContext ctx,
                                           final ResourceLocation effect, final BlockPos at, final Vec3 way,
                                           final String label, final String what, final IntPredicate colour) {
        final int[] seen = new int[1];
        return steps.then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(at))))
                .thenServer(0, level -> Effects.at(level, effect, Vec3.atCenterOf(ctx.abs(at))).facing(way).send())
                .then(GROWN, () -> seen[0] = count(ctx, colour))
                .thenScreenshot(0, label)
                .then(0, () -> ctx.assertTrue(seen[0] >= LEAST, what + ": " + seen[0] + " points"));
    }

    private static int count(final ClientTestContext ctx, final IntPredicate colour) {
        final int cx = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int cy = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int found = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = cx - SAMPLE; x <= cx + SAMPLE; x += STEP) {
                for (int y = cy - SAMPLE; y <= cy + SAMPLE; y += STEP) {
                    if (colour.test(frame.pixel(x, y))) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    private static int red(final int argb) {
        return argb >> 16 & CHANNEL;
    }

    private static int green(final int argb) {
        return argb >> 8 & CHANNEL;
    }

    private static int blue(final int argb) {
        return argb & CHANNEL;
    }
}
