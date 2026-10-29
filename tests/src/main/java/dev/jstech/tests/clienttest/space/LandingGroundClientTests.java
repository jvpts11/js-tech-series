/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.platform.LandingPlatform;
import dev.jstech.space.platform.LandingPlatformBlockEntity;
import dev.jstech.space.wreck.RocketWreckage;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Where a spent stage comes down, on a player's game: a landing platform's pads are bare concrete until it forms, and
 * then its target is painted across them, the ring and the cross a stage aims at; and the wreckage a stage leaves where
 * it strikes the ground is drawn as its torn plates, not as the ground round it.
 */
public final class LandingGroundClientTests {

    private static final BlockPos MIDDLE = new BlockPos(0, 2, 10);
    private static final BlockPos ABOVE = new BlockPos(0, 14, 4);
    private static final BlockPos WRECK = new BlockPos(3, 2, 3);
    private static final BlockPos BESIDE = new BlockPos(-3, 2, 3);
    private static final BlockPos BY_WRECK = new BlockPos(0, 2, 0);
    private static final int SIDE = 9;
    private static final int PATCH = 6;
    private static final int ARRIVE = 400;
    private static final int SETTLE = 20;
    private static final int PAINTED = 30;
    private static final int CHANNEL = 0xFF;

    private LandingGroundClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void platform_isPaintedWithItsTargetOnceItForms(final ClientTestContext ctx) {
        final int[][] seen = new int[2][];
        final BlockPos corner = MIDDLE.offset(-SIDE / 2, 0, -SIDE / 2);
        ctx.afterTest(() -> ctx.server().execute(() -> {
            for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(MIDDLE.offset(-SIDE / 2, 0, -SIDE / 2)),
                    ctx.abs(MIDDLE.offset(SIDE / 2, 0, SIDE / 2)))) {
                ctx.serverLevel().removeBlock(at, false);
            }
        }));
        ctx.thenTeleport(0, ABOVE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(MIDDLE), LandingPlatform.TERMINAL.get().defaultBlockState());
                    ((LandingPlatformBlockEntity) level.getBlockEntity(ctx.abs(MIDDLE))).own(Ownership.of(
                            UUID.randomUUID()).withAccess(Access.TEAM));
                    for (int dx = -SIDE / 2; dx <= SIDE / 2; dx++) {
                        for (int dz = -SIDE / 2; dz <= SIDE / 2; dz++) {
                            final BlockPos at = MIDDLE.offset(dx, 0, dz);
                            // One corner left out, so the platform stands bare until it is laid.
                            if ((dx != 0 || dz != 0) && !at.equals(corner)) {
                                level.setBlockAndUpdate(ctx.abs(at), LandingPlatform.PAD.get().defaultBlockState());
                            }
                        }
                    }
                })
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(MIDDLE)).add(1.5, 0.5, 1.5)))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenScreenshot(0, "platform-bare")
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(corner),
                        LandingPlatform.PAD.get().defaultBlockState()))
                .thenWaitUntil(() -> ctx.mc().level.getBlockEntity(ctx.abs(MIDDLE))
                        instanceof LandingPlatformBlockEntity platform && platform.shownSide() == SIDE, ARRIVE,
                        "the formed platform to reach the player")
                .then(SETTLE, () -> seen[1] = mean(ctx))
                .thenScreenshot(0, "platform-painted")
                .then(0, () -> ctx.assertTrue(distance(seen[0], seen[1]) > PAINTED,
                        "formed, its target is painted across its pads: " + str(seen[0]) + " bare, "
                                + str(seen[1]) + " painted"));
    }

    @ClientTest(timeoutTicks = 800)
    public static void wreckage_isDrawnAsTornPlates(final ClientTestContext ctx) {
        final int[][] seen = new int[2][];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(WRECK), false)));
        ctx.thenTeleport(0, BY_WRECK, Direction.SOUTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(WRECK),
                        RocketWreckage.BLOCK.get().defaultBlockState()))
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(WRECK))))
                .then(SETTLE, () -> seen[0] = mean(ctx))
                .thenScreenshot(0, "wreckage")
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(BESIDE)).add(0, -0.5, 0)))
                .then(SETTLE, () -> seen[1] = mean(ctx))
                .then(0, () -> ctx.assertTrue(distance(seen[0], seen[1]) > PAINTED,
                        "the wreckage is drawn as its plates, not as the ground: " + str(seen[0]) + " against "
                                + str(seen[1])));
    }

    /* The mean colour of a patch beside the crosshair, which whitens its own middle. */
    private static int[] mean(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 + PATCH;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 + PATCH;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -PATCH / 2; dx <= PATCH / 2; dx++) {
                for (int dy = -PATCH / 2; dy <= PATCH / 2; dy++) {
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

    private static int distance(final int[] a, final int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
