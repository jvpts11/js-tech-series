/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.space.crawler.CrawlerBlocks;
import dev.jstech.space.crawler.CrawlerLayout;
import dev.jstech.space.crawler.CrawlerPieceBlock;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A crawler-transporter on the player's game, as the approved crawler: built whole on its crawlerway, its pieces show
 * nothing of themselves and its engine draws it whole, its trucks of two tracks at the corners, the grey hull with its
 * hazard band, the deck with its jack pins and railing, the cabs at two corners; seen from the side and from above.
 */
public final class CrawlerClientTests {

    /* The engine, the crawler's length along x, and its lanes of gravel each side under its trucks. */
    private static final BlockPos ENGINE = new BlockPos(0, 4, 24);
    private static final int LANES = 30;
    /* Where the player looks from: off its left side, and high over its back end. */
    private static final Vec3 SIDE = new Vec3(-6.0, 3.5, -15.0);
    private static final Vec3 HIGH = new Vec3(-16.0, 9.0, -9.0);
    private static final int ARRIVE = 200;
    private static final int SETTLE = 20;
    private static final int CHANNEL = 0xFF;
    /* The hull's grey round the crosshair: its channels near one another, neither dark nor sky. */
    private static final int GREY_SPREAD = 40;
    private static final int GREY_DARKEST = 60;

    private CrawlerClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void crawler_isDrawnAsTheApprovedOne(final ClientTestContext ctx) {
        final Map<BlockPos, BlockState> was = new HashMap<>();
        final BlockPos[] engine = new BlockPos[1];
        ctx.afterTest(() -> ctx.server().execute(() -> was.forEach((at, state) -> ctx.serverLevel()
                .setBlockAndUpdate(at, state))));
        ctx.thenServer(0, level -> engine[0] = build(ctx, level, was))
                .thenWaitUntil(() -> engine[0] != null && ctx.mc().level.getBlockState(engine[0]).getBlock()
                        instanceof CrawlerPieceBlock && ctx.mc().level.getBlockState(engine[0])
                        .getValue(CrawlerPieceBlock.FORMED), ARRIVE, "the formed crawler to reach the player")
                .thenServer(0, level -> stand(level, engine[0], SIDE, was))
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(engine[0])))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "crawler-side")
                .then(0, () -> {
                    final int[] middle = middle(ctx);
                    final int spread = Math.max(middle[0], Math.max(middle[1], middle[2]))
                            - Math.min(middle[0], Math.min(middle[1], middle[2]));
                    ctx.assertTrue(spread < GREY_SPREAD && middle[0] > GREY_DARKEST, "the hull's grey is seen at the "
                            + "crosshair: " + middle[0] + ", " + middle[1] + ", " + middle[2]);
                })
                .thenServer(0, level -> stand(level, engine[0], HIGH, was))
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(engine[0]).add(2, 0, 0)))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "crawler-above");
    }

    /* Builds the crawler and its lanes of gravel, keeping what stood there to put back; gives the engine's place. */
    private static BlockPos build(final ClientTestContext ctx, final ServerLevel level,
                                  final Map<BlockPos, BlockState> was) {
        final BlockPos engine = ctx.abs(ENGINE);
        for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(engine, Direction.EAST)) {
            was.putIfAbsent(spot.at(), level.getBlockState(spot.at()));
            level.setBlockAndUpdate(spot.at(), spot.kind().block().defaultBlockState());
        }
        for (int x = -LANES; x <= LANES; x++) {
            for (final int z : new int[] {-5, -4, 4, 5}) {
                final BlockPos ground = engine.offset(x, -CrawlerLayout.TRACKS - 1, z);
                was.putIfAbsent(ground, level.getBlockState(ground));
                level.setBlockAndUpdate(ground, Blocks.GRAVEL.defaultBlockState());
            }
        }
        return CrawlerBlocks.CRAWLER_ENGINE.get() == level.getBlockState(engine).getBlock() ? engine : null;
    }

    /* The player at {@code from} the engine at {@code engine}, on a block no one sees, kept to put back. */
    private static void stand(final ServerLevel level, final BlockPos engine, final Vec3 from,
                              final Map<BlockPos, BlockState> was) {
        final Vec3 at = Vec3.atBottomCenterOf(engine).add(from);
        final BlockPos under = BlockPos.containing(at).below();
        was.putIfAbsent(under, level.getBlockState(under));
        level.setBlockAndUpdate(under, Blocks.BARRIER.defaultBlockState());
        level.players().getFirst().teleportTo(level, at.x, at.y, at.z, 0.0F, 0.0F);
    }

    /* The colour at the middle of the frame. */
    private static int[] middle(final ClientTestContext ctx) {
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            final int argb = frame.pixel(ctx.mc().getWindow().getGuiScaledWidth() / 2,
                    ctx.mc().getWindow().getGuiScaledHeight() / 2);
            return new int[] {argb >> 16 & CHANNEL, argb >> 8 & CHANNEL, argb & CHANNEL};
        }
    }
}
