/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.TestRockets;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The rocket parts' formed models on a player's game: a small rocket on a stand is drawn part by part where the bare
 * stand shows only what is behind it, its engine's copper bell at the foot, its tank's white paint, the black
 * interstage skirt its separation ring raises round the vacuum engine above it, and its capsule at the top.
 */
public final class RocketPartsClientTests {

    /* The player twelve blocks south of the stand, looking north at it. */
    private static final BlockPos EYE = new BlockPos(0, 2, -12);
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    /*
     * The heights looked at: the sea-level engine's bell, the first tank, the skirt round the vacuum engine, the
     * capsule.
     */
    private static final double BELL = 0.5;
    private static final double TANK = 3.5;
    private static final double SKIRT = 7.5;
    private static final double CAPSULE = 13.2;
    /* How far out from the rocket's axis, towards the player, each of them is looked at: on its surface. */
    private static final double[] OUT = {0.3, 1.4, 1.4, 0.9};
    private static final double MIDDLE = 7.5;
    private static final int PATCH = 2;
    /* The patch read is this many scaled pixels below the crosshair, about this many blocks at the rocket. */
    private static final int BESIDE = 8;
    private static final double RAISED = 0.4;
    private static final int DRAWN = 40;
    private static final int BLACK = 80;
    private static final int COPPER = 20;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;

    private RocketPartsClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void rocket_isDrawnPartByPartAsItsModelsShowThem(final ClientTestContext ctx) {
        final double[] heights = {BELL, TANK, SKIRT, CAPSULE};
        final int[][] drawn = new int[heights.length][];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(STAND), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(STAND), TestRockets.DISPLAY.get().defaultBlockState());
                    ((TestRockets.DisplayEntity) level.getBlockEntity(ctx.abs(STAND))).show(rocket(), false);
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestRockets.DisplayEntity stand && !stand.stack().sections().isEmpty(), ARRIVE,
                        "the stand's rocket to reach the player's game")
                .then(2, () -> look(ctx, MIDDLE, 0))
                .thenScreenshot(SETTLE, "rocket-parts-formed");
        for (int i = 0; i < heights.length; i++) {
            final int k = i;
            ctx.then(2, () -> look(ctx, heights[k], OUT[k])).then(4, () -> drawn[k] = mean(ctx));
        }
        ctx.then(0, () -> {
            ctx.assertTrue(drawn[2][0] < BLACK && drawn[2][1] < BLACK && drawn[2][2] < BLACK,
                    "the interstage skirt round the vacuum engine is black: " + str(drawn[2]));
            ctx.assertTrue(lighter(drawn[1], drawn[2]), "the tank is painted white, lighter than the skirt: "
                    + str(drawn[1]) + " against " + str(drawn[2]));
            ctx.assertTrue(lighter(drawn[3], drawn[2]), "the capsule is light, lighter than the skirt: "
                    + str(drawn[3]) + " against " + str(drawn[2]));
            ctx.assertTrue(drawn[0][0] - drawn[0][2] > COPPER && drawn[0][0] > drawn[0][1],
                    "the engine's bell is copper, redder than green and than blue: " + str(drawn[0]));
        }).thenServer(0, level -> ((TestRockets.DisplayEntity) level.getBlockEntity(ctx.abs(STAND)))
                .show(new RocketStack(List.of()), false))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(STAND))
                        instanceof TestRockets.DisplayEntity stand && stand.stack().sections().isEmpty(), ARRIVE,
                        "the bare stand to reach the player's game");
        for (int i = 0; i < heights.length; i++) {
            final int k = i;
            ctx.then(2, () -> look(ctx, heights[k], OUT[k])).then(4, () -> {
                final int[] bare = mean(ctx);
                ctx.assertTrue(distance(drawn[k], bare) > DRAWN, "the rocket is drawn at " + heights[k]
                        + " blocks up: " + str(drawn[k]) + " where the bare stand shows " + str(bare));
            });
        }
    }

    /*
     * A small rocket: a sea-level engine, three layers of tank, a separation ring, a vacuum engine in its skirt, two
     * layers of tank, the instrument ring and the capsule, fifteen blocks tall.
     */
    private static RocketStack rocket() {
        return RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 3), part("separation_ring_3", 1),
                part("vacuum_engine_3", 1), part("tank_3", 2), part("instrument_ring_3", 1), part("capsule_3", 1));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /*
     * Looks at the rocket a little above {@code height} up, {@code out} from its axis towards the player, to the
     * north, so the patch read below the crosshair falls at that height.
     */
    private static void look(final ClientTestContext ctx, final double height, final double out) {
        ctx.lookAt(Vec3.atBottomCenterOf(ctx.abs(STAND)).add(0, height + RAISED, -out));
    }

    /*
     * The mean colour, red, green and blue, of a patch just below the crosshair, which would whiten it; the look is
     * raised as far, so the patch falls where the rocket is looked at.
     */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + BESIDE;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH; dx <= PATCH; dx++) {
                for (int dy = -PATCH; dy <= PATCH; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                    count++;
                }
            }
        }
        return new int[] {(int) (sum[0] / count), (int) (sum[1] / count), (int) (sum[2] / count)};
    }

    /* Whether {@code a} is lighter than {@code b} in every channel, by {@link #DRAWN} at least. */
    private static boolean lighter(final int[] a, final int[] b) {
        return a[0] - b[0] > DRAWN && a[1] - b[1] > DRAWN && a[2] - b[2] > DRAWN;
    }

    private static int distance(final int[] a, final int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
